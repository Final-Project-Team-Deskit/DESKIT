package com.deskit.deskit.product.repository;

import com.deskit.deskit.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Product 엔티티에 대한 DB 접근 레이어(Repository)
 * - Spring Data JPA가 구현체를 자동 생성해줌
 * - 기본 CRUD(save/findById/findAll/delete 등)는 JpaRepository가 제공
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

  /**
   * deleted_at 이 NULL(= 논리삭제 안 된 데이터)인 상품만 조회
   * 그리고 id 오름차순으로 정렬해서 리스트로 반환
   *
   * 메서드 이름 규칙(쿼리 메서드)로 SQL/JPQL을 자동 생성:
   * - findAllByDeletedAtIsNull : deletedAt 컬럼이 null인 것만
   * - OrderByIdAsc : id 기준 오름차순 정렬
   */
  List<Product> findAllByDeletedAtIsNullOrderByIdAsc();

  /**
   * 특정 id의 상품을 조회하되, deleted_at 이 NULL인 경우만 조회
   * 결과가 없을 수 있으니 Optional로 감쌈
   *
   * 예: id는 존재하지만 deleted_at이 채워져 있으면(논리삭제) -> Optional.empty()
   */
  Optional<Product> findByIdAndDeletedAtIsNull(Long id);
}