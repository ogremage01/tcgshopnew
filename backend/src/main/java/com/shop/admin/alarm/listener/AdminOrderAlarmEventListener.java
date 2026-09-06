package com.shop.admin.alarm.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.shop.admin.alarm.dto.AlarmDto;
import com.shop.admin.alarm.service.AdminAlarmService;
import com.shop.order.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOrderAlarmEventListener {

    private final AdminAlarmService adminAlarmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 알림 이벤트 수신: orderId={}", event.getOrderId());
        adminAlarmService.send(AlarmDto.builder()
                .title("새로운 주문이 들어왔습니다.")
                .content("새로운 주문이 들어왔습니다. 확인해주세요.")
                .link("/admin/order/detail/" + event.getOrderId())
                .build());
    }
}
