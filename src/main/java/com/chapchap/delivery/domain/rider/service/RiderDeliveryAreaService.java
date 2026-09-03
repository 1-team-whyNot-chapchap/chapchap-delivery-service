package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.entity.RiderDeliveryArea;
import com.chapchap.delivery.domain.rider.repository.RiderDeliveryAreaRepository;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaCreateRequest;
import com.chapchap.delivery.domain.rider.request.RiderDeliveryAreaUpdateRequest;
import com.chapchap.delivery.domain.rider.response.RiderDeliveryAreaResponse;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAreaCodeRepository;
import com.chapchap.delivery.global.exception.business.InvalidRiderDeliveryAreaException;
import com.chapchap.delivery.global.exception.business.RiderDeliveryAreaConflictException;
import com.chapchap.delivery.global.exception.business.RiderDeliveryAreaNotFoundException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RiderDeliveryAreaService {
    private final RiderRepository riderRepository;
    private final RiderDeliveryAreaRepository riderDeliveryAreaRepository;
    private final DeliveryAreaCodeRepository deliveryAreaCodeRepository;
    private final DeliveryAccessService deliveryAccessService;

    public RiderDeliveryAreaService(
        RiderRepository riderRepository
        , RiderDeliveryAreaRepository riderDeliveryAreaRepository
        , DeliveryAreaCodeRepository deliveryAreaCodeRepository
        , DeliveryAccessService deliveryAccessService
    ) {
        this.riderRepository = riderRepository;
        this.riderDeliveryAreaRepository = riderDeliveryAreaRepository;
        this.deliveryAreaCodeRepository = deliveryAreaCodeRepository;
        this.deliveryAccessService = deliveryAccessService;
    }

    @Transactional
    public RiderDeliveryAreaResponse createDeliveryArea(
        Long riderId
        , Long actorId
        , UserRole actorRole
        , RiderDeliveryAreaCreateRequest request
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        validatePeriod(
            request.effectiveFrom()
            , request.effectiveTo()
        );

        Rider rider =
            riderRepository.findByIdAndDeletedAtIsNull(
                    riderId
                )
                .orElseThrow(
                    RiderNotFoundException::new
                );

        deliveryAreaCodeRepository
            .findByAreaCodeAndIsActiveTrue(
                request.deliveryAreaCode()
            )
            .orElseThrow(
                InvalidRiderDeliveryAreaException::new
            );

        Optional<RiderDeliveryArea> existingArea =
            riderDeliveryAreaRepository
                .findByRiderIdAndDeliveryAreaCodeAndEffectiveFrom(
                    riderId
                    , request.deliveryAreaCode()
                    , request.effectiveFrom()
                );

        if (existingArea.isPresent()) {
            return handleExistingArea(
                existingArea.get()
                , request
            );
        }

        RiderDeliveryArea riderDeliveryArea =
            new RiderDeliveryArea(
                rider
                , request.deliveryAreaCode()
                , request.effectiveFrom()
                , request.effectiveTo()
                , request.isActive()
            );

        RiderDeliveryArea savedArea =
            riderDeliveryAreaRepository.save(
                riderDeliveryArea
            );

        return RiderDeliveryAreaResponse.from(
            savedArea
        );
    }

    @Transactional(readOnly = true)
    public List<RiderDeliveryAreaResponse> getDeliveryAreas(
        Long riderId
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

        return riderDeliveryAreaRepository
            .findAllByRiderIdAndDeletedAtIsNull(
                riderId
            )
            .stream()
            .map(RiderDeliveryAreaResponse::from)
            .toList();
    }

    @Transactional
    public RiderDeliveryAreaResponse updateDeliveryArea(
        Long riderId
        , Long riderAreaId
        , Long actorId
        , UserRole actorRole
        , RiderDeliveryAreaUpdateRequest request
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

        RiderDeliveryArea riderDeliveryArea =
            riderDeliveryAreaRepository
                .findByIdAndRiderIdAndDeletedAtIsNull(
                    riderAreaId
                    , riderId
                )
                .orElseThrow(
                    RiderDeliveryAreaNotFoundException::new
                );

        validatePeriod(
            riderDeliveryArea.getEffectiveFrom()
            , request.effectiveTo()
        );

        if (hasSameContent(
            riderDeliveryArea
            , request
        )) {
            return RiderDeliveryAreaResponse.from(
                riderDeliveryArea
            );
        }

        riderDeliveryArea.change(
            request.effectiveTo()
            , request.isActive()
        );

        return RiderDeliveryAreaResponse.from(
            riderDeliveryArea
        );
    }

    private RiderDeliveryAreaResponse handleExistingArea(
        RiderDeliveryArea riderDeliveryArea
        , RiderDeliveryAreaCreateRequest request
    ) {
        if (riderDeliveryArea.getDeletedAt() != null) {
            riderDeliveryArea.restore(
                request.effectiveTo()
                , request.isActive()
            );

            return RiderDeliveryAreaResponse.from(
                riderDeliveryArea
            );
        }

        if (hasSameContent(
            riderDeliveryArea
            , request
        )) {
            return RiderDeliveryAreaResponse.from(
                riderDeliveryArea
            );
        }

        throw new RiderDeliveryAreaConflictException();
    }

    private boolean hasSameContent(
        RiderDeliveryArea riderDeliveryArea
        , RiderDeliveryAreaCreateRequest request
    ) {
        return Objects.equals(
            riderDeliveryArea.getDeliveryAreaCode()
            , request.deliveryAreaCode()
        )
            && Objects.equals(
            riderDeliveryArea.getEffectiveFrom()
            , request.effectiveFrom()
        )
            && Objects.equals(
            riderDeliveryArea.getEffectiveTo()
            , request.effectiveTo()
        )
            && Objects.equals(
            riderDeliveryArea.getIsActive()
            , request.isActive()
        );
    }

    private boolean hasSameContent(
        RiderDeliveryArea riderDeliveryArea
        , RiderDeliveryAreaUpdateRequest request
    ) {
        return Objects.equals(
            riderDeliveryArea.getEffectiveTo()
            , request.effectiveTo()
        )
            && Objects.equals(
            riderDeliveryArea.getIsActive()
            , request.isActive()
        );
    }

    private void validatePeriod(
        LocalDate effectiveFrom
        , LocalDate effectiveTo
    ) {
        if (
            effectiveTo != null
                && effectiveTo.isBefore(effectiveFrom)
        ) {
            throw new InvalidRiderDeliveryAreaException();
        }
    }
}