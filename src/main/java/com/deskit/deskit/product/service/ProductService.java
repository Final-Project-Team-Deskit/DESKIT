package com.deskit.deskit.product.service;

import com.deskit.deskit.product.dto.ProductResponse;
import com.deskit.deskit.product.dto.ProductResponse.ProductTags;
import com.deskit.deskit.product.entity.Product;
import com.deskit.deskit.product.repository.ProductRepository;
import com.deskit.deskit.product.repository.ProductTagRepository;
import com.deskit.deskit.product.repository.ProductTagRepository.ProductTagRow;
import com.deskit.deskit.tag.entity.TagCategory.TagCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service // 스프링 서비스 계층 빈 등록 (비즈니스 로직/조합 담당)
public class ProductService {

  private final ProductRepository productRepository; // Product 조회용 JPA Repository
  private final ProductTagRepository productTagRepository; // Product-Tag 매핑 조회용 JPA Repository

  // 생성자 주입: 테스트/대체 구현에 유리하고, final 필드와 잘 맞음
  public ProductService(ProductRepository productRepository,
                        ProductTagRepository productTagRepository) {
    this.productRepository = productRepository;
    this.productTagRepository = productTagRepository;
  }

  // 상품 목록 조회: deleted_at IS NULL인 상품만 가져오고, 태그는 productIds로 한 번에 batch 조회 (N+1 방지)
  public List<ProductResponse> getProducts() {
    List<Product> products = productRepository.findAllByDeletedAtIsNullOrderByIdAsc();
    if (products.isEmpty()) {
      return Collections.emptyList();
    }

    // 조회된 상품 id 목록 뽑아서 IN 쿼리로 태그를 한 번에 가져오기
    List<Long> productIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toList());

    // (product_id, tagCode, tagName) 형태의 projection row들
    List<ProductTagRow> rows = productTagRepository.findActiveTagsByProductIds(productIds);

    // productId -> (tags, tagsFlat) 형태로 변환
    Map<Long, TagsBundle> tagsByProductId = buildTagsByProductId(rows);

    // 상품 엔티티 + 태그 번들 => 프론트 호환 DTO로 조립
    return products.stream()
            .map(product -> {
              TagsBundle bundle = tagsByProductId.get(product.getId());
              ProductTags tags = bundle == null ? ProductTags.empty() : bundle.getTags();
              List<String> tagsFlat = bundle == null ? Collections.emptyList() : bundle.getTagsFlat();
              return ProductResponse.from(product, tags, tagsFlat);
            })
            .collect(Collectors.toList());
  }

  // 상품 단건 조회: deleted_at IS NULL인 상품만 반환. 없으면 Optional.empty()
  public Optional<ProductResponse> getProduct(Long id) {
    Optional<Product> product = productRepository.findByIdAndDeletedAtIsNull(id);
    if (product.isEmpty()) {
      return Optional.empty();
    }

    // 단건이지만 동일 로직 재사용: IN(List.of(id))로 tags batch 조회
    List<ProductTagRow> rows = productTagRepository.findActiveTagsByProductIds(List.of(id));
    Map<Long, TagsBundle> tagsByProductId = buildTagsByProductId(rows);

    TagsBundle bundle = tagsByProductId.get(id);
    ProductTags tags = bundle == null ? ProductTags.empty() : bundle.getTags();
    List<String> tagsFlat = bundle == null ? Collections.emptyList() : bundle.getTagsFlat();

    return Optional.of(ProductResponse.from(product.get(), tags, tagsFlat));
  }

  // DB에서 가져온 tag row들을 productId별로 묶어서 tags/tagsFlat을 만든다
  // - TagCode별 리스트 유지(space/tone/situation/mood)
  // - 중복 태그 제거(LinkedHashSet) + 순서 안정적으로 유지
  static Map<Long, TagsBundle> buildTagsByProductId(List<ProductTagRow> rows) {
    Map<Long, TagAccumulator> accumulators = new java.util.HashMap<>();

    for (ProductTagRow row : rows) {
      // projection이 깨졌거나 필수 값이 없으면 스킵 (방어코드)
      if (row == null || row.getProductId() == null || row.getTagCode() == null) {
        continue;
      }

      // productId별 누적기 생성/재사용
      TagAccumulator acc = accumulators.computeIfAbsent(row.getProductId(),
              ignored -> new TagAccumulator());

      // 코드별로 tagName 추가(중복 제거)
      acc.add(row.getTagCode(), row.getTagName());
    }

    // 누적기 -> 최종 응답 번들로 변환
    return accumulators.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().toBundle()
            ));
  }

  // Product 1건에 대한 태그 결과 묶음
  // - tags: 카테고리별 리스트
  // - tagsFlat: UI용 단일 리스트(카테고리 순서대로 합침)
  static class TagsBundle {
    private final ProductTags tags;
    private final List<String> tagsFlat;

    TagsBundle(ProductTags tags, List<String> tagsFlat) {
      this.tags = tags;
      this.tagsFlat = tagsFlat;
    }

    ProductTags getTags() {
      return tags;
    }

    List<String> getTagsFlat() {
      return tagsFlat;
    }
  }

  // TagCode별 태그명을 누적하는 내부 헬퍼
  // - EnumMap: enum 키에 최적화된 Map
  // - LinkedHashSet: 중복 제거 + 입력 순서 유지
  private static class TagAccumulator {
    private final Map<TagCode, LinkedHashSet<String>> byCode = new EnumMap<>(TagCode.class);

    void add(TagCode code, String tagName) {
      if (tagName == null || tagName.isBlank()) {
        return;
      }
      byCode.computeIfAbsent(code, ignored -> new LinkedHashSet<>()).add(tagName);
    }

    // 누적된 데이터를 ProductTags + tagsFlat로 변환
    TagsBundle toBundle() {
      // 프론트가 기대하는 키 순서( space -> tone -> situation -> mood )로 정렬
      List<String> space = listFor(TagCode.SPACE);
      List<String> tone = listFor(TagCode.TONE);
      List<String> situation = listFor(TagCode.SITUATION);
      List<String> mood = listFor(TagCode.MOOD);

      ProductTags tags = new ProductTags(space, tone, situation, mood);

      // flat은 카테고리 순서대로 합치되 중복 제거(LinkedHashSet)
      LinkedHashSet<String> flat = new LinkedHashSet<>();
      addAll(flat, space);
      addAll(flat, tone);
      addAll(flat, situation);
      addAll(flat, mood);

      return new TagsBundle(tags, new ArrayList<>(flat));
    }

    private List<String> listFor(TagCode code) {
      LinkedHashSet<String> values = byCode.get(code);
      if (values == null || values.isEmpty()) {
        return Collections.emptyList();
      }
      return new ArrayList<>(values);
    }

    private void addAll(LinkedHashSet<String> target, List<String> source) {
      if (source == null || source.isEmpty()) {
        return;
      }
      for (String value : source) {
        if (value != null && !value.isBlank()) {
          target.add(value);
        }
      }
    }
  }
}