package com.deskit.deskit.tag.entity;

import com.deskit.deskit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tag_category")
public class TagCategory extends BaseEntity {

  public enum TagCode {
    SPACE,
    TONE,
    SITUATION,
    MOOD
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tag_category_id", nullable = false)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "tag_code", nullable = false)
  private TagCode tagCode;

  @Column(name = "tag_category_name", nullable = false, length = 30)
  private String tagCategoryName;

  protected TagCategory() {
  }

  public TagCategory(TagCode tagCode, String tagCategoryName) {
    this.tagCode = tagCode;
    this.tagCategoryName = tagCategoryName;
  }

  public Long getId() {
    return id;
  }

  public TagCode getTagCode() {
    return tagCode;
  }

  public String getTagCategoryName() {
    return tagCategoryName;
  }
}
