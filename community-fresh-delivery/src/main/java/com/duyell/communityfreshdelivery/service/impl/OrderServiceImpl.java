package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.CartItemVO;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderItemVO;
import com.duyell.communityfreshdelivery.dto.OrderTimeoutMessage;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.entity.Address;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.duyell.communityfreshdelivery.common.utils.OrderNoUtil;
import com.duyell.communityfreshdelivery.service.CartService;
import com.duyell.communityfreshdelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>订单服务实现</h2>
 *
 * <h3>下单事务（place）</h3>
 * <ol>
 *   <li>校验配送参数（地址/自提点）</li>
 *   <li>调用 {@link CartService#list()} 获取购物车内容</li>
 *   <li>库存预检 + 计算金额 + 构建 OrderItem 列表</li>
 *   <li>起送价校验 + 配送费计算</li>
 *   <li>扣减 MySQL 库存（{@code UPDATE ... WHERE stock >= quantity}防超卖）</li>
 *   <li>生成订单号 + 写入 order + order_item</li>
 *   <li>调用 {@link CartService#clear()} 清空购物车</li>
 *   <li>发送 RabbitMQ 延迟消息（15 分钟后自动取消）</li>
 * </ol>
 *
 * <h3>取消订单（cancel）</h3>
 * <p>仅待付款/待接单/待分拣状态可取消，取消后回补库存.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;

    private final CartService cartService;
    private final OrderNoUtil orderNoUtil;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.biz.min-order-amount}")
    private BigDecimal minOrderAmount;

    @Value("${app.biz.delivery-fee}")
    private BigDecimal deliveryFee;

    @Value("${app.biz.package-fee}")
    private BigDecimal packageFee;

    @Value("${app.biz.free-delivery-threshold}")
    private BigDecimal freeDeliveryThreshold;

    /** 死信队列：发到此处，TTL 到期后自动投递到取消队列 */
    private static final String DELAY_QUEUE = "order.delay.queue";

    // ==================== 下单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO place(OrderCreateDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        // 1. 校验配送参数
        if (dto.getDeliveryType() == 1) {
            if (dto.getAddressId() == null) {
                throw new BusinessException(20001, "配送到家请选择收货地址");
            }
            Address address = addressMapper.selectById(dto.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException(20001, "收货地址不存在");
            }
        } else if (dto.getDeliveryType() == 2) {
            if (dto.getPickupPointId() == null) {
                throw new BusinessException(20001, "自提请选择自提点");
            }
        } else {
            throw new BusinessException(20001, "配送方式无效");
        }

        // 2. 获取购物车内容（复用 CartService，不重复读 Redis/查 SKU/查 Product）
        List<CartItemVO> cartItems = cartService.list();
        if (cartItems.isEmpty()) {
            throw new BusinessException(20002, "购物车为空，请先添加商品");
        }

        // 3. 库存预检 + 计算金额 + 构建 OrderItem
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemVO ci : cartItems) {
            if (!ci.getStockSufficient()) {
                throw new BusinessException(20004,
                        ci.getProductName() + "（" + ci.getSpecName() + "）库存不足");
            }

            BigDecimal itemAmount = ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);

            OrderItem item = new OrderItem();
            item.setProductId(ci.getProductId());
            item.setSkuId(ci.getSkuId());
            item.setProductName(ci.getProductName());
            item.setSpecName(ci.getSpecName());
            item.setPrice(ci.getPrice());
            item.setQuantity(ci.getQuantity());
            item.setAmount(itemAmount);
            item.setShortage(0);
            orderItems.add(item);
        }

        // 4. 计算配送费 + 包装费 + 实付
        BigDecimal delivery = totalAmount.compareTo(freeDeliveryThreshold) >= 0
                ? BigDecimal.ZERO
                : deliveryFee;
        BigDecimal actualAmount = totalAmount.add(delivery).add(packageFee);

        // 5. 起送价校验
        if (totalAmount.compareTo(minOrderAmount) < 0) {
            BigDecimal diff = minOrderAmount.subtract(totalAmount);
            throw new BusinessException(20005, "还差 " + diff + " 元起送");
        }

        // 6. 扣减库存（MySQL 行级锁，利用 WHERE stock >= quantity 防超卖）
        for (OrderItem item : orderItems) {
            int rows = productSkuMapper.deductStock(item.getSkuId(), item.getQuantity());
            if (rows == 0) {
                throw new BusinessException(20004, "商品库存不足，可能已被抢光");
            }
        }

        // 7. 生成订单号 + 写订单
        String orderNo = orderNoUtil.generate();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setAddressId(dto.getAddressId());
        order.setPickupPointId(dto.getPickupPointId());
        order.setDeliveryType(dto.getDeliveryType());
        order.setDeliveryTimeSlot(dto.getDeliveryTimeSlot());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setTotalAmount(totalAmount);
        order.setDeliveryFee(delivery);
        order.setPackageFee(packageFee);
        order.setCouponDiscount(BigDecimal.ZERO);
        order.setActualAmount(actualAmount);
        order.setRemark(dto.getRemark());
        order.setPickupCode(generatePickupCode(dto.getDeliveryType()));
        orderMapper.insert(order);

        // 8. 写订单明细（关联 orderId）
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 9. 清空购物车（复用 CartService）
        cartService.clear();

        // 10. 发送延迟消息（发到死信队列，TTL 到期后自动投递到取消队列）
        OrderTimeoutMessage msg = new OrderTimeoutMessage(order.getId());
        rabbitTemplate.convertAndSend(DELAY_QUEUE, msg);

        log.info("下单成功: userId={} orderNo={} amount={} items={}",
                userId, orderNo, actualAmount, orderItems.size());
        return buildOrderVO(order, orderItems);
    }

    // ==================== 订单详情 ====================

    @Override
    public OrderVO getById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(20006, "订单不存在");
        }
        if (!order.getUserId().equals(SecurityUtil.currentUserId())) {
            throw new BusinessException(20006, "订单不存在");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        return buildOrderVO(order, items);
    }

    // ==================== 我的订单分页 ====================

    @Override
    public Page<OrderVO> page(int page, int size, Integer status) {
        Long userId = SecurityUtil.currentUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<Order> orders = orderPage.getRecords();

        // 批量加载明细
        Map<Long, List<OrderItem>> itemMap = Collections.emptyMap();
        if (!orders.isEmpty()) {
            Set<Long> orderIds = orders.stream()
                    .map(Order::getId)
                    .collect(Collectors.toSet());
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
            );
            itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(OrderItem::getOrderId));
        }

        Map<Long, List<OrderItem>> finalItemMap = itemMap;
        List<OrderVO> voList = orders.stream()
                .map(o -> buildOrderVO(o, finalItemMap.getOrDefault(o.getId(), List.of())))
                .toList();

        Page<OrderVO> voPage = new Page<>(page, size, orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 取消订单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(20006, "订单不存在");
        }
        if (!order.getUserId().equals(SecurityUtil.currentUserId())) {
            throw new BusinessException(20006, "订单不存在");
        }

        int status = order.getStatus();
        if (status != OrderStatus.PENDING_PAYMENT.getCode()
                && status != OrderStatus.PENDING_ACCEPT.getCode()
                && status != OrderStatus.PENDING_SORTING.getCode()) {
            throw new BusinessException(20007, "当前订单状态不允许取消");
        }

        // 更新订单状态
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason(reason != null ? reason : "用户主动取消");
        orderMapper.updateById(order);

        // 回补库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            productSkuMapper.addStock(item.getSkuId(), item.getQuantity());
        }

        log.info("订单已取消: userId={} orderNo={} reason={}", SecurityUtil.currentUserId(), order.getOrderNo(), reason);
    }

    // ==================== 内部方法 ====================

    /** 组装 OrderVO */
    private OrderVO buildOrderVO(Order order, List<OrderItem> items) {
        // 批量加载 Product（获取商品图片）
        Set<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = Collections.emptyMap();
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectList(
                            new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                    ).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
        }

        Map<Long, Product> finalProductMap = productMap;
        List<OrderItemVO> itemVOs = items.stream()
                .map(i -> {
                    Product p = finalProductMap.get(i.getProductId());
                    String firstImage = null;
                    if (p != null && p.getImages() != null && !p.getImages().isBlank()) {
                        firstImage = p.getImages();
                    }
                    return OrderItemVO.builder()
                            .id(i.getId())
                            .productId(i.getProductId())
                            .skuId(i.getSkuId())
                            .productName(i.getProductName())
                            .specName(i.getSpecName())
                            .productImage(firstImage)
                            .price(i.getPrice())
                            .quantity(i.getQuantity())
                            .amount(i.getAmount())
                            .shortage(i.getShortage() != null && i.getShortage() == 1)
                            .build();
                })
                .toList();

        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .pickupPointId(order.getPickupPointId())
                .deliveryType(order.getDeliveryType())
                .deliveryTimeSlot(order.getDeliveryTimeSlot())
                .status(order.getStatus())
                .statusText(OrderStatus.textOf(order.getStatus()))
                .totalAmount(order.getTotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .packageFee(order.getPackageFee())
                .couponDiscount(order.getCouponDiscount())
                .actualAmount(order.getActualAmount())
                .pickupCode(order.getPickupCode())
                .remark(order.getRemark())
                .cancelReason(order.getCancelReason())
                .paidTime(order.getPaidTime())
                .deliveredTime(order.getDeliveredTime())
                .createTime(order.getCreateTime())
                .items(itemVOs)
                .build();
    }

    /** 生成自提码（6位数字，仅自提订单） */
    private String generatePickupCode(Integer deliveryType) {
        if (deliveryType == null || deliveryType != 2) {
            return "";
        }
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
