export const endpoints = {
  products: '/api/products',
  productDetail: (id: string | number) => `/api/products/${id}`,
  setups: '/api/setups',
  setupDetail: (id: string | number) => `/api/setups/${id}`,
  homePopularProducts: '/api/home/popular-products',
  homePopularSetups: '/api/home/popular-setups',
  cart: '/cart',
  cartItems: '/cart/items',
  orders: '/orders',
  orderDetail: (id: string | number) => `/orders/${id}`,
}
