package com.chapchap.delivery.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.chapchap.delivery.domain.assignment.constant.AssignmentReassignmentReason;
import com.chapchap.delivery.domain.assignment.constant.EmergencyRiderReplacementReason;
import com.chapchap.delivery.domain.assignment.constant.ManualAssignmentReason;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryFailureReason;
import com.chapchap.delivery.domain.delivery.constant.AdminRecoveryReason;
import com.chapchap.delivery.domain.rider.constant.RiderDeliveryActiveReason;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminReasonContractTest {
    @Test
    @DisplayName("일곱 관리자 업무는 문서에서 확정한 전용 사유 코드만 제공한다")
    void reasonEnumsMatchContracts() {
        assertValues(RiderDeliveryActiveReason.values(),
            "INITIAL_ACTIVATION", "TRAINING", "LONG_TERM_LEAVE",
            "OPERATIONAL_HOLD", "RESUME_DELIVERY", "OTHER");
        assertValues(RiderScheduleExceptionReason.values(),
            "ANNUAL_LEAVE", "SICK_LEAVE", "TRAINING", "SUBSTITUTE_WORK", "OTHER");
        assertValues(ManualAssignmentReason.values(),
            "AUTO_ASSIGNMENT_FAILED", "LATE_ORDER", "AREA_EXCEPTION",
            "OPERATIONAL_ADJUSTMENT", "OTHER");
        assertValues(AssignmentReassignmentReason.values(),
            "RIDER_ISSUE", "RIDER_UNAVAILABLE", "ACKNOWLEDGEMENT_OVERDUE",
            "OPERATIONAL_ADJUSTMENT", "OTHER");
        assertValues(EmergencyRiderReplacementReason.values(),
            "RIDER_ACCIDENT", "RIDER_HEALTH_ISSUE", "VEHICLE_ISSUE",
            "URGENT_OPERATIONAL_CHANGE", "OTHER");
        assertValues(AdminDeliveryFailureReason.values(),
            "RIDER_REPORT_CONFIRMED", "CUSTOMER_REPORT_CONFIRMED",
            "OPERATIONAL_REVIEW", "SYSTEM_RECOVERY", "OTHER");
        assertValues(AdminRecoveryReason.values(),
            "DEVICE_FAILURE", "NETWORK_FAILURE", "APP_FAILURE", "SERVER_FAILURE", "OTHER");
    }

    @Test
    @DisplayName("각 업무 사유에서 OTHER만 상세 설명을 필수로 표시한다")
    void onlyOtherRequiresDetail() {
        assertOtherOnly(RiderDeliveryActiveReason.values(), RiderDeliveryActiveReason.OTHER);
        assertOtherOnly(RiderScheduleExceptionReason.values(), RiderScheduleExceptionReason.OTHER);
        assertOtherOnly(ManualAssignmentReason.values(), ManualAssignmentReason.OTHER);
        assertOtherOnly(AssignmentReassignmentReason.values(), AssignmentReassignmentReason.OTHER);
        assertOtherOnly(EmergencyRiderReplacementReason.values(), EmergencyRiderReplacementReason.OTHER);
        assertOtherOnly(AdminDeliveryFailureReason.values(), AdminDeliveryFailureReason.OTHER);
        assertOtherOnly(AdminRecoveryReason.values(), AdminRecoveryReason.OTHER);
    }

    private void assertValues(Enum<?>[] values, String... expected) {
        assertThat(Arrays.stream(values).map(Enum::name)).containsExactly(expected);
    }

    private <E extends Enum<E>> void assertOtherOnly(E[] values, E other) {
        for (E value : values) {
            boolean requiresDetail = switch (value) {
                case RiderDeliveryActiveReason reason -> reason.requiresDetail();
                case RiderScheduleExceptionReason reason -> reason.requiresDetail();
                case ManualAssignmentReason reason -> reason.requiresDetail();
                case AssignmentReassignmentReason reason -> reason.requiresDetail();
                case EmergencyRiderReplacementReason reason -> reason.requiresDetail();
                case AdminDeliveryFailureReason reason -> reason.requiresDetail();
                case AdminRecoveryReason reason -> reason.requiresDetail();
                default -> throw new IllegalStateException("Unexpected reason enum");
            };
            assertThat(requiresDetail).isEqualTo(value == other);
        }
    }
}
