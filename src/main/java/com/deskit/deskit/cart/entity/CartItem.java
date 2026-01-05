package com.deskit.deskit.cart.entity;

import com.deskit.deskit.common.entity.BaseEntity;
import com.deskit.deskit.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_item", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cart_item", columnNames = {"cart_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cart_item_id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "price_snapshot", nullable = false)
  private Integer priceSnapshot;

  public CartItem(Cart cart, Product product, Integer quantity, Integer priceSnapshot) {
    this.cart = cart;
    this.product = product;
    this.quantity = quantity;
    this.priceSnapshot = priceSnapshot;
  }
}
