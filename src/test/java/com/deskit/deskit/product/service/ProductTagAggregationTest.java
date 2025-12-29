package com.deskit.deskit.product.service;

import com.deskit.deskit.product.repository.ProductTagRepository.ProductTagRow;
import com.deskit.deskit.tag.entity.TagCategory.TagCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductTagAggregationTest {

  @Test
  void buildTagsByProductId_buildsOrderedTagsAndFlatList() {
    List<ProductTagRow> rows = List.of(
        new Row(1L, TagCode.SPACE, "오피스"),
        new Row(1L, TagCode.SPACE, "서재"),
        new Row(1L, TagCode.TONE, "모던"),
        new Row(1L, TagCode.TONE, "모던"),
        new Row(1L, TagCode.SITUATION, "재택근무"),
        new Row(1L, TagCode.MOOD, "깔끔한"),
        new Row(2L, TagCode.MOOD, "포근한")
    );

    Map<Long, ProductService.TagsBundle> result = ProductService.buildTagsByProductId(rows);
    ProductService.TagsBundle bundle = result.get(1L);

    assertNotNull(bundle);
    assertEquals(List.of("오피스", "서재"), bundle.getTags().getSpace());
    assertEquals(List.of("모던"), bundle.getTags().getTone());
    assertEquals(List.of("재택근무"), bundle.getTags().getSituation());
    assertEquals(List.of("깔끔한"), bundle.getTags().getMood());
    assertEquals(List.of("오피스", "서재", "모던", "재택근무", "깔끔한"),
        bundle.getTagsFlat());
  }

  private static class Row implements ProductTagRow {
    private final Long productId;
    private final TagCode tagCode;
    private final String tagName;

    Row(Long productId, TagCode tagCode, String tagName) {
      this.productId = productId;
      this.tagCode = tagCode;
      this.tagName = tagName;
    }

    @Override
    public Long getProductId() {
      return productId;
    }

    @Override
    public TagCode getTagCode() {
      return tagCode;
    }

    @Override
    public String getTagName() {
      return tagName;
    }
  }
}
