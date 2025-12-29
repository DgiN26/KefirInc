// src/config/config.js
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  
  // Ваши реальные endpoints
  ENDPOINTS: {
    AUTH: {
      LOGIN: '/api/auth/login',
      LOGOUT: '/api/auth/logout',
      REGISTER: '/api/auth/register'
    },
    CLIENTS: '/api/clients',
    PRODUCTS: '/api/products',
    ORDERS: '/api/orders',
    CARTS: '/api/carts',
    DELIVERIES: '/api/deliveries',
    COURIERS: '/api/couriers',
    COLLECTORS: '/api/collector/collectors',
    WAREHOUSE: '/api/warehouse'
  }
};