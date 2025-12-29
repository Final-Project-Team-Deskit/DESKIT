package com.deskit.deskit.setup.entity;

import com.deskit.deskit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "setup")
public class Setup extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "setup_id", nullable = false)
  private Long id;

  @Column(name = "seller_id", nullable = false)
  private Long sellerId;

  @Column(name = "setup_name", nullable = false, length = 100)
  private String setupName;

  @Column(name = "short_desc", nullable = false, length = 250)
  private String shortDesc;

  @Column(name = "tip_text", length = 500)
  private String tipText;

  @Column(name = "setup_image_url", nullable = false, length = 500)
  private String setupImageUrl;

  protected Setup() {
  }

  public Setup(Long sellerId, String setupName, String shortDesc, String tipText,
               String setupImageUrl) {
    this.sellerId = sellerId;
    this.setupName = setupName;
    this.shortDesc = shortDesc;
    this.tipText = tipText;
    this.setupImageUrl = setupImageUrl;
  }

  public Long getId() {
    return id;
  }

  public Long getSellerId() {
    return sellerId;
  }

  public String getSetupName() {
    return setupName;
  }

  public String getShortDesc() {
    return shortDesc;
  }

  public String getTipText() {
    return tipText;
  }

  public String getSetupImageUrl() {
    return setupImageUrl;
  }
}
