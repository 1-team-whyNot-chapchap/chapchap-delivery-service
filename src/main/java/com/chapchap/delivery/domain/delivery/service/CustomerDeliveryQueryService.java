package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletionPhoto;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryResultCorrectionRepository;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryDetailResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryListItemResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryListResponse;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryInfoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerDeliveryQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryRepository deliveryRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryResultCorrectionRepository correctionRepository;

    @Transactional(readOnly = true)
    public CustomerDeliveryListResponse getMyDeliveries(
        Long customerId
        , UserRole role
        , LocalDate dateFrom
        , LocalDate dateTo
        , DeliverySlotCode slotCode
        , DeliveryStatus status
        , Pageable pageable
    ) {
        validateCustomer(role);
        validateDateRange(dateFrom, dateTo);

        Page<Delivery> deliveries = deliveryRepository.findAllForCustomer(
            customerId
            , dateFrom
            , dateTo
            , slotCode
            , status
            , DeliveryQueryPageable.customer(pageable)
        );
        RelatedData relatedData = loadRelatedData(deliveries.getContent());

        return CustomerDeliveryListResponse.from(
            deliveries.map(delivery -> toListItem(delivery, relatedData))
        );
    }

    @Transactional(readOnly = true)
    public CustomerDeliveryDetailResponse getMyDelivery(
        Long customerId
        , UserRole role
        , String deliveryPublicId
    ) {
        validateCustomer(role);
        Delivery delivery = deliveryRepository.findDetailByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);

        if (!delivery.getCustomerId().equals(customerId)) {
            throw new DeliveryAccessForbiddenException();
        }

        RelatedData data = loadRelatedData(java.util.List.of(delivery));
        DeliveryCompletion completion = data.completions().get(delivery.getId());
        DeliveryFailure failure = data.failures().get(delivery.getId());
        DeliveryDelay delay = data.delays().get(delivery.getId());
        CompletionValues completionValues = completionValues(delivery.getId(), completion, data);

        return new CustomerDeliveryDetailResponse(
            delivery.getDeliveryPublicId()
            , delivery.getSourceOrderId()
            , delivery.getDeliveryGroup().getDeliveryDate()
            , delivery.getDeliveryGroup().getSlot().getCode()
            , delivery.getStatus()
            , delay != null
            , delay == null ? null : delay.getDelayMinutes()
            , delivery.getRequestHandoffType()
            , completionValues == null ? null : completionValues.handoffType()
            , completion == null ? null : toOffset(completion.getCompletedAt())
            , completionValues == null ? null : completionValues.storageLocation()
            , customerFailureMessage(failure)
            , completion != null && data.photoCompletionIds().containsKey(completion.getId())
            , new CustomerDeliveryDetailResponse.Menu(
                delivery.getRotationMenuId()
                , delivery.getMenuNameSnapshot()
                , delivery.getLunchboxQuantity()
            )
        );
    }

    private CustomerDeliveryListItemResponse toListItem(
        Delivery delivery
        , RelatedData data
    ) {
        DeliveryCompletion completion = data.completions().get(delivery.getId());
        CompletionValues completionValues = completionValues(delivery.getId(), completion, data);
        return new CustomerDeliveryListItemResponse(
            delivery.getDeliveryPublicId()
            , delivery.getSourceOrderId()
            , delivery.getDeliveryGroup().getDeliveryDate()
            , delivery.getDeliveryGroup().getSlot().getCode()
            , delivery.getStatus()
            , data.delays().containsKey(delivery.getId())
            , completion == null ? null : toOffset(completion.getCompletedAt())
            , completionValues == null ? null : completionValues.handoffType()
            , completion != null && data.photoCompletionIds().containsKey(completion.getId())
        );
    }

    private RelatedData loadRelatedData(Collection<Delivery> deliveries) {
        java.util.List<Long> deliveryIds = deliveries.stream().map(Delivery::getId).toList();
        if (deliveryIds.isEmpty()) {
            return RelatedData.empty();
        }

        Map<Long, DeliveryCompletion> completions = completionRepository
            .findAllByDeliveryIdIn(deliveryIds).stream()
            .collect(Collectors.toMap(c -> c.getDelivery().getId(), Function.identity()));
        Map<Long, DeliveryFailure> failures = failureRepository
            .findAllByDeliveryIdIn(deliveryIds).stream()
            .collect(Collectors.toMap(f -> f.getDelivery().getId(), Function.identity()));
        Map<Long, DeliveryDelay> delays = delayRepository
            .findAllByDeliveryIdIn(deliveryIds).stream()
            .collect(Collectors.toMap(d -> d.getDelivery().getId(), Function.identity()));
        java.util.List<Long> completionIds = completions.values().stream()
            .map(DeliveryCompletion::getId).toList();
        Map<Long, DeliveryCompletionPhoto> photos = completionIds.isEmpty()
            ? Map.of()
            : photoRepository.findAllByDeliveryCompletionIdIn(completionIds).stream()
                .collect(Collectors.toMap(p -> p.getDeliveryCompletion().getId(), Function.identity()));
        Map<Long, java.util.List<com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection>>
            corrections = correctionRepository.findAllByDelivery_IdInOrderByIdAsc(deliveryIds)
                .stream().collect(Collectors.groupingBy(
                    correction -> correction.getDelivery().getId()
                ));
        return new RelatedData(completions, failures, delays, photos, corrections);
    }

    private CompletionValues completionValues(
        Long deliveryId, DeliveryCompletion completion, RelatedData data
    ) {
        if (completion == null) {
            return null;
        }
        ActualHandoffType handoffType = completion.getActualHandoffType();
        String storageLocation = completion.getStorageLocation();
        for (var correction : data.corrections().getOrDefault(deliveryId, java.util.List.of())) {
            if (correction.getResultType()
                != com.chapchap.delivery.domain.delivery.constant.DeliveryResultType.COMPLETION) {
                continue;
            }
            if ("actual_handoff_type".equals(correction.getFieldName())) {
                handoffType = ActualHandoffType.valueOf(correction.getAfterValue());
            } else if ("storage_location".equals(correction.getFieldName())) {
                storageLocation = correction.getAfterValue();
            }
        }
        return new CompletionValues(handoffType, storageLocation);
    }

    private String customerFailureMessage(DeliveryFailure failure) {
        if (failure == null) {
            return null;
        }
        return "배송을 완료하지 못했습니다. 고객센터로 문의해 주세요.";
    }

    private void validateCustomer(UserRole role) {
        if (role != UserRole.CUSTOMER) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDeliveryInfoException();
        }
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(KST).toOffsetDateTime();
    }

    private record RelatedData(
        Map<Long, DeliveryCompletion> completions
        , Map<Long, DeliveryFailure> failures
        , Map<Long, DeliveryDelay> delays
        , Map<Long, DeliveryCompletionPhoto> photoCompletionIds
        , Map<Long, java.util.List<com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection>> corrections
    ) {
        static RelatedData empty() {
            return new RelatedData(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    private record CompletionValues(
        ActualHandoffType handoffType, String storageLocation
    ) {
    }
}
