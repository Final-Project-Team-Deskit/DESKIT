package com.deskit.deskit.product.entity;

import com.deskit.deskit.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

  public enum Status {
    DRAFT,
    READY,
    ON_SALE,
    LIMITED_SALE,
    SOLD_OUT,
    PAUSED,
    HIDDEN,
    DELETED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_id", nullable = false)
  private Long id;

  @Column(name = "seller_id", nullable = false)
  private Long sellerId;

  @Column(name = "product_name", nullable = false, length = 100)
  private String productName;

  @Column(name = "short_desc", nullable = false, length = 250)
  private String shortDesc;

  @Lob
  @Column(name = "detail_html", nullable = false)
  private String detailHtml;

  @Column(name = "price", nullable = false)
  private Integer price;

  @Column(name = "cost_price", nullable = false)
  private Integer costPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "stock_qty", nullable = false)
  private Integer stockQty;

  @Column(name = "safety_stock", nullable = false)
  private Integer safetyStock;

  public Product(Long sellerId, String productName, String shortDesc, String detailHtml,
                 Integer price, Integer costPrice, Status status, Integer stockQty,
                 Integer safetyStock) {
    this.sellerId = sellerId;
    this.productName = productName;
    this.shortDesc = shortDesc;
    this.detailHtml = detailHtml;
    this.price = price;
    this.costPrice = costPrice;
    this.status = status;
    this.stockQty = stockQty;
    this.safetyStock = safetyStock;
  }

  public void decreaseStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be > 0");
    }
    if (this.stockQty == null) {
      this.stockQty = 0;
    }
    if (this.stockQty < quantity) {
      throw new IllegalStateException("insufficient stock");
    }
    this.stockQty -= quantity;
  }
}
