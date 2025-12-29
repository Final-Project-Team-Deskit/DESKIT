package com.deskit.deskit.tag.entity;

import com.deskit.deskit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tag")
public class Tag extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "tag_id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_category_id", nullable = false)
  private TagCategory tagCategory;

  @Column(name = "tag_name", nullable = false, length = 50)
  private String tagName;

  protected Tag() {
  }

  public Tag(TagCategory tagCategory, String tagName) {
    this.tagCategory = tagCategory;
    this.tagName = tagName;
  }

  public Long getId() {
    return id;
  }

  public TagCategory getTagCategory() {
    return tagCategory;
  }

  public String getTagName() {
    return tagName;
  }
}
