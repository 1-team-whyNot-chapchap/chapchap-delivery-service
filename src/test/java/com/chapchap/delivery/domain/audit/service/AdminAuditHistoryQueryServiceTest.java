package com.chapchap.delivery.domain.audit.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditHistoryQueryServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private AuditHistoryRepository repository;
    private AdminAuditHistoryQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditHistoryQueryService(accessService, repository);
    }

    @Test
    @DisplayName("감사 이력은 관리자를 검증하고 id 오름차순을 보조 정렬로 사용한다")
    void validatesAdminAndUsesStableSort() {
        when(repository.findAllForAdmin(eq("DELIVERY"), eq(10L), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.getAuditHistories(
            7L, UserRole.ADMIN, "DELIVERY", 10L,
            PageRequest.of(0, 20, Sort.by(Sort.Order.desc("occurredAt")))
        );

        verify(accessService).validateAdminAccess(7L, UserRole.ADMIN);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllForAdmin(eq("DELIVERY"), eq(10L), pageableCaptor.capture());
        List<Sort.Order> orders = pageableCaptor.getValue().getSort().stream().toList();
        assertThat(orders).extracting(Sort.Order::getProperty)
            .containsExactly("occurredAt", "id");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
