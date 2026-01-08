package com.deskit.deskit.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
  @JsonProperty("items")
  @NotNull
  @Size(min = 1)
  List<CreateOrderItemRequest> items
) {}
