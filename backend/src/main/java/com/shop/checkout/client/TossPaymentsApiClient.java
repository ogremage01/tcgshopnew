package com.shop.checkout.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.checkout.exception.TossPaymentsUncertainStateException;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Component
public class TossPaymentsApiClient {

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_URL = "https://api.tosspayments.com/v1/payments/{paymentKey}";
    private static final String TOSS_CANCEL_URL_TEMPLATE = "https://api.tosspayments.com/v1/payments/%s/cancel";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(12);

    private final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                            .responseTimeout(RESPONSE_TIMEOUT)))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${TOSS_WIDGET_SECRET_KEY:}")
    private String tossWidgetSecretKey;

    public ResponseEntity<JsonNode> confirmPayment(String paymentKey, String orderId, long amount) {
        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount);
        return postToss(
                TOSS_CONFIRM_URL,
                body,
                "confirm",
                orderId,
                confirmIdempotencyKey(orderId, paymentKey));
    }

    public ResponseEntity<JsonNode> getPayment(String paymentKey) {
        try {
            String responseBody = webClient.get()
                    .uri(TOSS_PAYMENT_URL, paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(BLOCK_TIMEOUT);
            return ResponseEntity.ok(parseJson(responseBody));
        } catch (WebClientResponseException ex) {
            log.warn(
                    "Toss getPayment API failed status={} key={} body={}",
                    ex.getStatusCode().value(),
                    paymentKey,
                    ex.getResponseBodyAsString());
            JsonNode errorBody = parseJson(ex.getResponseBodyAsString());
            if (errorBody != null) {
                return ResponseEntity.status(ex.getStatusCode()).body(errorBody);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TOSS_GET_PAYMENT_FAILED");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Toss getPayment API uncertain key={}", paymentKey, ex);
            throw new TossPaymentsUncertainStateException("getPayment", paymentKey, ex);
        }
    }

    public ResponseEntity<JsonNode> cancelPayment(String paymentKey, String cancelReason) {
        return cancelPayment(paymentKey, cancelReason, null, null);
    }

    public ResponseEntity<JsonNode> cancelPayment(String paymentKey, String cancelReason, String idempotencyKey) {
        return cancelPayment(paymentKey, cancelReason, null, idempotencyKey);
    }

    /**
     * Toss 결제 취소. {@code cancelAmount}가 null이면 전액 취소, 값이 있으면 해당 금액만 부분 취소.
     */
    public ResponseEntity<JsonNode> cancelPayment(
            String paymentKey,
            String cancelReason,
            BigDecimal cancelAmount,
            String idempotencyKey) {
        String url = TOSS_CANCEL_URL_TEMPLATE.formatted(paymentKey);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            body.put("cancelAmount", toCancelAmountLong(cancelAmount));
        }
        return postToss(url, body, "cancel", paymentKey, idempotencyKey);
    }

    public static String confirmIdempotencyKey(String orderId, String paymentKey) {
        return "confirm:" + orderId + ":" + paymentKey;
    }

    public static String compensateCancelIdempotencyKey(String orderId, String paymentKey) {
        return "compensate-cancel:" + orderId + ":" + paymentKey;
    }

    private static long toCancelAmountLong(BigDecimal cancelAmount) {
        return cancelAmount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private ResponseEntity<JsonNode> postToss(
            String url,
            Map<String, Object> body,
            String action,
            String logKey,
            String idempotencyKey) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();
        try {
            String responseBody = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Idempotency-Key", key)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(BLOCK_TIMEOUT);
            return ResponseEntity.ok(parseJson(responseBody));
        } catch (WebClientResponseException ex) {
            log.warn(
                    "Toss {} API failed status={} key={} body={}",
                    action,
                    ex.getStatusCode().value(),
                    logKey,
                    ex.getResponseBodyAsString());
            JsonNode errorBody = parseJson(ex.getResponseBodyAsString());
            if (errorBody != null) {
                return ResponseEntity.status(ex.getStatusCode()).body(errorBody);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TOSS_" + action.toUpperCase() + "_FAILED");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Toss {} API uncertain key={}", action, logKey, ex);
            throw new TossPaymentsUncertainStateException(action, logKey, ex);
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            log.warn("Failed to parse Toss response JSON: {}", json);
            return null;
        }
    }

    private String authorizationHeader() {
        String secretKey = resolveSecretKey();
        return "Basic "
                + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String resolveSecretKey() {
        if (tossWidgetSecretKey != null && !tossWidgetSecretKey.isBlank()) {
            return tossWidgetSecretKey;
        }
        String envKey = System.getenv("TOSS_WIDGET_SECRET_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "TOSS_SECRET_KEY_MISSING");
    }
}
