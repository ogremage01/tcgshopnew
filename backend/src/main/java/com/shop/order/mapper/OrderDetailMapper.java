package com.shop.order.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shop.admin.order.dto.AdminOrderCardProductDto;
import com.shop.admin.order.dto.AdminOrderCardProductGroupDto;
import com.shop.admin.order.dto.AdminOrderDetailDto;
import com.shop.admin.order.dto.AdminOrderManualProductDto;
import com.shop.admin.order.dto.AdminOrderSealedProductDto;
import com.shop.admin.order.dto.AdminOrderSupplyProductDto;
import com.shop.order.dto.OrderInfoDto;
import com.shop.order.dto.OrderProductDto;
import com.shop.order.dto.UserOrderCardProductDto;
import com.shop.order.dto.UserOrderCardProductGroupDto;
import com.shop.order.dto.UserOrderDetailDto;
import com.shop.order.dto.UserOrderManualProductDto;
import com.shop.order.dto.UserOrderSealedProductDto;
import com.shop.order.dto.UserOrderSupplyProductDto;

public final class OrderDetailMapper {

    private static final Logger log = LoggerFactory.getLogger(OrderDetailMapper.class);
    private static final String ORDER_DETAIL_LOG = "[ORDER-DETAIL]";

    private OrderDetailMapper() {
    }

    public static UserOrderDetailDto fromAdmin(AdminOrderDetailDto admin) {
        int adminCardGroups = admin.getOrderCardProductGroups() == null ? 0 : admin.getOrderCardProductGroups().size();
        int adminCardLines = admin.getOrderCardProductGroups() == null
                ? 0
                : admin.getOrderCardProductGroups().stream()
                        .mapToInt(g -> g.getProducts() == null ? 0 : g.getProducts().size())
                        .sum();
        int adminManualLines = admin.getOrderManualProducts() == null ? 0 : admin.getOrderManualProducts().size();
        int adminSealedLines = admin.getOrderSealedProducts() == null ? 0 : admin.getOrderSealedProducts().size();
        int adminSupplyLines = admin.getOrderSupplyProducts() == null ? 0 : admin.getOrderSupplyProducts().size();
        log.info(
                "{} OrderDetailMapper.fromAdmin input orderId={} adminCardGroups={} adminCardLines={} "
                        + "adminManualLines={} adminSealedLines={} adminSupplyLines={}",
                ORDER_DETAIL_LOG,
                admin.getOrderInfo() != null ? admin.getOrderInfo().getId() : null,
                adminCardGroups,
                adminCardLines,
                adminManualLines,
                adminSealedLines,
                adminSupplyLines);

        UserOrderDetailDto result = UserOrderDetailDto.builder()
                .orderInfo(OrderInfoDto.builder()
                        .id(admin.getOrderInfo().getId())
                        .guest(admin.getOrderInfo().getGuest())
                        .userId(admin.getOrderInfo().getUserId())
                        .recipientName(admin.getOrderInfo().getRecipientName())
                        .recipientAddress(admin.getOrderInfo().getRecipientAddress())
                        .recipientAddressDetail(admin.getOrderInfo().getRecipientAddressDetail())
                        .recipientPhone(admin.getOrderInfo().getRecipientPhone())
                        .recipientEmail(admin.getOrderInfo().getRecipientEmail())
                        .orderRequest(admin.getOrderInfo().getOrderRequest())
                        .orderStatus(admin.getOrderInfo().getOrderStatus())
                        .paymentStatus(admin.getOrderInfo().getPaymentStatus())
                        .deliveryCompany(admin.getOrderInfo().getDeliveryCompany())
                        .deliveryTrackingNumber(admin.getOrderInfo().getDeliveryTrackingNumber())
                        .deliveryMemo(admin.getOrderInfo().getDeliveryMemo())
                        .paymentCurrency(admin.getOrderInfo().getPaymentCurrency())
                        .totalProductAmount(admin.getOrderInfo().getTotalProductAmount())
                        .totalPaymentAmount(admin.getOrderInfo().getTotalPaymentAmount())
                        .usedPointAmount(admin.getOrderInfo().getUsedPointAmount())
                        .actualPaymentAmount(admin.getOrderInfo().getActualPaymentAmount())
                        .deliveryFee(admin.getOrderInfo().getDeliveryFee())
                        .totalQuantity(admin.getOrderInfo().getTotalQuantity())
                        .orderLineCount(admin.getOrderInfo().getOrderLineCount())
                        .paymentMethod(admin.getOrderInfo().getPaymentMethod())
                        .pgTransactionId(admin.getOrderInfo().getPgTransactionId())
                        .paymentCurrencyRateSnapshot(admin.getOrderInfo().getPaymentCurrencyRateSnapshot())
                        .totalEarnedPoints(admin.getOrderInfo().getTotalEarnedPoints())
                        .postalCode(admin.getOrderInfo().getPostalCode())
                        .build())
                .orderCardProductGroups(admin.getOrderCardProductGroups() == null
                        ? List.of()
                        : admin.getOrderCardProductGroups().stream()
                                .map(OrderDetailMapper::toCardGroup)
                                .collect(Collectors.toList()))
                .orderManualProducts(admin.getOrderManualProducts() == null
                        ? List.of()
                        : admin.getOrderManualProducts().stream()
                                .map(OrderDetailMapper::toManualProduct)
                                .collect(Collectors.toList()))
                .orderSealedProducts(admin.getOrderSealedProducts() == null
                        ? List.of()
                        : admin.getOrderSealedProducts().stream()
                                .map(OrderDetailMapper::toSealedProduct)
                                .collect(Collectors.toList()))
                .orderSupplyProducts(admin.getOrderSupplyProducts() == null
                        ? List.of()
                        : admin.getOrderSupplyProducts().stream()
                                .map(OrderDetailMapper::toSupplyProduct)
                                .collect(Collectors.toList()))
                .build();

        int userCardGroups = result.getOrderCardProductGroups() == null ? 0 : result.getOrderCardProductGroups().size();
        int userCardLines = result.getOrderCardProductGroups() == null
                ? 0
                : result.getOrderCardProductGroups().stream()
                        .mapToInt(g -> g.getProducts() == null ? 0 : g.getProducts().size())
                        .sum();
        log.info(
                "{} OrderDetailMapper.fromAdmin output orderId={} userCardGroups={} userCardLines={} "
                        + "userManualLines={} userSealedLines={} userSupplyLines={}",
                ORDER_DETAIL_LOG,
                result.getOrderInfo() != null ? result.getOrderInfo().getId() : null,
                userCardGroups,
                userCardLines,
                result.getOrderManualProducts() == null ? 0 : result.getOrderManualProducts().size(),
                result.getOrderSealedProducts() == null ? 0 : result.getOrderSealedProducts().size(),
                result.getOrderSupplyProducts() == null ? 0 : result.getOrderSupplyProducts().size());
        return result;
    }

    private static UserOrderCardProductGroupDto toCardGroup(AdminOrderCardProductGroupDto group) {
        return UserOrderCardProductGroupDto.builder()
                .game(group.getGame())
                .products(group.getProducts() == null
                        ? List.of()
                        : group.getProducts().stream()
                                .map(OrderDetailMapper::toCardProduct)
                                .collect(Collectors.toList()))
                .build();
    }

    private static UserOrderCardProductDto toCardProduct(AdminOrderCardProductDto admin) {
        UserOrderCardProductDto dto = new UserOrderCardProductDto();
        copyProductBase(admin, dto);
        dto.setGame(admin.getGame());
        dto.setSetCode(admin.getSetCode());
        dto.setSetName(admin.getSetName());
        dto.setReleaseDate(admin.getReleaseDate());
        dto.setSetNumber(admin.getSetNumber());
        dto.setPrintType(admin.getPrintType());
        dto.setPrinting(admin.getPrinting());
        dto.setLanguage(admin.getLanguage());
        dto.setCondition(admin.getCondition());
        log.debug(
                "{} toCardProduct orderProductId={} productId={} game={} setCode={} nameEn={} price={}",
                ORDER_DETAIL_LOG,
                dto.getId(),
                dto.getProductId(),
                dto.getGame(),
                dto.getSetCode(),
                dto.getProductNameEn(),
                dto.getPrice());
        return dto;
    }

    private static UserOrderManualProductDto toManualProduct(AdminOrderManualProductDto admin) {
        UserOrderManualProductDto dto = new UserOrderManualProductDto();
        copyProductBase(admin, dto);
        return dto;
    }

    private static UserOrderSealedProductDto toSealedProduct(AdminOrderSealedProductDto admin) {
        UserOrderSealedProductDto dto = new UserOrderSealedProductDto();
        copyProductBase(admin, dto);
        dto.setGame(admin.getGame());
        dto.setLanguage(admin.getLanguage());
        return dto;
    }

    private static UserOrderSupplyProductDto toSupplyProduct(AdminOrderSupplyProductDto admin) {
        UserOrderSupplyProductDto dto = new UserOrderSupplyProductDto();
        copyProductBase(admin, dto);
        dto.setSupplyType(admin.getSupplyType());
        dto.setMaker(admin.getMaker());
        return dto;
    }

    private static void copyProductBase(OrderProductDto source, OrderProductDto target) {
        target.setId(source.getId());
        target.setOrderInfoId(source.getOrderInfoId());
        target.setProductId(source.getProductId());
        target.setQuantity(source.getQuantity());
        target.setPrice(source.getPrice());
        target.setTotalPrice(source.getTotalPrice());
        target.setImageUrl(source.getImageUrl());
        target.setProductNameEn(source.getProductNameEn());
        target.setProductNameKo(source.getProductNameKo());
        target.setRewardPoints(source.getRewardPoints());
    }
}
