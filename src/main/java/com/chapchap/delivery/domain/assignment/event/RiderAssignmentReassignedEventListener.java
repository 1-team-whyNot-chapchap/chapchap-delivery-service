package com.chapchap.delivery.domain.assignment.event;

import com.chapchap.delivery.domain.assignment.service.RiderReassignmentNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RiderAssignmentReassignedEventListener {

    private final RiderReassignmentNotificationService riderReassignmentNotificationService;

    public RiderAssignmentReassignedEventListener(
        RiderReassignmentNotificationService riderReassignmentNotificationService
    ) {
        this.riderReassignmentNotificationService = riderReassignmentNotificationService;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
        RiderAssignmentReassignedEvent event
    ) {
        riderReassignmentNotificationService.publish(
            event.assignmentId()
        );
    }
}