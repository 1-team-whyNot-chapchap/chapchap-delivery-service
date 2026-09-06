package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryResultType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryResultCorrectionRepository;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryResultCorrectionRequest;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryResultCorrectionResponse;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryResultNotCorrectableException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminDeliveryResultCorrectionService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Set<String> COMPLETION_FIELDS =
        Set.of("actual_handoff_type", "storage_location");
    private static final Set<String> FAILURE_FIELDS =
        Set.of("failure_code", "failure_detail");

    private final DeliveryAccessService accessService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryRecipientSnapshotRepository recipientSnapshotRepository;
    private final DeliveryResultCorrectionRepository correctionRepository;
    private final AuditHistoryRepository auditHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdminDeliveryResultCorrectionResponse correctCompletion(
        Long actorId, UserRole role, String deliveryPublicId,
        AdminDeliveryResultCorrectionRequest request
    ) {
        accessService.validateAdminAccess(actorId, role);
        Delivery delivery = lockDelivery(deliveryPublicId);
        if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
            throw new DeliveryResultNotCorrectableException();
        }

        DeliveryCompletion completion = completionRepository.findByDeliveryId(delivery.getId())
            .orElseThrow(DeliveryResultNotCorrectableException::new);
        List<DeliveryResultCorrection> history =
            correctionRepository.findAllByDelivery_IdOrderByIdAsc(delivery.getId());

        Map<String, String> originals = new HashMap<>();
        originals.put("actual_handoff_type", completion.getActualHandoffType().name());
        originals.put("storage_location", completion.getStorageLocation());

        Map<String, String> current = currentValues(
            history, DeliveryResultType.COMPLETION, originals
        );
        validateRequest(request, COMPLETION_FIELDS, current);
        Map<String, String> after = apply(current, request);
        validateCompletion(delivery, completion, after);

        return save(
            delivery, DeliveryResultType.COMPLETION, request, current, after, actorId
        );
    }

    @Transactional
    public AdminDeliveryResultCorrectionResponse correctFailure(
        Long actorId, UserRole role, String deliveryPublicId,
        AdminDeliveryResultCorrectionRequest request
    ) {
        accessService.validateAdminAccess(actorId, role);
        Delivery delivery = lockDelivery(deliveryPublicId);
        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new DeliveryResultNotCorrectableException();
        }

        DeliveryFailure failure = failureRepository.findByDeliveryId(delivery.getId())
            .orElseThrow(DeliveryResultNotCorrectableException::new);
        List<DeliveryResultCorrection> history =
            correctionRepository.findAllByDelivery_IdOrderByIdAsc(delivery.getId());

        Map<String, String> originals = new HashMap<>();
        originals.put("failure_code", failure.getFailureCode().name());
        originals.put("failure_detail", failure.getFailureDetail());

        Map<String, String> current = currentValues(
            history, DeliveryResultType.FAILURE, originals
        );
        validateRequest(request, FAILURE_FIELDS, current);
        Map<String, String> after = apply(current, request);
        validateFailure(after);

        return save(
            delivery, DeliveryResultType.FAILURE, request, current, after, actorId
        );
    }

    private Delivery lockDelivery(String deliveryPublicId) {
        return deliveryRepository.findByDeliveryPublicIdForUpdate(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
    }

    private void validateRequest(
        AdminDeliveryResultCorrectionRequest request
        , Set<String> allowedFields
        , Map<String, String> current
    ) {
        if (request == null
            || request.changes() == null
            || request.changes().isEmpty()
            || request.changes().size() > 2
            || (request.reasonDetail() != null && request.reasonDetail().length() > 500)) {
            throw new DeliveryResultNotCorrectableException();
        }
        if (request.reasonCode() == null) {
            throw new DeliveryResultNotCorrectableException();
        }
        if (request.reasonCode().requiresDetail() && isBlank(request.reasonDetail())) {
            throw new OtherReasonDetailRequiredException();
        }

        Set<String> fields = new HashSet<>();
        for (AdminDeliveryResultCorrectionRequest.Change change : request.changes()) {
            if (change == null
                || isBlank(change.fieldName())
                || change.fieldName().length() > 50
                || (change.afterValue() != null && change.afterValue().length() > 500)) {
                throw new DeliveryResultNotCorrectableException();
            }

            String fieldName = change.fieldName().trim();
            if (!fieldName.equals(change.fieldName())
                || !allowedFields.contains(fieldName)
                || !fields.add(fieldName)) {
                throw new DeliveryResultNotCorrectableException();
            }

            String normalizedAfterValue = normalizeFieldValue(fieldName, change.afterValue());
            validateFieldLength(fieldName, normalizedAfterValue);
            if (Objects.equals(current.get(fieldName), normalizedAfterValue)) {
                throw new DeliveryResultNotCorrectableException();
            }
        }
    }

    private void validateCompletion(
        Delivery delivery
        , DeliveryCompletion completion
        , Map<String, String> values
    ) {
        ActualHandoffType handoffType = parseHandoffType(values.get("actual_handoff_type"));
        String storageLocation = values.get("storage_location");
        boolean hasPhoto = photoRepository.findByDeliveryCompletionId(completion.getId())
            .isPresent();

        if (handoffType == ActualHandoffType.DIRECT) {
            if (!isBlank(storageLocation) || hasPhoto) {
                throw new DeliveryResultNotCorrectableException();
            }
            return;
        }

        if (!Boolean.TRUE.equals(delivery.getTermsAgreed())
            || isBlank(storageLocation)
            || !hasPhoto) {
            throw new DeliveryResultNotCorrectableException();
        }

        RequestHandoffType requestedHandoffType = delivery.getRequestHandoffType();
        if (requestedHandoffType == RequestHandoffType.DIRECT
            && (completion.getContactAttemptedAt() == null
                || isBlank(completion.getContactResult()))) {
            throw new DeliveryResultNotCorrectableException();
        }

        if (handoffType == ActualHandoffType.OTHER
            && requestedHandoffType != RequestHandoffType.DIRECT) {
            if (requestedHandoffType != RequestHandoffType.OTHER) {
                throw new DeliveryResultNotCorrectableException();
            }

            DeliveryRecipientSnapshot snapshot = recipientSnapshotRepository
                .findById(delivery.getId())
                .orElseThrow(DeliveryResultNotCorrectableException::new);
            if (isBlank(snapshot.getOtherRequest())) {
                throw new DeliveryResultNotCorrectableException();
            }
        }
    }

    private void validateFailure(Map<String, String> values) {
        DeliveryFailureCode code = parseFailureCode(values.get("failure_code"));
        if (code.requiresDetail() && isBlank(values.get("failure_detail"))) {
            throw new DeliveryResultNotCorrectableException();
        }
    }

    private ActualHandoffType parseHandoffType(String value) {
        try {
            return ActualHandoffType.valueOf(value);
        } catch (RuntimeException exception) {
            throw new DeliveryResultNotCorrectableException();
        }
    }

    private DeliveryFailureCode parseFailureCode(String value) {
        try {
            return DeliveryFailureCode.valueOf(value);
        } catch (RuntimeException exception) {
            throw new DeliveryResultNotCorrectableException();
        }
    }

    private Map<String, String> currentValues(
        List<DeliveryResultCorrection> history
        , DeliveryResultType resultType
        , Map<String, String> originals
    ) {
        Map<String, String> current = new HashMap<>(originals);
        history.stream()
            .filter(correction -> correction.getResultType() == resultType)
            .forEach(correction ->
                current.put(correction.getFieldName(), correction.getAfterValue())
            );
        return current;
    }

    private Map<String, String> apply(
        Map<String, String> current, AdminDeliveryResultCorrectionRequest request
    ) {
        Map<String, String> after = new HashMap<>(current);
        request.changes().forEach(change ->
            after.put(
                change.fieldName(),
                normalizeFieldValue(change.fieldName(), change.afterValue())
            )
        );
        return after;
    }

    private AdminDeliveryResultCorrectionResponse save(
        Delivery delivery
        , DeliveryResultType resultType
        , AdminDeliveryResultCorrectionRequest request
        , Map<String, String> current
        , Map<String, String> after
        , Long actorId
    ) {
        LocalDateTime correctedAt = LocalDateTime.now(KST);
        String reasonDetail = normalizeToNull(request.reasonDetail());
        List<DeliveryResultCorrection> corrections = new ArrayList<>();

        request.changes().forEach(change -> corrections.add(new DeliveryResultCorrection(
            delivery
            , resultType
            , change.fieldName()
            , current.get(change.fieldName())
            , after.get(change.fieldName())
            , request.reasonCode()
            , reasonDetail
            , actorId
            , correctedAt
        )));

        List<DeliveryResultCorrection> saved = correctionRepository.saveAll(corrections);
        auditHistoryRepository.save(AuditHistory.record(
            "DELIVERY"
            , delivery.getId()
            , resultType + "_CORRECTED"
            , actorId
            , AuditActorType.ADMIN
            , request.reasonCode().name()
            , reasonDetail
            , json(current)
            , json(after)
            , correctedAt
        ));

        return new AdminDeliveryResultCorrectionResponse(
            delivery.getDeliveryPublicId()
            , resultType
            , saved.stream().map(correction -> new AdminDeliveryResultCorrectionResponse.Change(
                correction.getId()
                , correction.getFieldName()
                , correction.getBeforeValue()
                , correction.getAfterValue()
            )).toList()
            , actorId
            , correctedAt.atZone(KST).toOffsetDateTime()
        );
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize correction audit values", exception);
        }
    }


    private void validateFieldLength(String fieldName, String value) {
        if (value == null) {
            return;
        }
        int maxLength = switch (fieldName) {
            case "storage_location" -> 100;
            case "failure_detail" -> 500;
            default -> 500;
        };
        if (value.length() > maxLength) {
            throw new DeliveryResultNotCorrectableException();
        }
    }

    private String normalizeFieldValue(String fieldName, String value) {
        String normalized = normalize(value);
        if (("storage_location".equals(fieldName) || "failure_detail".equals(fieldName))
            && isBlank(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return isBlank(normalized) ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
