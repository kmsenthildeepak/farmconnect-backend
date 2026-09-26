package com.farmconnect.service.impl;
import org.springframework.transaction.annotation.Transactional;
import com.farmconnect.dto.response.SalesReportResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.Order;
import com.farmconnect.entity.OrderItem;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.repository.OrderItemRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl {
    private final FarmerRepository farmerRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * @param startDate optional - if both start/end are supplied, totalOrders/totalRevenue/
     *                   totalUnitsSold/topProducts are scoped to that inclusive date range.
     *                   If either is null, they cover all-time (existing/default behavior).
     * @param endDate   optional, see above. Must not be before startDate.
     *
     * The four period cards (today/weekly/monthly/yearly) are ALWAYS computed
     * relative to "now", independent of the date-range filter - they are the
     * dashboard's fixed quick-glance cards, not affected by the custom filter.
     */
    @Transactional(readOnly = true)
    public SalesReportResponse mySalesReport(LocalDate startDate, LocalDate endDate) {
        Farmer farmer = farmerRepository.findByUser_UserId(SecurityUtil.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        // "Sales" = DELIVERED order items only, consistent with the original
        // implementation's definition of revenue/units-sold.
        List<OrderItem> delivered = orderItemRepository
                .findByFarmer_FarmerIdOrderByOrderItemIdDesc(farmer.getFarmerId())
                .stream()
                .filter(i -> i.getOrder() != null && i.getOrder().getOrderStatus() == Order.OrderStatus.DELIVERED)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay(); // ISO week, Monday start
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();

        SalesReportResponse.PeriodSummary todayCard = summarize(delivered, todayStart, now);
        SalesReportResponse.PeriodSummary weeklyCard = summarize(delivered, weekStart, now);
        SalesReportResponse.PeriodSummary monthlyCard = summarize(delivered, monthStart, now);
        SalesReportResponse.PeriodSummary yearlyCard = summarize(delivered, yearStart, now);

        List<OrderItem> scoped;
        if (startDate != null && endDate != null) {
            LocalDateTime rangeStart = startDate.atStartOfDay();
            LocalDateTime rangeEndExclusive = endDate.plusDays(1).atStartOfDay(); // end date is inclusive
            scoped = delivered.stream()
                    .filter(i -> withinRange(i, rangeStart, rangeEndExclusive))
                    .toList();
        } else {
            scoped = delivered;
        }

        Map<Long, SalesReportResponse.ProductSales> byProduct = new LinkedHashMap<>();
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;

        for (OrderItem item : scoped) {
            long qty = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal subtotal = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
            units += qty;
            revenue = revenue.add(subtotal);

            Long productId = item.getProduct().getProductId();
            SalesReportResponse.ProductSales existing = byProduct.get(productId);
            if (existing == null) {
                byProduct.put(productId, SalesReportResponse.ProductSales.builder()
                        .productId(productId)
                        .productName(item.getProduct().getProductName())
                        .unit(item.getProduct().getUnit() == null ? null : item.getProduct().getUnit().name())
                        .unitsSold(qty)
                        .revenue(subtotal)
                        .build());
            } else {
                existing.setUnitsSold(existing.getUnitsSold() + qty);
                existing.setRevenue(existing.getRevenue().add(subtotal));
            }
        }

        List<SalesReportResponse.ProductSales> topProducts = byProduct.values().stream()
                .sorted(Comparator.comparingLong(SalesReportResponse.ProductSales::getUnitsSold).reversed())
                .limit(10)
                .toList();

        return SalesReportResponse.builder()
                .totalOrders(scoped.stream().map(OrderItem::getOrder).map(Order::getOrderId).distinct().count())
                .totalRevenue(revenue)
                .totalUnitsSold(units)
                .topProducts(topProducts)
                .today(todayCard)
                .weekly(weeklyCard)
                .monthly(monthlyCard)
                .yearly(yearlyCard)
                .build();
    }

    private boolean withinRange(OrderItem item, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        LocalDateTime createdAt = item.getOrder().getCreatedAt();
        return createdAt != null && !createdAt.isBefore(startInclusive) && createdAt.isBefore(endExclusive);
    }

    private SalesReportResponse.PeriodSummary summarize(List<OrderItem> items, LocalDateTime startInclusive, LocalDateTime endInclusive) {
        long orders = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        long units = 0;

        java.util.Set<Long> orderIds = new java.util.HashSet<>();
        for (OrderItem item : items) {
            LocalDateTime createdAt = item.getOrder().getCreatedAt();
            if (createdAt == null || createdAt.isBefore(startInclusive) || createdAt.isAfter(endInclusive)) {
                continue;
            }
            long qty = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal subtotal = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
            units += qty;
            revenue = revenue.add(subtotal);
            orderIds.add(item.getOrder().getOrderId());
        }
        orders = orderIds.size();

        return SalesReportResponse.PeriodSummary.builder()
                .orders(orders)
                .revenue(revenue)
                .unitsSold(units)
                .build();
    }
}
