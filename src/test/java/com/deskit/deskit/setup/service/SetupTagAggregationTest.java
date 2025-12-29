package com.deskit.deskit.setup.service;

import com.deskit.deskit.setup.repository.SetupTagRepository.SetupTagRow;
import com.deskit.deskit.tag.entity.TagCategory.TagCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SetupTagAggregationTest {

  @Test
  void buildTagsBySetupId_buildsOrderedTagsAndFlatList() {
    List<SetupTagRow> rows = List.of(
        new Row(10L, TagCode.SPACE, "거실"),
        new Row(10L, TagCode.SPACE, "거실"),
        new Row(10L, TagCode.TONE, "미니멀"),
        new Row(10L, TagCode.SITUATION, "재택근무"),
        new Row(10L, TagCode.MOOD, "편안한"),
        new Row(11L, TagCode.MOOD, "집중")
    );

    Map<Long, SetupService.TagsBundle> result = SetupService.buildTagsBySetupId(rows);
    SetupService.TagsBundle bundle = result.get(10L);

    assertNotNull(bundle);
    assertEquals(List.of("거실"), bundle.getTags().getSpace());
    assertEquals(List.of("미니멀"), bundle.getTags().getTone());
    assertEquals(List.of("재택근무"), bundle.getTags().getSituation());
    assertEquals(List.of("편안한"), bundle.getTags().getMood());
    assertEquals(List.of("거실", "미니멀", "재택근무", "편안한"),
        bundle.getTagsFlat());
  }

  private static class Row implements SetupTagRow {
    private final Long setupId;
    private final TagCode tagCode;
    private final String tagName;

    Row(Long setupId, TagCode tagCode, String tagName) {
      this.setupId = setupId;
      this.tagCode = tagCode;
      this.tagName = tagName;
    }

    @Override
    public Long getSetupId() {
      return setupId;
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
