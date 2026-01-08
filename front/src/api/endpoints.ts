export const endpoints = {
  products: '/api/products',
  productDetail: (id: string | number) => `/api/products/${id}`,
  setups: '/api/setups',
  setupDetail: (id: string | number) => `/api/setups/${id}`,
  homePopularProducts: '/api/home/popular-products',
  homePopularSetups: '/api/home/popular-setups',
  cart: '/api/cart',
  cartItems: '/api/cart/items',
  orders: '/api/orders',
  orderDetail: (id: string | number) => `/api/orders/${id}`,
  orderStatus: (id: string | number) => `/api/orders/${id}/status`,
}
