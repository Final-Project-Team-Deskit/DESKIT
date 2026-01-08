export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED' | 'COMPLETED'

export interface CreateOrderItemRequest {
  product_id: number
  quantity: number
}

export interface CreateOrderRequest {
  items: CreateOrderItemRequest[]
}

export interface CreateOrderResponse {
  order_id: number
  order_number: string
  status: OrderStatus
  order_amount: number
}

export interface OrderSummaryResponse {
  order_id: number
  order_number: string
  status: OrderStatus
  order_amount: number
  created_at: string
}

export interface OrderItemResponse {
  product_id: number
  quantity: number
  unit_price: number
  subtotal_price: number
}

export interface OrderDetailResponse {
  order_id: number
  order_number: string
  status: OrderStatus
  order_amount: number
  created_at: string
  items: OrderItemResponse[]
}

export interface OrderStatusUpdateRequest {
  status: OrderStatus
}

export interface OrderStatusUpdateResponse {
  order_id: number
  status: OrderStatus
}
