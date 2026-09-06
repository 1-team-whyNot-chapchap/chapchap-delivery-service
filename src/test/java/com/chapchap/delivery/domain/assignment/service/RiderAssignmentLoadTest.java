package com.chapchap.delivery.domain.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiderAssignmentLoadTest {

    @Test
    void recommendedCapacityUsesEightStopsAndThirtySixLunchboxes() {
        RiderAssignmentLoad load = new RiderAssignmentLoad(7, 35);

        assertThat(load.canAssignWithinRecommended(1)).isTrue();
        assertThat(load.canAssignWithinRecommended(2)).isFalse();
        assertThat(load.canAssign(2)).isTrue();
    }

    @Test
    void maximumCapacityRejectsEleventhStopOrMoreThanFortyTwoLunchboxes() {
        RiderAssignmentLoad stopLimit = new RiderAssignmentLoad(10, 20);
        RiderAssignmentLoad quantityLimit = new RiderAssignmentLoad(5, 42);

        assertThat(stopLimit.canAssign(1)).isFalse();
        assertThat(quantityLimit.canAssign(1)).isFalse();
    }
}
