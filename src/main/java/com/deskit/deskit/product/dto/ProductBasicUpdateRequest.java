package com.deskit.deskit.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

public record ProductBasicUpdateRequest(
  @JsonProperty("product_name")
  String productName,

  @JsonProperty("short_desc")
  String shortDesc,

  @JsonProperty("price")
  @Min(0)
  Integer price,

  @JsonProperty("stock_qty")
  @Min(0)
  Integer stockQty
) {}
