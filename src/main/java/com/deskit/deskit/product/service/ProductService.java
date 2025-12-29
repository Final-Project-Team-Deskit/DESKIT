package com.deskit.deskit.product.service;

import com.deskit.deskit.product.dto.ProductResponse;
import com.deskit.deskit.product.dto.ProductResponse.ProductTags;
import com.deskit.deskit.product.entity.Product;
import com.deskit.deskit.product.repository.ProductRepository;
import com.deskit.deskit.product.repository.ProductTagRepository;
import com.deskit.deskit.product.repository.ProductTagRepository.ProductTagRow;
import com.deskit.deskit.tag.entity.TagCategory.TagCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductTagRepository productTagRepository;

  public ProductService(ProductRepository productRepository,
                        ProductTagRepository productTagRepository) {
    this.productRepository = productRepository;
    this.productTagRepository = productTagRepository;
  }

  public List<ProductResponse> getProducts() {
    List<Product> products = productRepository.findAllByDeletedAtIsNullOrderByIdAsc();
    if (products.isEmpty()) {
      return Collections.emptyList();
    }
    List<Long> productIds = products.stream()
        .map(Product::getId)
        .collect(Collectors.toList());
    List<ProductTagRow> rows = productTagRepository.findActiveTagsByProductIds(productIds);
    Map<Long, TagsBundle> tagsByProductId = buildTagsByProductId(rows);
    return products.stream()
        .map(product -> {
          TagsBundle bundle = tagsByProductId.get(product.getId());
          ProductTags tags = bundle == null ? ProductTags.empty() : bundle.getTags();
          List<String> tagsFlat = bundle == null ? Collections.emptyList() : bundle.getTagsFlat();
          return ProductResponse.from(product, tags, tagsFlat);
        })
        .collect(Collectors.toList());
  }

  public Optional<ProductResponse> getProduct(Long id) {
    Optional<Product> product = productRepository.findByIdAndDeletedAtIsNull(id);
    if (product.isEmpty()) {
      return Optional.empty();
    }
    List<ProductTagRow> rows = productTagRepository.findActiveTagsByProductIds(List.of(id));
    Map<Long, TagsBundle> tagsByProductId = buildTagsByProductId(rows);
    TagsBundle bundle = tagsByProductId.get(id);
    ProductTags tags = bundle == null ? ProductTags.empty() : bundle.getTags();
    List<String> tagsFlat = bundle == null ? Collections.emptyList() : bundle.getTagsFlat();
    return Optional.of(ProductResponse.from(product.get(), tags, tagsFlat));
  }

  static Map<Long, TagsBundle> buildTagsByProductId(List<ProductTagRow> rows) {
    Map<Long, TagAccumulator> accumulators = new java.util.HashMap<>();
    for (ProductTagRow row : rows) {
      if (row == null || row.getProductId() == null || row.getTagCode() == null) {
        continue;
      }
      TagAccumulator acc = accumulators.computeIfAbsent(row.getProductId(),
          ignored -> new TagAccumulator());
      acc.add(row.getTagCode(), row.getTagName());
    }
    return accumulators.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().toBundle()
        ));
  }

  static class TagsBundle {
    private final ProductTags tags;
    private final List<String> tagsFlat;

    TagsBundle(ProductTags tags, List<String> tagsFlat) {
      this.tags = tags;
      this.tagsFlat = tagsFlat;
    }

    ProductTags getTags() {
      return tags;
    }

    List<String> getTagsFlat() {
      return tagsFlat;
    }
  }

  private static class TagAccumulator {
    private final Map<TagCode, LinkedHashSet<String>> byCode = new EnumMap<>(TagCode.class);

    void add(TagCode code, String tagName) {
      if (tagName == null || tagName.isBlank()) {
        return;
      }
      byCode.computeIfAbsent(code, ignored -> new LinkedHashSet<>()).add(tagName);
    }

    TagsBundle toBundle() {
      List<String> space = listFor(TagCode.SPACE);
      List<String> tone = listFor(TagCode.TONE);
      List<String> situation = listFor(TagCode.SITUATION);
      List<String> mood = listFor(TagCode.MOOD);
      ProductTags tags = new ProductTags(space, tone, situation, mood);
      LinkedHashSet<String> flat = new LinkedHashSet<>();
      addAll(flat, space);
      addAll(flat, tone);
      addAll(flat, situation);
      addAll(flat, mood);
      return new TagsBundle(tags, new ArrayList<>(flat));
    }

    private List<String> listFor(TagCode code) {
      LinkedHashSet<String> values = byCode.get(code);
      if (values == null || values.isEmpty()) {
        return Collections.emptyList();
      }
      return new ArrayList<>(values);
    }

    private void addAll(LinkedHashSet<String> target, List<String> source) {
      if (source == null || source.isEmpty()) {
        return;
      }
      for (String value : source) {
        if (value != null && !value.isBlank()) {
          target.add(value);
        }
      }
    }
  }
}
