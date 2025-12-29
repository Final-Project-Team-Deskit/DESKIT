export const endpoints = {
  products: '/products',
  productDetail: (id: string | number) => `/products/${id}`,
  setups: '/setups',
  setupDetail: (id: string | number) => `/setups/${id}`,
  cart: '/cart',
  cartItems: '/cart/items',
  orders: '/orders',
  orderDetail: (id: string | number) => `/orders/${id}`,
}
