package com.chapchap.delivery.domain.assignment.scheduler;

import com.chapchap.delivery.domain.assignment.service.AutoAssignmentService;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class AutoAssignmentScheduler {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final DeliveryGroupRepository deliveryGroupRepository;
    private final AutoAssignmentService autoAssignmentService;

    public AutoAssignmentScheduler(
        DeliveryGroupRepository deliveryGroupRepository
        , AutoAssignmentService autoAssignmentService
    ) {
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.autoAssignmentService = autoAssignmentService;
    }

    @Scheduled(
        cron = "0 10,20,30,40,50 16 * * *"
        , zone = "Asia/Seoul"
    )
    @Scheduled(
        cron = "0 0 17 * * *"
        , zone = "Asia/Seoul"
    )
    public void runAutoAssignment() {
        LocalDate deliveryDate =
            LocalDate.now(KST)
                .plusDays(1);

        List<Long> deliveryGroupIds =
            deliveryGroupRepository.findAutoAssignmentTargetIds(
                deliveryDate
                , DeliveryGroupStatus.WAITING_ASSIGNMENT
            );

        for (Long deliveryGroupId : deliveryGroupIds) {
            autoAssignmentService.assign(
                deliveryGroupId
            );
        }
    }
}