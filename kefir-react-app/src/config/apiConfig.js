// src/config/apiConfig.js - ИСПРАВЛЕННАЯ ВЕРСИЯ
const API_CONFIG = {
  // Пустая база для прокси
  BASE_URL: '',
  
  ENDPOINTS: {
    AUTH: {
      LOGIN: '/api/auth/login',
      LOGOUT: '/api/auth/logout',
      REGISTER: '/api/auth/register',
      PROFILE: '/api/auth/profile',
      VALIDATE: '/api/auth/validate',
      CHECK: '/api/auth/check'
    },
    CLIENTS: '/api/clients',
    PRODUCTS: '/api/products',
    ORDERS: '/api/orders',
    CARTS: '/api/carts',
    DELIVERIES: '/api/deliveries',
    COURIERS: '/api/couriers',
    COLLECTORS: '/api/collectors',
    WAREHOUSE: '/api/warehouse',
    OFFICE: '/api/office'
  },
  
  // Простая функция
  getBaseUrl: function() {
    return '';
  },
  
  // Метод для отладки
  debug: function() {
    console.log('API_CONFIG debug:');
    console.log('Base URL:', this.getBaseUrl());
    console.log('Endpoints:', this.ENDPOINTS);
    console.log('Full clients URL:', this.getBaseUrl() + this.ENDPOINTS.CLIENTS);
  }
};

// Проверка при загрузке
console.log('=== API_CONFIG loaded ===');
console.log('Base URL will be:', API_CONFIG.getBaseUrl());
console.log('Clients endpoint will be:', API_CONFIG.getBaseUrl() + API_CONFIG.ENDPOINTS.CLIENTS);

export default API_CONFIG;