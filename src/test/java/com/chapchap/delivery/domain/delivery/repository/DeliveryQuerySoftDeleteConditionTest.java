package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryQuerySoftDeleteConditionTest {

    @Test
    @DisplayName("고객 목록·상세 쿼리는 삭제된 전체 배송을 제외한다")
    void customerQueriesExcludeDeletedDeliveryGroups() {
        assertQueryContains(DeliveryRepository.class, "findAllForCustomer", "g.deletedAt IS NULL");
        assertQueryContains(DeliveryRepository.class, "findDetailByDeliveryPublicId", "g.deletedAt IS NULL");
    }

    @Test
    @DisplayName("기사 목록·상세 쿼리는 삭제된 전체 배송을 제외한다")
    void riderQueriesExcludeDeletedDeliveryGroups() {
        assertQueryContains(DeliveryAssignmentRepository.class, "findAllMine", "dg.deletedAt IS NULL");
        assertQueryContains(DeliveryAssignmentRepository.class, "findMineById", "deliveryGroup.deletedAt IS NULL");
        assertQueryContains(
            DeliveryAssignmentItemRepository.class,
            "findAllByAssignmentIdWithDelivery",
            "deliveryGroup.deletedAt IS NULL"
        );
    }

    @Test
    @DisplayName("관리자 배정 이슈 쿼리는 삭제된 이슈와 배정을 제외한다")
    void adminIssueQueryExcludesDeletedData() {
        assertQueryContains(
            DeliveryAssignmentIssueRepository.class,
            "findAllByDeliveryGroupIdIn",
            "dai.deletedAt IS NULL"
        );
        assertQueryContains(
            DeliveryAssignmentIssueRepository.class,
            "findAllByDeliveryGroupIdIn",
            "da.deletedAt IS NULL"
        );
    }

    private void assertQueryContains(
        Class<?> repositoryType
        , String methodName
        , String expectedCondition
    ) {
        Method method = Arrays.stream(repositoryType.getMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
        Query query = method.getAnnotation(Query.class);
        assertThat(query).isNotNull();
        assertThat(query.value()).contains(expectedCondition);
    }
}
