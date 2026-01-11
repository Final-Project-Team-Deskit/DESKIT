package com.deskit.deskit.product.controller;

import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.oauth.CustomOAuth2User;
import com.deskit.deskit.account.repository.SellerRepository;
import com.deskit.deskit.product.dto.ProductCreateRequest;
import com.deskit.deskit.product.dto.ProductCreateResponse;
import com.deskit.deskit.product.dto.ProductImageResponse;
import com.deskit.deskit.product.dto.ProductTagUpdateRequest;
import com.deskit.deskit.product.entity.ProductImage.ImageType;
import com.deskit.deskit.product.service.ProductImageService;
import com.deskit.deskit.product.service.ProductService;
import com.deskit.deskit.product.service.ProductTagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

  private final ProductService productService;
  private final ProductImageService productImageService;
  private final ProductTagService productTagService;
  private final SellerRepository sellerRepository;

  public SellerProductController(ProductService productService,
                                 ProductImageService productImageService,
                                 ProductTagService productTagService,
                                 SellerRepository sellerRepository) {
    this.productService = productService;
    this.productImageService = productImageService;
    this.productTagService = productTagService;
    this.sellerRepository = sellerRepository;
  }

  @PostMapping
  public ResponseEntity<ProductCreateResponse> createProduct(
          @AuthenticationPrincipal CustomOAuth2User user,
          @Valid @RequestBody ProductCreateRequest request
  ) {
    Long sellerId = resolveSellerId(user);
    return ResponseEntity.ok(productService.createProduct(sellerId, request));
  }

  @PostMapping("/{productId}/images")
  public ResponseEntity<ProductImageResponse> uploadProductImage(
          @AuthenticationPrincipal CustomOAuth2User user,
          @PathVariable("productId") Long productId,
          @RequestParam("file") MultipartFile file,
          @RequestParam("imageType") ImageType imageType,
          @RequestParam("slotIndex") Integer slotIndex
  ) {
    Long sellerId = resolveSellerId(user);
    return ResponseEntity.ok(
      productImageService.uploadImage(sellerId, productId, file, imageType, slotIndex)
    );
  }

  @PutMapping("/{productId}/tags")
  public ResponseEntity<Void> updateProductTags(
          @AuthenticationPrincipal CustomOAuth2User user,
          @PathVariable("productId") Long productId,
          @Valid @RequestBody ProductTagUpdateRequest request
  ) {
    Long sellerId = resolveSellerId(user);
    productTagService.updateProductTags(sellerId, productId, request);
    return ResponseEntity.ok().build();
  }

  private Long resolveSellerId(CustomOAuth2User user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
    }

    String role = user.getAuthorities().iterator().next().getAuthority();
    if (role == null || !role.startsWith("ROLE_SELLER")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "seller role required");
    }

    String loginId = user.getUsername();
    if (loginId == null || loginId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "seller not found");
    }

    Seller seller = sellerRepository.findByLoginId(loginId);
    if (seller == null || seller.getSellerId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "seller not found");
    }

    return seller.getSellerId();
  }
}
