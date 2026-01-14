# 상품 정책서

## 정책 요약(핵심 규칙)
- 상품 상태 전이는 `Product.Status.canTransitionTo`에 의해 제한된다. 예: DRAFT -> READY, READY -> ON_SALE/HIDDEN 등 (근거: `src/main/java/com/deskit/deskit/product/entity/Product.java`#L23-L54)
- 판매자 상품 기본 정보 수정(PUT `/api/seller/products/{id}`)은 상태 및 주문 존재 여부에 따라 제한된다. ON_SALE 상태에서는 `product_name`, `stock_qty` 수정 불가 (근거: `src/main/java/com/deskit/deskit/product/service/ProductService.java`#L309-L323)
- 결제 완료 주문(PAID/COMPLETED)이 존재하는 상품은 가격/정보(상품명, 한 줄 설명, 가격, 상세HTML) 변경이 409로 차단된다 (근거: `src/main/java/com/deskit/deskit/product/service/ProductService.java`#L326-L339)
- 상품 이미지 업데이트는 5 슬롯 고정이며, `image_urls` 길이 5 필수, `image_keys`는 선택이지만 존재 시 길이 5/정합성(키만 존재 불가) 강제 (근거: `src/main/java/com/deskit/deskit/product/service/ProductService.java`#L362-L399)
- 상품 이미지 업로드는 확장자/용량/비율 검사: jpg/jpeg/png, 5MB, 1:1 비율. 컨트롤러는 content-type `image/*` 추가 검사 (근거: `AwsS3Service`, `UploadType`, `SellerProductController`)

## 상태/전이 표
| 현재 상태 | 허용 전이 | 불가/금지 전이 | 조건/비고 |
|---|---|---|---|
| DRAFT | READY, DELETED | 그 외 | 등록 완료 전 상태 (근거: `Product.Status.canTransitionTo`) |
| READY | ON_SALE, HIDDEN | 그 외 | 판매 준비 완료 상태 |
| ON_SALE | SOLD_OUT, PAUSED, HIDDEN | READY/DELETED 등 | 판매중 상태 |
| SOLD_OUT | ON_SALE, HIDDEN | 그 외 | 재고 부족 상태 |
| PAUSED | ON_SALE, HIDDEN | 그 외 | 일시 중지 |
| HIDDEN | READY, DELETED | 그 외 | 숨김 상태 |
| LIMITED_SALE | 없음 | 모든 전이 | 파생 상태(ON_SALE + 재고 임박), 직접 전이 금지 (근거: `Product.java`#L30-L53, #L179-L188) |
| DELETED | 없음 | 모든 전이 | 논리 삭제 상태 |

## 주요 제약
### 필드 제약/검증
- 생성(POST `/api/seller/products`)
  - `product_name`, `short_desc` 필수, `price >= 0`, `stock_qty >= 0`, `cost_price >= 0` (근거: `ProductCreateRequest`, `ProductService.createProduct`)
  - `detail_html`는 null이면 빈 문자열로 보정 (근거: `ProductService.createProduct`)
- 기본 수정(PUT `/api/seller/products/{id}`)
  - 요청에 수정 필드가 하나도 없으면 400 (근거: `ProductService.updateProductBasicInfo`#L294-L307)
  - 허용 상태: DRAFT/READY/PAUSED/ON_SALE (근거: `ProductService`#L309-L315)
  - ON_SALE 상태에서 `product_name`, `stock_qty` 수정 불가 (근거: `ProductService`#L317-L323)
  - 결제 주문 존재 시 가격/정보 수정 차단: `product_name`, `short_desc`, `price`, `detail_html` 변경 시 409 (근거: `ProductService`#L326-L339)
- 상세 수정(PATCH `/api/seller/products/{id}/detail`)
  - `detail_html` 필수 (근거: `ProductDetailUpdateRequest`, `ProductService.updateProductDetailHtml`)
- 상태 변경(PATCH `/api/seller/products/{id}/status`)
  - `status` 필수, 전이 규칙 위반 시 400 `invalid status transition` (근거: `SellerProductStatusUpdateRequest`, `Product.changeStatus`)

### 이미지 정책
- 업로드 엔드포인트: POST `/api/seller/products/images/upload`
  - content-type `image/*` 필수 (근거: `SellerProductController.uploadProductImageFile`)
  - 허용 확장자: jpg/jpeg/png (근거: `AwsS3Service`#L32-L58)
  - 최대 용량: 5MB (UploadType.PRODUCT_IMAGE) + 전역 멀티파트 10MB (근거: `UploadType`, `application.properties`)
  - 비율: 1:1, 허용 오차 ±0.05 (근거: `UploadType`#L12-L19, `AwsS3Service.validateImageRatio`)
- 저장 정책
  - 이미지 슬롯 5개 고정, slotIndex 0=THUMBNAIL, 1~4=GALLERY (근거: `ProductService` 이미지 업데이트 루프, `ProductImage`)
  - `image_urls` 길이 5 필수, `image_keys`는 선택이지만 존재 시 길이 5/정합성 필수 (근거: `ProductService`#L362-L399)
  - 슬롯 단위 교체/삭제: 동일 URL+KEY는 유지, 다르면 기존 소프트 삭제 후 신규 생성 (근거: `ProductService`#L401-L456)
  - 저장 시 기존 객체 키가 있으면 Object Storage delete 시도(실패 시 warn 로그) (근거: `ProductService`#L403-L452)

### 태그 정책
- 판매자 태그 목록: GET `/api/seller/tags` (seller 권한 필요, ACTIVE만 허용) (근거: `TagController`)
- 상품 태그 갱신: PUT `/api/seller/products/{id}/tags`
  - `tag_ids` 필수, 유효한 태그만 허용, 중복 제거 후 재매핑 (근거: `ProductTagService`)

## API 엔드포인트 기준 동작
### Seller API
- POST `/api/seller/products` : 상품 생성
  - 오류: 400(product_name/short_desc/price/stock_qty), 403(seller), 404(seller not found) (근거: `ProductService.createProduct`, `SellerProductController.resolveSellerId`)
- PUT `/api/seller/products/{id}` : 기본 정보/이미지 업데이트
  - 오류: 400(request/status not allowed/이미지 길이), 403(forbidden), 404(product not found), 409(주문 존재 시 정보 수정 불가) (근거: `ProductService.updateProductBasicInfo`)
- PATCH `/api/seller/products/{id}/detail` : 상세 HTML 업데이트
- PATCH `/api/seller/products/{id}/status` : 상태 변경
- PATCH `/api/seller/products/{id}/complete` : 등록 완료(DRAFT -> READY), detail_html 필수 (근거: `ProductService.completeProductRegistration`)
- DELETE `/api/seller/products/{id}` : 소프트 삭제(삭제 시각만 설정) (근거: `ProductService.softDeleteProduct`)
- POST `/api/seller/products/images/upload` : 이미지 업로드, 응답 `{url, key}` (근거: `ProductImageUploadResponse`)

### Public API
- GET `/api/products` : ON_SALE 상품만 조회 (근거: `ProductService.getProducts`)
- GET `/api/products/{id}` : ON_SALE 단건 조회 (근거: `ProductService.getProduct`)

## DB 스키마 근거
- `product` 테이블: status ENUM, detail_html LONGTEXT, soft delete 컬럼 `deleted_at` (근거: `src/main/resources/sql/livecommerce_create_table.sql`#L186-L213)
- `product_image` 테이블: `product_image_url`, `stored_file_name`, `image_type`, `slot_index`, unique 키 `(product_id, image_type, slot_index)` (근거: `livecommerce_create_table.sql`#L214-L228)
- `product_tag` 테이블: 복합 PK(product_id, tag_id) + deleted_at (근거: `livecommerce_create_table.sql`#L230-L238)

## 프론트 플로우/가드
- 상품 등록 기본 정보 페이지
  - 썸네일(slot 0) 필수, 업로드 중이면 진행 차단 (근거: `front/src/pages/seller/ProductCreateInfo.vue`#L205-L221)
  - 이미지 업로드 전 1:1 크롭 모달 사용, 5MB 초과 시 차단 (근거: `ProductCreateInfo.vue`#L90-L177, `LiveImageCropModal.vue`)
  - 등록 후 이미지 저장: PUT `/api/seller/products/{id}`에 `image_urls`/`image_keys` 길이 5 전송 (근거: `ProductCreateInfo.vue`#L242-L290)
- 상품 수정 기본 정보 페이지
  - 이미지 슬롯 5개 고정, 업로드 중이면 저장/삭제 차단 (근거: `front/src/pages/seller/ProductEditInfo.vue`#L18-L38, #L233-L311)
  - 이미지 변경 시 `image_urls`/`image_keys` 길이 5 전송, 썸네일 누락 시 저장 차단 (근거: `ProductEditInfo.vue`#L311-L351)

## 확인 필요
- 프론트 `apiBase` 구성에 따라 `/api` prefix 여부가 다름. 예: `ProductCreateInfo.vue`는 `${apiBase}/seller/products`로 호출 (근거: `ProductCreateInfo.vue`#L223-L238). 실제 VITE_API_BASE_URL 값에 `/api`가 포함되는지 확인 필요.
- `Product.Status.DELETED`는 상태 전이 규칙에 존재하지만, 실제 삭제는 `deleted_at`만 설정됨. 상태 필드가 DELETED로 세팅되는 경로가 없는지 확인 필요 (근거: `ProductService.softDeleteProduct`, `Product.Status.canTransitionTo`).

## Evidence map
- Backend 핵심
  - `src/main/java/com/deskit/deskit/product/service/ProductService.java`
  - `src/main/java/com/deskit/deskit/product/entity/Product.java`
  - `src/main/java/com/deskit/deskit/product/entity/ProductImage.java`
  - `src/main/java/com/deskit/deskit/product/dto/*.java`
  - `src/main/java/com/deskit/deskit/product/controller/SellerProductController.java`
  - `src/main/java/com/deskit/deskit/tag/controller/TagController.java`
  - `src/main/java/com/deskit/deskit/livehost/service/AwsS3Service.java`
  - `src/main/java/com/deskit/deskit/livehost/common/enums/UploadType.java`
  - `src/main/java/com/deskit/deskit/livehost/common/exception/ErrorCode.java`
  - `src/main/resources/sql/livecommerce_create_table.sql`
- Frontend 핵심
  - `front/src/pages/seller/ProductCreateInfo.vue`
  - `front/src/pages/seller/ProductEditInfo.vue`
  - `front/src/components/LiveImageCropModal.vue`
