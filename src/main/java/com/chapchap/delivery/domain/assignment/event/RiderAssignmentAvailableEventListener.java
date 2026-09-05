package com.chapchap.delivery.domain.assignment.event;

import com.chapchap.delivery.domain.assignment.service.RiderAssignmentNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RiderAssignmentAvailableEventListener {
    private final RiderAssignmentNotificationService riderAssignmentNotificationService;

    public RiderAssignmentAvailableEventListener(
        RiderAssignmentNotificationService riderAssignmentNotificationService
    ) {
        this.riderAssignmentNotificationService = riderAssignmentNotificationService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(RiderAssignmentAvailableEvent event) {
        riderAssignmentNotificationService.publish(
            event.assignmentId()
        );
    }
}