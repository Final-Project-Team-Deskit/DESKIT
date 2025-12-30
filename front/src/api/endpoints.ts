export const endpoints = {
  products: '/api/products',
  productDetail: (id: string | number) => `/api/products/${id}`,
  setups: '/setups',
  setupDetail: (id: string | number) => `/setups/${id}`,
  cart: '/cart',
  cartItems: '/cart/items',
  orders: '/orders',
  orderDetail: (id: string | number) => `/orders/${id}`,
}
