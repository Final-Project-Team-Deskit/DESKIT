package com.deskit.deskit.order.repository;

import com.deskit.deskit.order.entity.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Order(주문) 엔티티에 대한 조회/저장 Repository.
 *
 * - JpaRepository가 기본 CRUD를 제공한다. (save, findById, delete 등)
 * - 아래 커스텀 메서드는 Spring Data JPA 메서드 네이밍 규칙으로 쿼리를 자동 생성한다.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

  /**
   * 특정 회원(member_id)의 주문 목록을 생성일(created_at) 기준 내림차순으로 조회한다.
   *
   * - findByMemberId...:
   *   Order 엔티티의 memberId 컬럼을 조건으로 필터링한다.
   *
   * - OrderByCreatedAtDesc:
   *   BaseEntity에 있는 createdAt(created_at) 컬럼 기준으로 최신 주문이 먼저 오도록 정렬한다.
   *
   * - 사용 예:
   *   List<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
   */
  List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}