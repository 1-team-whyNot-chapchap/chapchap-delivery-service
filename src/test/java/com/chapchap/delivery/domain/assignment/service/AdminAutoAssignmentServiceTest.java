package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAutoAssignmentServiceTest {
    @Mock
    private DeliveryAccessService deliveryAccessService;

    @Mock
    private AutoAssignmentService autoAssignmentService;

    @InjectMocks
    private AdminAutoAssignmentService adminAutoAssignmentService;

    @Test
    void adminCanRunAutoAssignment() {
        when(autoAssignmentService.assign(100L)).thenReturn(true);

        boolean assigned = adminAutoAssignmentService.assign(
            1L
            , UserRole.ADMIN
            , 100L
        );

        assertThat(assigned).isTrue();
        verify(deliveryAccessService).validateAdminAccess(1L, UserRole.ADMIN);
        verify(autoAssignmentService).assign(100L);
    }
}
