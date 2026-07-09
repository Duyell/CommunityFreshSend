package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.DeliveryVO;
import com.duyell.communityfreshdelivery.entity.Address;
import com.duyell.communityfreshdelivery.entity.Delivery;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.PickupPoint;
import com.duyell.communityfreshdelivery.enums.DeliveryStatus;
import com.duyell.communityfreshdelivery.enums.DeliveryType;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.AddressMapper;
import com.duyell.communityfreshdelivery.mapper.DeliveryMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import com.duyell.communityfreshdelivery.service.DeliveryService;
import com.duyell.communityfreshdelivery.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>配送服务实现</h2>
 *
 * <h3>配送状态流转</h3>
 * <pre>{@code
 * 接单大厅（order.status = PENDING_DELIVERY）
 *   │
 *   └── 抢单 grab()
 *         │
 *         ├── 创建 delivery（status = WAITING_PICKUP）
 *         ├── 更新 order.status = IN_DELIVERY
 *         │
 *         └── 取货 confirmPickup()
 *               │
 *               ├── 更新 delivery.status = IN_DELIVERY
 *               │
 *               └── 送达 confirmDelivery()
 *                     │
 *                     ├── 更新 delivery.status = DELIVERED
 *                     └── 更新 order.status = RECEIVED
 * }</pre>
 *
 * <h3>防重复抢单</h3>
 * <p>grab() 使用 {@code UPDATE ... WHERE status = PENDING_DELIVERY} 原子操作，
 * 只有第一个抢单请求能成功.</p>
 *
 * <h3>配送员隔离</h3>
 * <p>取货/送达操作校验 deliveryUserId == 当前用户.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryMapper deliveryMapper;
    private final OrderMapper orderMapper;
    private final AddressMapper addressMapper;
    private final PickupPointMapper pickupPointMapper;
    private final OperationLogService operationLogService;

    // ==================== 接单大厅 ====================

    @Override
    public Page<DeliveryVO> hall(int page, int size) {
        // 查询所有待配送订单，按创建时间升序
        Page<Order> orderPage = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_DELIVERY.getCode())
                        .orderByAsc(Order::getCreateTime)
        );
        List<Order> orders = orderPage.getRecords();

        // 批量加载地址和自提点（避免 N+1）
        Map<Long, Address> addressMap = loadAddressMap(orders);
        Map<Long, PickupPoint> pickupMap = loadPickupPointMap(orders);

        List<DeliveryVO> voList = orders.stream()
                .map(o -> toDeliveryVO(o, addressMap, pickupMap))
                .toList();

        Page<DeliveryVO> voPage = new Page<>(page, size, orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 抢单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryVO grab(Long orderId) {
        Long userId = SecurityUtil.currentUserId();

        // 1. 载入订单信息
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(20006, "订单不存在");
        }

        // 抢单仅限配送到家订单（自提订单不需要配送员）
        if (!DeliveryType.HOME_DELIVERY.getCode().equals(order.getDeliveryType())) {
            throw new BusinessException(40001, "该订单为自提订单，不可抢单");
        }

        // 2. 原子抢单：WHERE status = PENDING_DELIVERY AND delivery_type = 1 确保只有第一个抢的人成功
        int updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, OrderStatus.PENDING_DELIVERY.getCode())
                        .eq(Order::getDeliveryType, DeliveryType.HOME_DELIVERY.getCode())
                        .set(Order::getStatus, OrderStatus.IN_DELIVERY.getCode())
        );
        if (updated == 0) {
            throw new BusinessException(40001, "该订单已被抢或状态不正确");
        }

        // 3. 创建配送记录
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setDeliveryUserId(userId);
        delivery.setStatus(DeliveryStatus.WAITING_PICKUP.getCode());
        deliveryMapper.insert(delivery);

        // 4. 组装返回（带 delivery ID）
        Map<Long, Address> addressMap = loadAddressMap(List.of(order));
        Map<Long, PickupPoint> pickupMap = loadPickupPointMap(List.of(order));

        log.info("配送员抢单: userId={} orderNo={}", userId, order.getOrderNo());

        operationLogService.record(userId,
                OperationLogService.DELIVERY_GRAB,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.PENDING_DELIVERY.getText(),
                OrderStatus.IN_DELIVERY.getText(),
                "配送员抢单");

        return buildVO(delivery, order, addressMap, pickupMap);
    }

    // ==================== 取货确认 ====================

    @Override
    public void confirmPickup(Long orderId) {
        // 校验配送记录归属 + 状态
        Delivery delivery = getOwnDelivery(orderId);
        if (delivery.getStatus() != DeliveryStatus.WAITING_PICKUP.getCode()) {
            throw new BusinessException(40002, "当前配送状态不可取货");
        }

        // 更新配送状态 + 取货时间
        delivery.setStatus(DeliveryStatus.IN_DELIVERY.getCode());
        delivery.setPickupTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        log.info("配送员取货确认: userId={} orderId={}", SecurityUtil.currentUserId(), orderId);
    }

    // ==================== 送达确认 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(Long orderId) {
        // 校验配送记录归属 + 状态
        Delivery delivery = getOwnDelivery(orderId);
        if (delivery.getStatus() != DeliveryStatus.IN_DELIVERY.getCode()) {
            throw new BusinessException(40002, "当前配送状态不可送达");
        }

        // 更新配送记录
        delivery.setStatus(DeliveryStatus.DELIVERED.getCode());
        delivery.setDeliverTime(LocalDateTime.now());
        deliveryMapper.updateById(delivery);

        // 同步更新订单状态
        Order order = orderMapper.selectById(orderId);
        order.setStatus(OrderStatus.RECEIVED.getCode());
        order.setDeliveredTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("配送员送达确认: userId={} orderNo={}", SecurityUtil.currentUserId(), order.getOrderNo());

        operationLogService.record(SecurityUtil.currentUserId(),
                OperationLogService.DELIVERY_COMPLETE,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.IN_DELIVERY.getText(),
                OrderStatus.RECEIVED.getText(),
                "配送员确认送达");
    }

    // ==================== 我的配送 ====================

    @Override
    public Page<DeliveryVO> myDeliveries(int page, int size, Integer status) {
        Long userId = SecurityUtil.currentUserId();

        // 查当前配送员的所有配送记录
        LambdaQueryWrapper<Delivery> wrapper = new LambdaQueryWrapper<Delivery>()
                .eq(Delivery::getDeliveryUserId, userId)
                .orderByDesc(Delivery::getCreateTime);
        if (status != null) {
            wrapper.eq(Delivery::getStatus, status);
        }
        Page<Delivery> deliveryPage = deliveryMapper.selectPage(new Page<>(page, size), wrapper);
        List<Delivery> deliveries = deliveryPage.getRecords();

        // 批量加载关联订单（一次 IN 查询，避免 N+1）
        Map<Long, Order> orderMap = Collections.emptyMap();
        if (!deliveries.isEmpty()) {
            Set<Long> orderIds = deliveries.stream()
                    .map(Delivery::getOrderId)
                    .collect(Collectors.toSet());
            orderMap = orderMapper.selectList(
                            new LambdaQueryWrapper<Order>().in(Order::getId, orderIds)
                    ).stream()
                    .collect(Collectors.toMap(Order::getId, o -> o));
        }

        // 批量加载地址和自提点（避免 N+1）
        List<Order> relatedOrders = orderMap.values().stream().toList();
        Map<Long, Address> addressMap = loadAddressMap(relatedOrders);
        Map<Long, PickupPoint> pickupMap = loadPickupPointMap(relatedOrders);

        // 拼装 VO
        Map<Long, Order> finalOrderMap = orderMap;
        List<DeliveryVO> voList = deliveries.stream()
                .map(d -> {
                    Order order = finalOrderMap.get(d.getOrderId());
                    if (order == null) {
                        return null;
                    }
                    return buildVO(d, order, addressMap, pickupMap);
                })
                .filter(Objects::nonNull)
                .toList();

        Page<DeliveryVO> voPage = new Page<>(page, size, deliveryPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 内部方法 ====================

    /**
     * 查配送记录并校验归属.
     *
     * <p>通过 orderId + deliveryUserId 双重匹配，确保只有该订单的配送员本人能操作.</p>
     *
     * @param orderId 订单ID
     * @return 配送记录（保证存在且归属当前用户）
     */
    private Delivery getOwnDelivery(Long orderId) {
        Delivery delivery = deliveryMapper.selectOne(
                new LambdaQueryWrapper<Delivery>()
                        .eq(Delivery::getOrderId, orderId)
                        .eq(Delivery::getDeliveryUserId, SecurityUtil.currentUserId())
        );
        if (delivery == null) {
            throw new BusinessException(40003, "配送记录不存在");
        }
        return delivery;
    }

    /**
     * 批量加载订单关联的收货地址.
     *
     * @param orders 订单列表
     * @return addressId → Address
     */
    private Map<Long, Address> loadAddressMap(List<Order> orders) {
        Set<Long> addressIds = orders.stream()
                .map(Order::getAddressId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (addressIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return addressMapper.selectList(
                        new LambdaQueryWrapper<Address>().in(Address::getId, addressIds)
                ).stream()
                .collect(Collectors.toMap(Address::getId, a -> a));
    }

    /**
     * 批量加载订单关联的自提点.
     *
     * @param orders 订单列表
     * @return pickupPointId → PickupPoint
     */
    private Map<Long, PickupPoint> loadPickupPointMap(List<Order> orders) {
        Set<Long> pointIds = orders.stream()
                .map(Order::getPickupPointId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (pointIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pickupPointMapper.selectList(
                        new LambdaQueryWrapper<PickupPoint>().in(PickupPoint::getId, pointIds)
                ).stream()
                .collect(Collectors.toMap(PickupPoint::getId, p -> p));
    }

    /**
     * 解析订单的送达地址文本.
     *
     * <p>配送到家：拼接省市区+详细地址；自提：取自提点名称+地址.</p>
     *
     * @param order      订单
     * @param addressMap 预加载的地址 Map
     * @param pickupMap  预加载的自提点 Map
     * @return 可读地址字符串
     */
    private String resolveAddress(Order order, Map<Long, Address> addressMap,
                                   Map<Long, PickupPoint> pickupMap) {
        if (order.getDeliveryType().equals(DeliveryType.HOME_DELIVERY.getCode())
                && order.getAddressId() != null) {
            Address address = addressMap.get(order.getAddressId());
            if (address != null) {
                return address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetail();
            }
        }
        if (order.getDeliveryType().equals(DeliveryType.PICKUP.getCode())
                && order.getPickupPointId() != null) {
            PickupPoint point = pickupMap.get(order.getPickupPointId());
            if (point != null) {
                return point.getName() + "（" + point.getAddress() + "）";
            }
        }
        return "未知地址";
    }

    /** 订单 → VO（接单大厅 / 抢单返回用） */
    private DeliveryVO toDeliveryVO(Order order, Map<Long, Address> addressMap,
                                     Map<Long, PickupPoint> pickupMap) {
        String addressText = resolveAddress(order, addressMap, pickupMap);
        String deliveryTypeText = DeliveryType.textOf(order.getDeliveryType());

        return DeliveryVO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .deliveryTypeText(deliveryTypeText)
                .address(addressText)
                .deliveryTimeSlot(order.getDeliveryTimeSlot())
                .createTime(order.getCreateTime())
                .build();
    }

    /** Delivery + Order → 完整 VO（我的配送记录用） */
    private DeliveryVO buildVO(Delivery delivery, Order order,
                                Map<Long, Address> addressMap,
                                Map<Long, PickupPoint> pickupMap) {
        String addressText = resolveAddress(order, addressMap, pickupMap);
        String deliveryTypeText = DeliveryType.textOf(order.getDeliveryType());

        return DeliveryVO.builder()
                .id(delivery.getId())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .deliveryUserId(delivery.getDeliveryUserId())
                .status(delivery.getStatus())
                .statusText(DeliveryStatus.textOf(delivery.getStatus()))
                .deliveryTypeText(deliveryTypeText)
                .address(addressText)
                .deliveryTimeSlot(order.getDeliveryTimeSlot())
                .pickupTime(delivery.getPickupTime())
                .deliverTime(delivery.getDeliverTime())
                .createTime(delivery.getCreateTime())
                .build();
    }
}
