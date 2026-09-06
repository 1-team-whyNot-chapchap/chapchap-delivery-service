package com.chapchap.delivery.global.kafka.constant;

public final class DeliveryOperationNotificationBusinessKey {
    private static final String RIDER_ASSIGNMENT_AVAILABLE =
        "RIDER_ASSIGNMENT_AVAILABLE";

    private static final String RIDER_ACK_OPENED =
        "RIDER_ACK_OPENED";

    private static final String RIDER_ACK_REMINDER =
        "RIDER_ACK_REMINDER";

    private static final String RIDER_REASSIGNED =
        "RIDER_REASSIGNED";

    private DeliveryOperationNotificationBusinessKey() {
    }

    public static String riderAssignmentAvailable(
        Long assignmentId
    ) {
        return RIDER_ASSIGNMENT_AVAILABLE
            + ":"
            + assignmentId;
    }

    public static String riderAcknowledgementOpened(
        Long assignmentId
    ) {
        return RIDER_ACK_OPENED
            + ":"
            + assignmentId;
    }

    public static String riderAcknowledgementReminder(
        Long assignmentId
        , String reminderStage
    ) {
        return RIDER_ACK_REMINDER
            + ":"
            + assignmentId
            + ":"
            + reminderStage;
    }

    public static String riderReassigned(
        Long assignmentId
    ) {
        return RIDER_REASSIGNED
            + ":"
            + assignmentId;
    }

    public static String adminLateOrder(String deliveryId) {
        return "ADMIN_LATE_ORDER_REVIEW:" + deliveryId;
    }

    public static String adminAssignmentAction(String referenceType, Long referenceId, String reason) {
        return "ADMIN_ASSIGNMENT_ACTION_REQUIRED:" + referenceType + ":" + referenceId + ":" + reason;
    }

    public static String adminUnresolvedDelivery(String deliveryId) {
        return "ADMIN_UNRESOLVED_DELIVERY:" + deliveryId;
    }

    public static String adminEventPublishFailed(Long recordId) {
        return "ADMIN_EVENT_PUBLISH_FAILED:" + recordId;
    }
}
