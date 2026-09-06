package com.shop.checkout.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.checkout.domain.DraftStatus;
import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.dto.CheckoutConfirmFailureResponse;
import com.shop.checkout.entity.CheckoutDraft;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.checkout.lines.CheckoutLineFailures;
import com.shop.checkout.lines.CheckoutLineHandler;
import com.shop.checkout.lines.CheckoutLineHandlerRegistry;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.common.identity.UserIdentity;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutDraftValidationServiceImpl implements CheckoutDraftValidationService {

    private final CheckoutDraftRepository checkoutDraftRepository;
    private final ProductSearchMapRepository productSearchMapRepository;
    private final CheckoutLineHandlerRegistry lineHandlerRegistry;
    private final UserService userService;

    @Override
    @Transactional
    public ResponseEntity<?> validateDraft(String draftPublicId, UserIdentity identity) {
        CheckoutDraft draft = checkoutDraftRepository.findByPublicId(draftPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND"));

        assertOwner(draft, identity);

        if (draft.getStatus() == DraftStatus.CONFIRMED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(simpleFailure("DRAFT_ALREADY_CONFIRMED", "Draft is already confirmed."));
        }

        return validateReadyDraft(draft, identity)
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    @Override
    public void assertOwner(CheckoutDraft draft, UserIdentity identity) {
        if (identity.isUser()) {
            Long uid = userService.findUserIdByPublicId(identity.getUserPublicId()).orElse(null);
            if (uid == null || !uid.equals(draft.getUserId())) {
                log.warn("DRAFT_FORBIDDEN: user identity mismatch draftPublicId={} draftUserId={} requestUserPublicId={}",
                        draft.getPublicId(), draft.getUserId(), identity.getUserPublicId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DRAFT_FORBIDDEN");
            }
            return;
        }
        if (identity.getGuestId() == null || !identity.getGuestId().equals(draft.getGuestId())) {
            log.warn("DRAFT_FORBIDDEN: guest identity mismatch draftPublicId={} draftGuestId={} requestGuestId={}",
                    draft.getPublicId(), draft.getGuestId(), identity.getGuestId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DRAFT_FORBIDDEN");
        }
    }

    @Override
    public Optional<ResponseEntity<?>> validateReadyDraft(CheckoutDraft draft, UserIdentity identity) {
        assertOwner(draft, identity);

        if (draft.getStatus() == DraftStatus.REPLACED) {
            return Optional.of(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(simpleFailure("DRAFT_REPLACED", "Draft was replaced by a newer checkout.")));
        }

        if (draft.getStatus() == DraftStatus.EXPIRED
                || (draft.getStatus() == DraftStatus.READY && LocalDateTime.now().isAfter(draft.getExpiresAt()))) {
            if (draft.getStatus() == DraftStatus.READY) {
                draft.setStatus(DraftStatus.EXPIRED);
            }
            return Optional.of(ResponseEntity.status(HttpStatus.GONE)
                    .body(simpleFailure("DRAFT_EXPIRED", "Checkout draft expired.")));
        }

        if (draft.getStatus() != DraftStatus.READY) {
            return Optional.of(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(simpleFailure("DRAFT_NOT_CONFIRMABLE", "Draft cannot be confirmed.")));
        }

        CheckoutConfirmFailureResponse recipientInfoError = validateRequiredRecipientInfo(draft);
        if (recipientInfoError != null) {
            return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(recipientInfoError));
        }

        draft.getItems().size();

        List<CheckoutConfirmFailedItem> failures = validateLineItems(draft);
        if (!failures.isEmpty()) {
            String code = aggregateFailureCode(failures);
            return Optional.of(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CheckoutConfirmFailureResponse.builder()
                            .code(code)
                            .message("Checkout validation failed.")
                            .failedItems(failures)
                            .build()));
        }

        CheckoutConfirmFailureResponse integrityError = verifyAmountInvariants(draft);
        if (integrityError != null) {
            return Optional.of(ResponseEntity.status(HttpStatus.CONFLICT).body(integrityError));
        }

        return Optional.empty();
    }

    private CheckoutConfirmFailureResponse verifyAmountInvariants(CheckoutDraft draft) {
        BigDecimal subtotalFromLines = BigDecimal.ZERO;

        for (CheckoutDraftItem line : draft.getItems()) {
            BigDecimal expectedLineTotal = line.getSnapshotUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()));
            if (expectedLineTotal.compareTo(line.getSnapshotTotalPrice()) != 0) {
                return amountIntegrityError("라인 총액이 단가 × 수량과 일치하지 않습니다.");
            }
            subtotalFromLines = subtotalFromLines.add(line.getSnapshotTotalPrice());
        }

        BigDecimal subtotal = draft.getSubtotalAmount() != null ? draft.getSubtotalAmount() : BigDecimal.ZERO;
        if (subtotalFromLines.compareTo(subtotal) != 0) {
            return amountIntegrityError("라인 합계가 주문 소계와 일치하지 않습니다.");
        }

        BigDecimal delivery = draft.getDeliveryFee() != null ? draft.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal usedPts = draft.getUsedPointAmount() != null ? draft.getUsedPointAmount() : BigDecimal.ZERO;
        BigDecimal total = draft.getTotalAmount() != null ? draft.getTotalAmount() : BigDecimal.ZERO;

        if (usedPts.compareTo(BigDecimal.ZERO) < 0) {
            return amountIntegrityError("사용 포인트가 음수입니다.");
        }
        if (usedPts.compareTo(subtotal.add(delivery)) > 0) {
            return amountIntegrityError("사용 포인트가 결제 가능 금액을 초과합니다.");
        }

        BigDecimal expectedTotal = subtotal.add(delivery).subtract(usedPts).max(BigDecimal.ZERO);
        if (expectedTotal.compareTo(total) != 0) {
            return amountIntegrityError("최종 결제 금액이 소계 + 배송비 - 포인트와 일치하지 않습니다.");
        }

        return null;
    }

    private CheckoutConfirmFailureResponse amountIntegrityError(String message) {
        return CheckoutConfirmFailureResponse.builder()
                .code("AMOUNT_INTEGRITY_ERROR")
                .message(message)
                .failedItems(List.of())
                .build();
    }

    private List<CheckoutConfirmFailedItem> validateLineItems(CheckoutDraft draft) {
        List<CheckoutConfirmFailedItem> failures = new ArrayList<>();
        List<CheckoutDraftItem> lines = new ArrayList<>(draft.getItems());
        lines.sort(Comparator.comparing(CheckoutDraftItem::getProductPublicId));

        for (CheckoutDraftItem line : lines) {
            CheckoutConfirmFailedItem failure = validateDraftLine(line);
            if (failure != null) {
                failures.add(failure);
            }
        }
        return failures;
    }

    private CheckoutConfirmFailedItem validateDraftLine(CheckoutDraftItem line) {
        ProductSearchMap map = productSearchMapRepository.findById(line.getSearchMapId()).orElse(null);
        if (map == null || !Boolean.TRUE.equals(map.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, 0L);
        }
        if (!map.getTableName().name().equals(line.getTableName())
                || !Objects.equals(map.getSourceId(), line.getSourceId())
                || !Objects.equals(map.getProductId(), line.getProductPublicId())) {
            return CheckoutLineFailures.unavailable(line, 0L);
        }

        Optional<ProductTableEnum> tableOpt = parseProductTable(line.getTableName());
        if (tableOpt.isEmpty()) {
            return CheckoutLineFailures.unsupportedTable(line);
        }

        CheckoutLineHandler handler = lineHandlerRegistry.find(tableOpt.get()).orElse(null);
        if (handler == null) {
            return CheckoutLineFailures.unsupportedTable(line);
        }

        return handler.validate(line, map);
    }

    private static Optional<ProductTableEnum> parseProductTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ProductTableEnum.valueOf(tableName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String aggregateFailureCode(List<CheckoutConfirmFailedItem> failures) {
        if (failures.isEmpty()) {
            return "UNKNOWN";
        }
        String first = failures.get(0).getReason();
        boolean allSame = failures.stream().allMatch(f -> Objects.equals(first, f.getReason()));
        return allSame ? first : "MULTIPLE_FAILURES";
    }

    private CheckoutConfirmFailureResponse simpleFailure(String code, String message) {
        return CheckoutConfirmFailureResponse.builder()
                .code(code)
                .message(message)
                .failedItems(List.of())
                .build();
    }

    private CheckoutConfirmFailureResponse validateRequiredRecipientInfo(CheckoutDraft draft) {
        if (isBlank(draft.getRecipientName())) {
            return simpleFailure("RECIPIENT_NAME_REQUIRED", "Recipient name is required.");
        }
        if (isBlank(draft.getRecipientEmail())) {
            return simpleFailure("RECIPIENT_EMAIL_REQUIRED", "Recipient email is required.");
        }
        if (isBlank(draft.getRecipientPhone())) {
            return simpleFailure("RECIPIENT_PHONE_REQUIRED", "Recipient phone is required.");
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
