package com.shop.order.adjustment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderInfoSnapshot;
import com.shop.order.entity.OrderProduct;
import com.shop.order.entity.OrderProductSnapshot;
import com.shop.order.repository.OrderInfoSnapshotRepository;
import com.shop.order.repository.OrderProductRepository;
import com.shop.order.repository.OrderProductSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderSnapshotService {

    private final OrderInfoSnapshotRepository orderInfoSnapshotRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderProductSnapshotRepository orderProductSnapshotRepository;

    public Long saveSnapshot(OrderInfo orderInfo) {
        OrderInfoSnapshot infoSnapshot = OrderInfoSnapshot.builder()
                .originalOrderInfoId(orderInfo.getId())
                .snapshotAt(LocalDateTime.now())
                .modifiedBy("ADMIN")
                .totalProductAmount(orderInfo.getTotalProductAmount())
                .deliveryFee(orderInfo.getDeliveryFee())
                .usedPointAmount(orderInfo.getUsedPointAmount())
                .actualPaymentAmount(orderInfo.getActualPaymentAmount())
                .totalQuantity(orderInfo.getTotalQuantity())
                .orderLineCount(orderInfo.getOrderLineCount())
                .orderStatus(orderInfo.getOrderStatus())
                .build();
        orderInfoSnapshotRepository.save(infoSnapshot);

        List<OrderProduct> lines = orderProductRepository.findByOrderInfoId(orderInfo.getId());
        List<OrderProductSnapshot> productSnapshots = lines.stream()
                .map(line -> OrderProductSnapshot.builder()
                        .orderInfoSnapshotId(infoSnapshot.getId())
                        .originalOrderProductId(line.getId())
                        .productId(line.getProductId())
                        .productTable(line.getProductTable())
                        .quantity(line.getQuantity())
                        .price(line.getPrice())
                        .totalPrice(line.getTotalPrice())
                        .snapshotUnitPrice(line.getSnapshotUnitPrice())
                        .productNameKo(line.getProductNameKo())
                        .productNameEn(line.getProductNameEn())
                        .build())
                .collect(Collectors.toList());
        orderProductSnapshotRepository.saveAll(productSnapshots);
        return infoSnapshot.getId();
    }
}
