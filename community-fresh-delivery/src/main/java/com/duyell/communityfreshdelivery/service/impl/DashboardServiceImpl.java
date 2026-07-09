package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.dto.DashboardVO;
import com.duyell.communityfreshdelivery.dto.HotProductVO;
import com.duyell.communityfreshdelivery.dto.SalesStatsVO;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>数据看板服务实现</h2>
 *
 * <p>所有统计基于 {@code order} + {@code order_item} 表实时计算.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public DashboardVO today() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // 今日所有订单
        List<Order> todayOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreateTime, todayStart)
                        .lt(Order::getCreateTime, todayEnd)
        );

        long newOrders = todayOrders.stream()
                .filter(o -> o.getStatus() >= OrderStatus.PENDING_ACCEPT.getCode())
                .count();
        long pendingAccept = countByStatus(todayOrders, OrderStatus.PENDING_ACCEPT);
        long pendingSorting = countByStatus(todayOrders, OrderStatus.PENDING_SORTING);
        long pendingDelivery = countByStatus(todayOrders, OrderStatus.PENDING_DELIVERY);
        long inDelivery = countByStatus(todayOrders, OrderStatus.IN_DELIVERY);
        long completed = todayOrders.stream()
                .filter(o -> o.getStatus() >= OrderStatus.RECEIVED.getCode()
                        && o.getStatus() != OrderStatus.CANCELLED.getCode()
                        && o.getStatus() != OrderStatus.REFUNDING.getCode()
                        && o.getStatus() != OrderStatus.REFUNDED.getCode())
                .count();

        // 今日销售额（已支付及以上状态）
        BigDecimal todayRevenue = todayOrders.stream()
                .filter(o -> o.getStatus() >= OrderStatus.PENDING_ACCEPT.getCode())
                .map(Order::getActualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardVO.builder()
                .newOrders(newOrders)
                .pendingAccept(pendingAccept)
                .pendingSorting(pendingSorting)
                .pendingDelivery(pendingDelivery)
                .inDelivery(inDelivery)
                .completed(completed)
                .todayRevenue(todayRevenue)
                .build();
    }

    @Override
    public List<SalesStatsVO> salesStats(String period) {
        // 查所有已支付及以上的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, OrderStatus.PENDING_ACCEPT.getCode())
                        .orderByAsc(Order::getCreateTime)
        );

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        DateTimeFormatter fmt = switch (period) {
            case "month" -> DateTimeFormatter.ofPattern("yyyy-MM");
            case "week" -> {
                // 按周统计用 ISO week
                yield DateTimeFormatter.ofPattern("yyyy-'W'ww");
            }
            default -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
        };

        // 按 period 分组聚合
        Map<String, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().format(fmt),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SalesStatsVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            List<Order> periodOrders = entry.getValue();
            long orderCount = periodOrders.size();
            BigDecimal totalAmount = periodOrders.stream()
                    .map(Order::getActualAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 加载订单明细求总件数
            Set<Long> orderIds = periodOrders.stream()
                    .map(Order::getId)
                    .collect(Collectors.toSet());
            long totalQuantity = 0;
            if (!orderIds.isEmpty()) {
                totalQuantity = orderItemMapper.selectList(
                                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
                        ).stream()
                        .mapToLong(OrderItem::getQuantity)
                        .sum();
            }

            result.add(SalesStatsVO.builder()
                    .period(entry.getKey())
                    .totalQuantity(totalQuantity)
                    .totalAmount(totalAmount)
                    .orderCount(orderCount)
                    .build());
        }

        return result;
    }

    @Override
    public List<HotProductVO> hotProducts() {
        // 查所有已支付订单的明细
        List<Order> paidOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, OrderStatus.PENDING_ACCEPT.getCode())
        );

        if (paidOrders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> orderIds = paidOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toSet());

        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );

        // 按商品ID聚合
        Map<Long, List<OrderItem>> grouped = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId));

        // 计算每个商品的总销量 + 销售额
        List<HotProductVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : grouped.entrySet()) {
            long totalQty = entry.getValue().stream().mapToLong(OrderItem::getQuantity).sum();
            BigDecimal totalAmt = entry.getValue().stream()
                    .map(i -> i.getActualAmount() != null && i.getActualAmount().compareTo(BigDecimal.ZERO) > 0
                            ? i.getActualAmount() : i.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(HotProductVO.builder()
                    .productId(entry.getKey())
                    .totalQuantity(totalQty)
                    .totalAmount(totalAmt)
                    .build());
        }

        // 按销量降序取 TOP 10
        result.sort((a, b) -> Long.compare(b.getTotalQuantity(), a.getTotalQuantity()));
        List<HotProductVO> top10 = result.size() > 10 ? result.subList(0, 10) : result;

        // 批量加载商品名称
        Set<Long> productIds = top10.stream()
                .map(HotProductVO::getProductId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        top10.forEach(h -> h.setProductName(nameMap.getOrDefault(h.getProductId(), "未知商品")));

        return top10;
    }

    // ==================== 内部方法 ====================

    private long countByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(o -> o.getStatus().equals(status.getCode()))
                .count();
    }
}
