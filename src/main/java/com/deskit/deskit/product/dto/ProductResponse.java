package com.deskit.deskit.product.dto;

import com.deskit.deskit.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.List;

public class ProductResponse {

  @JsonProperty("product_id")
  private final Long productId;

  @JsonProperty("seller_id")
  private final Long sellerId;

  @JsonProperty("name")
  private final String name;

  @JsonProperty("short_desc")
  private final String shortDesc;

  @JsonProperty("detail_html")
  private final String detailHtml;

  @JsonProperty("price")
  private final Integer price;

  @JsonProperty("cost_price")
  private final Integer costPrice;

  @JsonProperty("status")
  private final Product.Status status;

  @JsonProperty("stock_qty")
  private final Integer stockQty;

  @JsonProperty("safety_stock")
  private final Integer safetyStock;

  @JsonProperty("tags")
  private final ProductTags tags;

  @JsonProperty("tagsFlat")
  private final List<String> tagsFlat;

  public ProductResponse(Long productId, Long sellerId, String name, String shortDesc,
                         String detailHtml, Integer price, Integer costPrice,
                         Product.Status status, Integer stockQty, Integer safetyStock,
                         ProductTags tags, List<String> tagsFlat) {
    this.productId = productId;
    this.sellerId = sellerId;
    this.name = name;
    this.shortDesc = shortDesc;
    this.detailHtml = detailHtml;
    this.price = price;
    this.costPrice = costPrice;
    this.status = status;
    this.stockQty = stockQty;
    this.safetyStock = safetyStock;
    this.tags = tags == null ? ProductTags.empty() : tags;
    this.tagsFlat = tagsFlat == null ? Collections.emptyList() : tagsFlat;
  }

  public static ProductResponse from(Product product, ProductTags tags, List<String> tagsFlat) {
    return new ProductResponse(
        product.getId(),
        product.getSellerId(),
        product.getProductName(),
        product.getShortDesc(),
        product.getDetailHtml(),
        product.getPrice(),
        product.getCostPrice(),
        product.getStatus(),
        product.getStockQty(),
        product.getSafetyStock(),
        tags,
        tagsFlat
    );
  }

  @JsonPropertyOrder({"space", "tone", "situation", "mood"})
  public static class ProductTags {

    @JsonProperty("space")
    private final List<String> space;

    @JsonProperty("tone")
    private final List<String> tone;

    @JsonProperty("situation")
    private final List<String> situation;

    @JsonProperty("mood")
    private final List<String> mood;

    public ProductTags(List<String> space, List<String> tone,
                       List<String> situation, List<String> mood) {
      this.space = space == null ? Collections.emptyList() : space;
      this.tone = tone == null ? Collections.emptyList() : tone;
      this.situation = situation == null ? Collections.emptyList() : situation;
      this.mood = mood == null ? Collections.emptyList() : mood;
    }

    public static ProductTags empty() {
      return new ProductTags(Collections.emptyList(), Collections.emptyList(),
          Collections.emptyList(), Collections.emptyList());
    }

    public List<String> getSpace() {
      return space;
    }

    public List<String> getTone() {
      return tone;
    }

    public List<String> getSituation() {
      return situation;
    }

    public List<String> getMood() {
      return mood;
    }
  }
}
