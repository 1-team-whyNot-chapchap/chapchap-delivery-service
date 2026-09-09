package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.repository.RiderScheduleExceptionRepository;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderScheduleExceptionUpdateRequest;
import com.chapchap.delivery.domain.rider.response.RiderScheduleExceptionResponse;
import com.chapchap.delivery.global.exception.business.OptimisticLockConflictException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderScheduleExceptionConflictException;
import com.chapchap.delivery.global.exception.business.RiderScheduleExceptionNotFoundException;
import com.chapchap.delivery.global.exception.technical.DeliverySlotConfigurationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RiderScheduleExceptionService {
    private final RiderRepository riderRepository;
    private final RiderScheduleExceptionRepository riderScheduleExceptionRepository;
    private final DeliverySlotRepository deliverySlotRepository;
    private final DeliveryAccessService deliveryAccessService;

    public RiderScheduleExceptionService(
        RiderRepository riderRepository
        , RiderScheduleExceptionRepository riderScheduleExceptionRepository
        , DeliverySlotRepository deliverySlotRepository
        , DeliveryAccessService deliveryAccessService
    ) {
        this.riderRepository = riderRepository;
        this.riderScheduleExceptionRepository = riderScheduleExceptionRepository;
        this.deliverySlotRepository = deliverySlotRepository;
        this.deliveryAccessService = deliveryAccessService;
    }

    @Transactional
    public RiderScheduleExceptionResponse createScheduleException(
        Long riderId
        , Long actorId
        , UserRole actorRole
        , RiderScheduleExceptionCreateRequest request
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        validateReason(
            request.reasonCode()
            , request.reasonDetail()
        );

        Rider rider =
            riderRepository
                .findByIdAndDeletedAtIsNull(
                    riderId
                )
                .orElseThrow(
                    RiderNotFoundException::new
                );

        DeliverySlot deliverySlot =
            deliverySlotRepository
                .findByCodeAndDeletedAtIsNull(
                    request.deliverySlot()
                )
                .orElseThrow(
                    () -> new DeliverySlotConfigurationException(
                        request.deliverySlot()
                    )
                );

        Optional<RiderScheduleException> existingException =
            riderScheduleExceptionRepository
                .findByRiderIdAndScheduleDateAndSlotId(
                    riderId
                    , request.scheduleDate()
                    , deliverySlot.getId()
                );

        if (existingException.isPresent()) {
            return handleExistingException(
                existingException.get()
                , request
            );
        }

        RiderScheduleException scheduleException =
            new RiderScheduleException(
                rider
                , request.scheduleDate()
                , deliverySlot
                , request.isWorking()
                , request.reasonCode()
                , request.reasonDetail()
                , actorId
            );

        RiderScheduleException savedException =
            riderScheduleExceptionRepository.save(
                scheduleException
            );

        return RiderScheduleExceptionResponse.from(
            savedException
        );
    }

    @Transactional(readOnly = true)
    public List<RiderScheduleExceptionResponse> getScheduleExceptions(
        Long riderId
        , Long actorId
        , UserRole actorRole
        , LocalDate dateFrom
        , LocalDate dateTo
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        riderRepository
            .findByIdAndDeletedAtIsNull(
                riderId
            )
            .orElseThrow(
                RiderNotFoundException::new
            );

        return riderScheduleExceptionRepository
            .findAllByRiderIdAndScheduleDateBetweenAndDeletedAtIsNullOrderByScheduleDateAsc(
                riderId
                , dateFrom
                , dateTo
            )
            .stream()
            .map(
                RiderScheduleExceptionResponse::from
            )
            .toList();
    }

    @Transactional
    public RiderScheduleExceptionResponse updateScheduleException(
        Long riderId
        , Long exceptionId
        , Long actorId
        , UserRole actorRole
        , RiderScheduleExceptionUpdateRequest request
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        validateReason(
            request.reasonCode()
            , request.reasonDetail()
        );

        riderRepository
            .findByIdAndDeletedAtIsNull(
                riderId
            )
            .orElseThrow(
                RiderNotFoundException::new
            );

        RiderScheduleException scheduleException =
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    exceptionId
                    , riderId
                )
                .orElseThrow(
                    RiderScheduleExceptionNotFoundException::new
                );

        validateVersion(
            scheduleException
            , request.version()
        );

        if (
            hasSameContent(
                scheduleException
                , request
            )
        ) {
            return RiderScheduleExceptionResponse.from(
                scheduleException
            );
        }

        scheduleException.change(
            request.isWorking()
            , request.reasonCode()
            , request.reasonDetail()
        );

        riderScheduleExceptionRepository.flush();

        return RiderScheduleExceptionResponse.from(
            scheduleException
        );
    }

    @Transactional
    public void deleteScheduleException(
        Long riderId
        , Long exceptionId
        , Long actorId
        , UserRole actorRole
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        riderRepository
            .findByIdAndDeletedAtIsNull(
                riderId
            )
            .orElseThrow(
                RiderNotFoundException::new
            );

        RiderScheduleException scheduleException =
            riderScheduleExceptionRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    exceptionId
                    , riderId
                )
                .orElseThrow(
                    RiderScheduleExceptionNotFoundException::new
                );

        scheduleException.delete(
            LocalDateTime.now(
                ZoneId.of("Asia/Seoul")
            )
        );
    }

    private RiderScheduleExceptionResponse handleExistingException(
        RiderScheduleException scheduleException
        , RiderScheduleExceptionCreateRequest request
    ) {
        if (scheduleException.getDeletedAt() != null) {
            scheduleException.restore(
                request.isWorking()
                , request.reasonCode()
                , request.reasonDetail()
            );

            return RiderScheduleExceptionResponse.from(
                scheduleException
            );
        }

        if (
            hasSameContent(
                scheduleException
                , request
            )
        ) {
            return RiderScheduleExceptionResponse.from(
                scheduleException
            );
        }

        throw new RiderScheduleExceptionConflictException();
    }

    private boolean hasSameContent(
        RiderScheduleException scheduleException
        , RiderScheduleExceptionCreateRequest request
    ) {
        return Objects.equals(
            scheduleException.getIsWorking()
            , request.isWorking()
        )
            && scheduleException.getReasonCode()
            == request.reasonCode()
            && Objects.equals(
            scheduleException.getReasonDetail()
            , request.reasonDetail()
        );
    }

    private boolean hasSameContent(
        RiderScheduleException scheduleException
        , RiderScheduleExceptionUpdateRequest request
    ) {
        return Objects.equals(
            scheduleException.getIsWorking()
            , request.isWorking()
        )
            && scheduleException.getReasonCode()
            == request.reasonCode()
            && Objects.equals(
            scheduleException.getReasonDetail()
            , request.reasonDetail()
        );
    }

    private void validateReason(
        RiderScheduleExceptionReason reasonCode
        , String reasonDetail
    ) {
        if (
            reasonCode
                == RiderScheduleExceptionReason.OTHER
                && (
                reasonDetail == null
                    || reasonDetail.isBlank()
            )
        ) {
            throw new OtherReasonDetailRequiredException();
        }
    }

    private void validateVersion(
        RiderScheduleException scheduleException
        , Long requestVersion
    ) {
        if (
            !Objects.equals(
                scheduleException.getVersion()
                , requestVersion
            )
        ) {
            throw new OptimisticLockConflictException();
        }
    }
}