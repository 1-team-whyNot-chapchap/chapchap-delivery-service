package com.chapchap.delivery.domain.audit.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.audit.response.AdminAuditHistoryItemResponse;
import com.chapchap.delivery.domain.audit.response.AdminAuditHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuditHistoryQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
        "occurredAt", "occurredAt"
        , "entityType", "entityType"
        , "action", "action"
    );

    private final DeliveryAccessService accessService;
    private final AuditHistoryRepository repository;

    @Transactional(readOnly = true)
    public AdminAuditHistoryListResponse getAuditHistories(
        Long actorId
        , UserRole role
        , String entityType
        , Long entityId
        , Pageable pageable
    ) {
        accessService.validateAdminAccess(actorId, role);
        return AdminAuditHistoryListResponse.from(
            repository.findAllForAdmin(entityType, entityId, normalize(pageable))
                .map(this::toResponse)
        );
    }

    private Pageable normalize(Pageable pageable) {
        List<Sort.Order> orders = new ArrayList<>();
        pageable.getSort().forEach(order -> {
            String property = SORT_PROPERTIES.get(order.getProperty());
            if (property != null) {
                orders.add(new Sort.Order(order.getDirection(), property));
            }
        });
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("occurredAt"));
        }
        orders.add(Sort.Order.asc("id"));
        return PageRequest.of(
            pageable.getPageNumber()
            , Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
            , Sort.by(orders)
        );
    }

    private AdminAuditHistoryItemResponse toResponse(AuditHistory history) {
        return new AdminAuditHistoryItemResponse(
            history.getId(), history.getEntityType(), history.getEntityId(), history.getAction(),
            history.getActorId(), history.getActorType(), history.getReasonCode(),
            history.getReasonDetail(), history.getOccurredAt().atZone(KST).toOffsetDateTime()
        );
    }
}
