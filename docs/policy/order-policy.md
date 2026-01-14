# 주문 정책서

## 정책 요약(핵심 규칙)
- 주문 생성 시 회원/요청/아이템 검증을 통과해야 하며, 상품은 ON_SALE 상태 + 재고 충분해야 한다 (근거: `OrderService.createOrder`)
- 주문 금액은 실시간 판매가(라이브 가격) 우선 적용 후 합산하며, 배송비는 50,000원 미만이면 3,000원 (근거: `OrderService.createOrder`#L94-L126)
- 결제 확정 시 Toss 확인 로직에서 주문 금액 변동이 있으면 주문을 취소 처리하고 409로 응답한다 (근거: `TossPaymentService`#L94-L113, #L250-L276)
- 주문 취소 요청은 상태에 따라 CANCEL_REQUESTED 또는 REFUND_REQUESTED로 전이하며, PAID 취소는 즉시 환불 승인(REFUNDED)까지 진행된다 (근거: `Order.requestCancel`, `OrderService.requestCancel`)

## 상태/전이 표
| 트리거 | 현재 상태 | 다음 상태 | 조건/비고 |
|---|---|---|---|
| 주문 생성 | (없음) | CREATED | 최초 생성 (근거: `OrderService.createOrder`)
| 결제 확정(Toss) | CREATED | PAID | 금액 일치 + 현재가 검증 통과 (근거: `TossPaymentService.updateOrderPaid`, `Order.markPaid`)
| 취소 요청 | CREATED | CANCEL_REQUESTED | 사용자가 취소 요청 (근거: `Order.requestCancel`)
| 취소 요청 | PAID | REFUND_REQUESTED | 결제 완료 주문 환불 요청 (근거: `Order.requestCancel`)
| 환불 승인 | REFUND_REQUESTED | REFUNDED | `Order.approveRefund` 실행 (근거: `OrderService.requestCancel`)
| 취소 승인 | CANCEL_REQUESTED | CANCELLED | `Order.approveCancel` 필요 (현재 일반 취소 흐름에서는 호출되지 않음) (근거: `Order.approveCancel`, `OrderService.requestCancel`)
| 가격 변동 취소 | CREATED | CANCELLED | 결제 확인 시 가격 변동이면 `requestCancel` + `approveCancel` 수행 (근거: `TossPaymentService.cancelOrderDueToPriceChange`)
| 환불 거절 | REFUND_REQUESTED | REFUND_REJECTED | 별도 호출 필요(현재 공개 API 없음) (근거: `Order.rejectRefund`)

## 주요 제약
### 생성 요청 제약
- 필수 필드: items(1개 이상), receiver(<=20), postcode(5자리), addr_detail(<=255) (근거: `CreateOrderRequest`)
- 아이템: product_id 필수, quantity > 0 (근거: `CreateOrderItemRequest`)
- 상품 상태/재고 검증
  - `findByIdForUpdateAndStatus(..., ON_SALE)`로 ON_SALE만 허용
  - 재고 부족 시 409 `insufficient stock` (근거: `OrderService.createOrder`#L94-L108)

### 금액 계산 규칙
- 단가: 라이브 방송가가 있으면 우선, 없으면 상품 가격 (근거: `OrderService.resolveCurrentPrice`)
- 배송비: 총 상품 금액 50,000원 미만이면 3,000원 (근거: `OrderService.createOrder`#L115-L123)
- 총 결제 금액: `total_product_amount - discount_fee + shipping_fee` (근거: `OrderService.createOrder`#L115-L126)

### 취소/환불 규칙
- CREATED 상태 취소 요청은 CANCEL_REQUESTED까지만 전이됨 (승인 로직은 별도 필요) (근거: `OrderService.requestCancel`#L209-L242)
- PAID 상태 취소 요청은 Toss 결제 취소 후 즉시 REFUNDED로 전이 (근거: `OrderService.requestCancel`#L235-L242)

## API 엔드포인트 기준 동작
- POST `/api/orders`
  - 정상: 200 + `order_id`, `order_number`, `status`, `order_amount`
  - 오류: 400(member_id/items/receiver/postcode/addr_detail), 404(member/product), 409(insufficient stock) (근거: `OrderService.createOrder`)
- GET `/api/orders`
  - 본인 주문 요약 리스트 (근거: `OrderController.getMyOrders`)
- GET `/api/orders/{orderId}`
  - 본인 주문 상세 + 아이템 (근거: `OrderService.getMyOrderDetail`)
- PATCH `/api/orders/{orderId}/cancel`
  - 주문 취소 요청, 상태에 따라 CANCEL_REQUESTED 또는 REFUND_REQUESTED/REFUNDED (근거: `OrderService.requestCancel`)

## DB 스키마 근거
- `order` 테이블: status ENUM (CREATED/PAID/CANCEL_REQUESTED/CANCELLED/COMPLETED/REFUND_REQUESTED/REFUND_REJECTED/REFUNDED), order_amount/total_product_amount/shipping_fee/discount_fee (근거: `livecommerce_create_table.sql`#L306-L336)
- `order_item` 테이블: 상품 스냅샷(product_name/unit_price/quantity/subtotal_price) + deleted_at (근거: `livecommerce_create_table.sql`#L337-L356)

## 프론트 플로우/가드
- 주문 생성 시 프론트는 product_id/quantity만 전송하고, quantity > 0인 아이템만 포함 (근거: `front/src/pages/Checkout.vue`#L502-L509)
- 배송 정보(수령인/우편번호/상세주소) 조합 후 `addr_detail`로 전송 (근거: `Checkout.vue`#L513-L527)
- 결제 전 단계에서 가격 재동기화가 수행되며, 가격 변경 감지 시 결제 진행을 중단함 (근거: `Checkout.vue`#L479-L498, #L500-L512)

## 확인 필요
- `Order` 엔티티는 `BaseEntity`를 상속하지만, 스키마(`order` 테이블)에 `deleted_at` 컬럼이 없음. 실제 운영 DB/마이그레이션에 `deleted_at`이 존재하는지 확인 필요 (근거: `BaseEntity.java`, `livecommerce_create_table.sql`#L306-L336)
- 일반 취소 흐름에서 CANCEL_REQUESTED -> CANCELLED 승인 로직이 공개 API에 존재하지 않음. 운영 정책상 자동 승인 여부 확인 필요 (근거: `OrderService.requestCancel`, `Order.approveCancel`)

## Evidence map
- Backend 핵심
  - `src/main/java/com/deskit/deskit/order/service/OrderService.java`
  - `src/main/java/com/deskit/deskit/order/entity/Order.java`
  - `src/main/java/com/deskit/deskit/order/entity/OrderItem.java`
  - `src/main/java/com/deskit/deskit/order/enums/OrderStatus.java`
  - `src/main/java/com/deskit/deskit/order/controller/OrderController.java`
  - `src/main/java/com/deskit/deskit/order/dto/*.java`
  - `src/main/java/com/deskit/deskit/order/payment/service/TossPaymentService.java`
  - `src/main/resources/sql/livecommerce_create_table.sql`
- Frontend 핵심
  - `front/src/pages/Checkout.vue`
