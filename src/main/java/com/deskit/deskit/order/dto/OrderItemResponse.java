package com.deskit.deskit.order.dto;

import com.deskit.deskit.order.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItemResponse(
  @JsonProperty("product_id")
  Long productId,

  @JsonProperty("quantity")
  Integer quantity,

  @JsonProperty("unit_price")
  Integer unitPrice,

  @JsonProperty("subtotal_price")
  Integer subtotalPrice
) {
  public static OrderItemResponse from(OrderItem item) {
    if (item == null) {
      return new OrderItemResponse(null, null, null, null);
    }
    return new OrderItemResponse(
      item.getProductId(),
      item.getQuantity(),
      item.getUnitPrice(),
      item.getSubtotalPrice()
    );
  }
}
