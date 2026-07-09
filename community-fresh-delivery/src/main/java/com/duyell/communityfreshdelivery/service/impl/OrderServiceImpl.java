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
import com.duyell.communityfreshdelivery.dto.SortItemDTO;
import com.duyell.communityfreshdelivery.entity.Address;
import com.duyell.communityfreshdelivery.entity.Coupon;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.entity.Product;

import com.duyell.communityfreshdelivery.entity.UserCoupon;
import com.duyell.communityfreshdelivery.enums.DeliveryType;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.mapper.CouponMapper;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductBatchMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.UserCouponMapper;
import com.duyell.communityfreshdelivery.common.utils.OrderNoUtil;
import com.duyell.communityfreshdelivery.service.CartService;
import com.duyell.communityfreshdelivery.service.OperationLogService;
import com.duyell.communityfreshdelivery.service.OrderService;
import com.duyell.communityfreshdelivery.service.ProductBatchService;
import com.duyell.communityfreshdelivery.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ProductBatchMapper productBatchMapper;

    private final CartService cartService;
    private final ProductBatchService productBatchService;
    private final OrderNoUtil orderNoUtil;
    private final RabbitTemplate rabbitTemplate;
    private final OperationLogService operationLogService;
    private final SysConfigService sysConfigService;

    /** 死信队列：发到此处，TTL 到期后自动投递到取消队列 */
    private static final String DELAY_QUEUE = "order.delay.queue";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 批次分配明细条目，用于 JSON 序列化/反序列化 */
    private record BatchAlloc(long bid, int qty) {}

    // ==================== 下单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO place(OrderCreateDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        // 1. 校验配送参数
        if (dto.getDeliveryType().equals(DeliveryType.HOME_DELIVERY.getCode())) {
            if (dto.getAddressId() == null) {
                throw new BusinessException(20001, "配送到家请选择收货地址");
            }
            Address address = addressMapper.selectById(dto.getAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException(20001, "收货地址不存在");
            }
        } else if (dto.getDeliveryType().equals(DeliveryType.PICKUP.getCode())) {
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

        // 4. 构建 productMap（优惠券范围校验 + 批次预扣用）
        Set<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
        final Map<Long, Product> productMap;
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectList(
                            new LambdaQueryWrapper<Product>().in(Product::getId, productIds))
                    .stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
        } else {
            productMap = Collections.emptyMap();
        }

        // 5. 处理优惠券
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Long usedCouponId = null;
        if (dto.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(dto.getUserCouponId());
            if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
                throw new BusinessException(70004, "优惠券不存在");
            }
            if (userCoupon.getStatus() != 0) {
                throw new BusinessException(70005, "优惠券已使用或已过期");
            }
            if (userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
                userCoupon.setStatus(2);
                userCouponMapper.updateById(userCoupon);
                throw new BusinessException(70005, "优惠券已过期");
            }

            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon == null || coupon.getStatus() != 1) {
                throw new BusinessException(70004, "优惠券已失效");
            }

            // 计算优惠券适用金额（品类券/范围券仅计算匹配分类的商品小计）
            BigDecimal applicableAmount = totalAmount;
            if (coupon.getScopeType() == 1 && coupon.getScopeId() != null) {
                applicableAmount = orderItems.stream()
                        .filter(i -> {
                            Product p = productMap.get(i.getProductId());
                            return p != null && coupon.getScopeId().equals(p.getCategoryId());
                        })
                        .map(OrderItem::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (applicableAmount.compareTo(BigDecimal.ZERO) == 0) {
                    throw new BusinessException(70007, "该优惠券不适用于当前订单中的商品");
                }
            }

            // 门槛校验（基于适用金额）
            if (applicableAmount.compareTo(coupon.getThreshold()) < 0) {
                throw new BusinessException(70006,
                        "未满" + coupon.getThreshold() + "元，无法使用该优惠券");
            }

            // 计算折扣（基于适用金额）
            couponDiscount = CouponServiceImpl.calculateDiscount(applicableAmount, coupon);

            // 标记已使用
            userCoupon.setStatus(1);
            userCouponMapper.updateById(userCoupon);
            usedCouponId = userCoupon.getId();

            log.info("优惠券已使用: userCouponId={} couponId={} discount={}",
                    usedCouponId, coupon.getId(), couponDiscount);
        }

        // 6. 计算配送费 + 包装费 + 实付（从 sys_config 读取）
        BigDecimal freeThreshold = sysConfigService.getDecimal("free_delivery_threshold", "30.00");
        BigDecimal deliveryFee = sysConfigService.getDecimal("delivery_fee", "5.00");
        BigDecimal packageFee = sysConfigService.getDecimal("package_fee", "1.00");

        BigDecimal delivery = totalAmount.compareTo(freeThreshold) >= 0
                ? BigDecimal.ZERO
                : deliveryFee;
        BigDecimal actualAmount = totalAmount.add(delivery).add(packageFee).subtract(couponDiscount);
        // 实付不能为负（优惠券额度可能超过商品金额）
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualAmount = BigDecimal.ZERO;
        }

        // 7. 起送价校验
        BigDecimal minOrderAmount = sysConfigService.getDecimal("min_order_amount", "15.00");
        if (totalAmount.compareTo(minOrderAmount) < 0) {
            BigDecimal diff = minOrderAmount.subtract(totalAmount);
            throw new BusinessException(20005, "还差 " + diff + " 元起送");
        }

        // 8. 按 product 聚合数量 → FIFO 扣批次库存，拆分到各 OrderItem（称重商品跳过）
        Map<Long, Integer> productQtyMap = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new BusinessException(10001, "商品" + item.getProductName() + "不存在");
            }
            // 校验商品是否仍在售（防止已下架商品被下单）
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(10004, "商品" + item.getProductName() + "已下架");
            }
            // 称重商品不参与下单时的批次预扣（分拣时再处理）
            if (product.getIsWeighted() != null && product.getIsWeighted() == 1) {
                continue;
            }
            productQtyMap.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        // productId → FIFO 分配结果（可变副本，用于分发到各 OrderItem）
        Map<Long, List<Map<String, Object>>> productAllocsMap = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : productQtyMap.entrySet()) {
            List<Map<String, Object>> allocs = productBatchService.allocateFIFO(
                    entry.getKey(), entry.getValue());
            if (allocs.isEmpty() || allocs.stream().noneMatch(
                    a -> ((Number) a.get("allocatedQuantity")).intValue() > 0)) {
                throw new BusinessException(20004, "商品库存不足");
            }
            productAllocsMap.put(entry.getKey(), new ArrayList<>(allocs));
        }

        // 将 FIFO 结果按各 OrderItem 数量拆分，写入 batchAllocations
        for (OrderItem item : orderItems) {
            Product product = productMap.get(item.getProductId());
            if (product == null || (product.getIsWeighted() != null && product.getIsWeighted() == 1)) {
                continue;
            }

            List<Map<String, Object>> allocs = productAllocsMap.get(item.getProductId());
            if (allocs == null || allocs.isEmpty()) {
                continue;
            }

            int need = item.getQuantity();
            List<BatchAlloc> itemAllocs = new ArrayList<>();

            while (need > 0 && !allocs.isEmpty()) {
                Map<String, Object> first = allocs.getFirst();
                int remaining = ((Number) first.get("allocatedQuantity")).intValue();
                int take = Math.min(remaining, need);
                itemAllocs.add(new BatchAlloc(
                        ((Number) first.get("batchId")).longValue(), take));
                need -= take;
                if (take < remaining) {
                    first.put("allocatedQuantity", remaining - take);
                } else {
                    allocs.removeFirst();
                }
            }

            if (!itemAllocs.isEmpty()) {
                item.setBatchId(itemAllocs.getFirst().bid());
                try {
                    item.setBatchAllocations(OBJECT_MAPPER.writeValueAsString(itemAllocs));
                } catch (Exception e) {
                    throw new BusinessException(20000, "批次分配序列化失败");
                }
            }
        }

        // 9. 生成订单号 + 写订单
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
        order.setCouponDiscount(couponDiscount);
        order.setActualAmount(actualAmount);
        order.setCouponId(usedCouponId);
        order.setRemark(dto.getRemark());
        order.setPickupCode(generatePickupCode(dto.getDeliveryType()));
        orderMapper.insert(order);

        // 10. 写订单明细（关联 orderId）
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 11. 清空购物车（复用 CartService）
        cartService.clear();

        // 12. 发送延迟消息（发到死信队列，TTL 到期后自动投递到取消队列）
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

        // 原子更新订单状态（防并发重复取消）
        int updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .in(Order::getStatus, List.of(
                                OrderStatus.PENDING_PAYMENT.getCode(),
                                OrderStatus.PENDING_ACCEPT.getCode(),
                                OrderStatus.PENDING_SORTING.getCode()))
                        .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                        .set(Order::getCancelReason, reason != null ? reason : "用户主动取消")
        );
        if (updated == 0) {
            throw new BusinessException(20007, "当前订单状态不允许取消");
        }

        // 回补批次库存（优先用 batchAllocations 精确回补，兼容旧 batchId）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            String allocJson = item.getBatchAllocations();
            if (allocJson != null && !allocJson.isBlank()) {
                // 新逻辑：按 batchAllocations 逐批精确回补
                try {
                    List<BatchAlloc> allocs = OBJECT_MAPPER.readValue(
                            allocJson, new TypeReference<List<BatchAlloc>>() {});
                    for (BatchAlloc a : allocs) {
                        productBatchMapper.addRemaining(a.bid(), a.qty());
                    }
                } catch (Exception e) {
                    log.error("解析批次分配明细失败: orderItemId={} json={}", item.getId(), allocJson, e);
                    throw new BusinessException(20000, "库存回补失败");
                }
            } else if (item.getBatchId() != null) {
                // 兼容旧订单：仅记录了第一个 batchId，全量回补到该批次
                productBatchMapper.addRemaining(item.getBatchId(), item.getQuantity());
            }
        }

        log.info("订单已取消: userId={} orderNo={} reason={}", SecurityUtil.currentUserId(), order.getOrderNo(), reason);

        operationLogService.record(SecurityUtil.currentUserId(),
                OperationLogService.ORDER_CANCEL,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.textOf(status),
                OrderStatus.CANCELLED.getText(),
                reason != null ? reason : "用户主动取消");
    }

    // ==================== 商家操作 ====================

    @Override
    public void accept(Long orderId) {
        // 原子更新（防并发重复接单）
        int updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, OrderStatus.PENDING_ACCEPT.getCode())
                        .set(Order::getStatus, OrderStatus.PENDING_SORTING.getCode())
        );
        if (updated == 0) {
            throw new BusinessException(20007, "订单不存在或当前状态不可接单");
        }

        Order order = orderMapper.selectById(orderId);
        log.info("商家接单: orderNo={}", order.getOrderNo());

        operationLogService.record(SecurityUtil.currentUserId(),
                OperationLogService.ORDER_ACCEPT,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.PENDING_ACCEPT.getText(),
                OrderStatus.PENDING_SORTING.getText(),
                "商家接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortComplete(Long orderId, List<SortItemDTO> items) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(20006, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_SORTING.getCode()) {
            throw new BusinessException(20007, "当前订单状态不可分拣");
        }

        // 加载订单明细
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        if (orderItems.isEmpty()) {
            throw new BusinessException(20006, "订单明细异常");
        }

        // 构建 orderItemId → OrderItem 映射
        Map<Long, OrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));

        // 构建 orderItemId → SortItemDTO 映射
        Map<Long, SortItemDTO> sortMap = Collections.emptyMap();
        if (items != null && !items.isEmpty()) {
            sortMap = items.stream()
                    .filter(s -> s.getOrderItemId() != null)
                    .collect(Collectors.toMap(SortItemDTO::getOrderItemId, s -> s, (a, b) -> a));
        }

        // 加载商品信息（判断是否称重）
        Set<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal totalAdjustment = BigDecimal.ZERO; // 差额合计（正=补款，负=退款）
        boolean hasRefund = false;

        for (OrderItem item : orderItems) {
            SortItemDTO sort = sortMap.get(item.getId());

            // 处理缺货
            if (sort != null && Boolean.TRUE.equals(sort.getShortage())) {
                item.setShortage(1);
                // 缺货退款 = 该商品预估金额
                totalAdjustment = totalAdjustment.subtract(item.getAmount());
                item.setActualAmount(BigDecimal.ZERO);
                hasRefund = true;

                // 回补预扣的批次库存（与 cancel() 逻辑一致）
                rollbackItemStock(item);

                // 清空批次分配信息（已回补，避免后续重复操作）
                item.setBatchAllocations(null);
                item.setBatchId(null);
                orderItemMapper.updateById(item);
                continue;
            }

            // 处理称重商品
            Product product = productMap.get(item.getProductId());
            if (product != null && product.getIsWeighted() != null && product.getIsWeighted() == 1
                    && sort != null && sort.getActualWeight() != null) {
                BigDecimal estimatedWeight = item.getEstimatedWeight() != null
                        ? item.getEstimatedWeight() : BigDecimal.ZERO;
                BigDecimal actualWeight = sort.getActualWeight();
                BigDecimal weightDiff = actualWeight.subtract(estimatedWeight);
                BigDecimal priceDiff = weightDiff.multiply(item.getPrice()).setScale(2, java.math.RoundingMode.HALF_UP);

                item.setActualWeight(actualWeight);
                item.setActualAmount(item.getAmount().add(priceDiff));
                totalAdjustment = totalAdjustment.add(priceDiff);

                if (priceDiff.compareTo(BigDecimal.ZERO) < 0) {
                    hasRefund = true;
                }

                orderItemMapper.updateById(item);
                log.info("称重调整: orderItemId={} estimatedWeight={} actualWeight={} diff={}",
                        item.getId(), estimatedWeight, actualWeight, priceDiff);
            } else {
                // 固定规格：实付 = 预估
                item.setActualAmount(item.getAmount());
                orderItemMapper.updateById(item);
            }
        }

        // 更新订单金额
        if (totalAdjustment.compareTo(BigDecimal.ZERO) != 0) {
            order.setTotalAmount(order.getTotalAmount().add(totalAdjustment));
            order.setActualAmount(order.getActualAmount().add(totalAdjustment));
            // 实付金额不能为负
            if (order.getActualAmount().compareTo(BigDecimal.ZERO) < 0) {
                order.setActualAmount(BigDecimal.ZERO);
            }
        }

        // 更新订单状态
        order.setStatus(OrderStatus.PENDING_DELIVERY.getCode());
        orderMapper.updateById(order);

        log.info("商家分拣完成: orderNo={} totalAdjustment={} hasRefund={}",
                order.getOrderNo(), totalAdjustment, hasRefund);

        operationLogService.record(SecurityUtil.currentUserId(),
                OperationLogService.ORDER_SORT_COMPLETE,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.PENDING_SORTING.getText(),
                OrderStatus.PENDING_DELIVERY.getText(),
                "分拣完成，差额调整:" + totalAdjustment);
    }

    // ==================== 内部方法 ====================

    /**
     * 回补订单项的批次库存.

     * <p>优先按 batchAllocations JSON 逐批精确回补，兼容旧 batchId 逻辑.
     * 使用原子 UPDATE（addRemaining），避免并发问题.</p>
     */
    private void rollbackItemStock(OrderItem item) {
        String allocJson = item.getBatchAllocations();
        if (allocJson != null && !allocJson.isBlank()) {
            try {
                List<BatchAlloc> allocs = OBJECT_MAPPER.readValue(
                        allocJson, new TypeReference<List<BatchAlloc>>() {});
                for (BatchAlloc a : allocs) {
                    productBatchMapper.addRemaining(a.bid(), a.qty());
                }
                log.info("库存回补完成(按batchAllocations): orderItemId={}", item.getId());
            } catch (Exception e) {
                log.error("解析批次分配明细失败: orderItemId={} json={}", item.getId(), allocJson, e);
            }
        } else if (item.getBatchId() != null) {
            productBatchMapper.addRemaining(item.getBatchId(), item.getQuantity());
            log.info("库存回补完成(旧batchId): orderItemId={} batchId={} qty={}",
                    item.getId(), item.getBatchId(), item.getQuantity());
        }
    }

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
