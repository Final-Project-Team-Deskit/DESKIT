import { http } from './http'
import { endpoints } from './endpoints'
import type {
  CreateOrderRequest,
  CreateOrderResponse,
  OrderDetailResponse,
  OrderStatusUpdateRequest,
  OrderStatusUpdateResponse,
  OrderSummaryResponse,
} from './types/orders'

const withCredentials = { withCredentials: true }

export const createOrder = async (
  request: CreateOrderRequest,
): Promise<CreateOrderResponse> => {
  const response = await http.post<CreateOrderResponse>(endpoints.orders, request, withCredentials)
  return response.data
}

export const getMyOrders = async (): Promise<OrderSummaryResponse[]> => {
  const response = await http.get<OrderSummaryResponse[]>(endpoints.orders, withCredentials)
  return response.data
}

export const getMyOrderDetail = async (orderId: number): Promise<OrderDetailResponse> => {
  const response = await http.get<OrderDetailResponse>(endpoints.orderDetail(orderId), withCredentials)
  return response.data
}

export const updateOrderStatus = async (
  orderId: number,
  request: OrderStatusUpdateRequest,
): Promise<OrderStatusUpdateResponse> => {
  const response = await http.patch<OrderStatusUpdateResponse>(
    endpoints.orderStatus(orderId),
    request,
    withCredentials,
  )
  return response.data
}
