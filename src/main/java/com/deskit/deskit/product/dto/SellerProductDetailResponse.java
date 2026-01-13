package com.deskit.deskit.product.dto;

import com.deskit.deskit.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerProductDetailResponse(
  @JsonProperty("product_id")
  Long productId,

  @JsonProperty("product_name")
  String productName,

  @JsonProperty("short_desc")
  String shortDesc,

  @JsonProperty("price")
  Integer price,

  @JsonProperty("stock_qty")
  Integer stockQty,

  @JsonProperty("status")
  Product.Status status
) {
  public static SellerProductDetailResponse from(Product product) {
    if (product == null) {
      return new SellerProductDetailResponse(null, null, null, null, null, null);
    }
    return new SellerProductDetailResponse(
      product.getId(),
      product.getProductName(),
      product.getShortDesc(),
      product.getPrice(),
      product.getStockQty(),
      product.getStatus()
    );
  }
}
