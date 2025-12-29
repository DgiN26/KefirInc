// src/services/realApi.js
import axios from 'axios';
import API_CONFIG from '../config/apiConfig';

// Создаем основной инстанс axios
const api = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Интерцептор для добавления токена
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Интерцептор для обработки ошибок
api.interceptors.response.use(
  response => response,
  error => {
    console.error('API Error:', error.response || error.message);
    
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }
    
    if (error.code === 'ERR_NETWORK') {
      console.error('Сетевая ошибка. Проверьте доступность API Gateway.');
    }
    
    return Promise.reject(error);
  }
);

// Обертка для обработки ошибок
// realApi.js - правильная версия
const handleRequest = async (request) => {
  try {
    const response = await request;
    return response.data;  // Axios уже оборачивает в data
  } catch (error) {
    console.error('Request failed:', error.message);
    throw error;
  }
};

// ==================== AUTH SERVICE ====================
export const realAuthAPI = {
  login: (credentials) => handleRequest(
    api.post(API_CONFIG.ENDPOINTS.AUTH.LOGIN, credentials)
  ),
  
  logout: () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    return Promise.resolve();
  },
  
  getProfile: () => handleRequest(
    api.get(API_CONFIG.ENDPOINTS.AUTH.PROFILE)
  ),
  
  register: (userData) => handleRequest(
    api.post(API_CONFIG.ENDPOINTS.AUTH.REGISTER, userData)
  ),
  
  verifyToken: () => handleRequest(
    api.get(API_CONFIG.ENDPOINTS.AUTH.VERIFY)
  ),
};

// ==================== CLIENTS SERVICE ====================
export const realClientsAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.CLIENTS)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.CLIENTS}/${id}`)),
  create: (client) => handleRequest(api.post(API_CONFIG.ENDPOINTS.CLIENTS, client)),
  update: (id, client) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.CLIENTS}/${id}`, client)),
  delete: (id) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.CLIENTS}/${id}`)),
};

// ==================== PRODUCTS SERVICE ====================
export const realProductsAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.PRODUCTS)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.PRODUCTS}/${id}`)),
  create: (product) => handleRequest(api.post(API_CONFIG.ENDPOINTS.PRODUCTS, product)),
  update: (id, product) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.PRODUCTS}/${id}`, product)),
  delete: (id) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.PRODUCTS}/${id}`)),
  search: (query) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.PRODUCTS}/search`, { params: { q: query } })),
  getByCategory: (category) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.PRODUCTS}/category/${category}`)),
};

// ==================== ORDERS SERVICE ====================
export const realOrdersAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.ORDERS)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.ORDERS}/${id}`)),
  create: (order) => handleRequest(api.post(API_CONFIG.ENDPOINTS.ORDERS, order)),
  updateStatus: (id, status) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.ORDERS}/${id}/status`, { status })),
  getByUser: (userId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.ORDERS}/user/${userId}`)),
  cancel: (id) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.ORDERS}/${id}`)),
};

// ==================== CART SERVICE ====================
export const realCartAPI = {
  getCart: (userId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}`)),
  addToCart: (userId, item) => handleRequest(api.post(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}/items`, item)),
  updateQuantity: (userId, itemId, quantity) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}/items/${itemId}`, { quantity })),
  removeFromCart: (userId, itemId) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}/items/${itemId}`)),
  clearCart: (userId) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}`)),
  getTotal: (userId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.CARTS}/user/${userId}/total`)),
};

// ==================== DELIVERIES SERVICE ====================
export const realDeliveriesAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.DELIVERIES)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.DELIVERIES}/${id}`)),
  create: (delivery) => handleRequest(api.post(API_CONFIG.ENDPOINTS.DELIVERIES, delivery)),
  updateStatus: (id, status) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.DELIVERIES}/${id}/status`, { status })),
  assignCourier: (id, courierId) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.DELIVERIES}/${id}/assign`, { courierId })),
  getByCourier: (courierId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.DELIVERIES}/courier/${courierId}`)),
  cancel: (id) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.DELIVERIES}/${id}`)),
};

// ==================== COURIERS SERVICE ====================
export const realCouriersAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.COURIERS)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.COURIERS}/${id}`)),
  create: (courier) => handleRequest(api.post(API_CONFIG.ENDPOINTS.COURIERS, courier)),
  update: (id, courier) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.COURIERS}/${id}`, courier)),
  delete: (id) => handleRequest(api.delete(`${API_CONFIG.ENDPOINTS.COURIERS}/${id}`)),
  getAvailable: () => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.COURIERS}/available`)),
  updateStatus: (id, status) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.COURIERS}/${id}/status`, { status })),
  getStats: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.COURIERS}/${id}/stats`)),
};

// ==================== COLLECTORS SERVICE ====================
export const realCollectorsAPI = {
  getAll: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.COLLECTORS)),
  getById: (id) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.COLLECTORS}/${id}`)),
  create: (collector) => handleRequest(api.post(API_CONFIG.ENDPOINTS.COLLECTORS, collector)),
  update: (id, collector) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.COLLECTORS}/${id}`, collector)),
  assignTask: (collectorId, orderId) => handleRequest(api.post(`${API_CONFIG.ENDPOINTS.COLLECTORS}/${collectorId}/tasks`, { orderId })),
  getTasks: (collectorId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.COLLECTORS}/${collectorId}/tasks`)),
};

// ==================== OFFICE SERVICE ====================
export const realOfficeAPI = {
  getStats: () => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.OFFICE}/stats`)),
  getSystemHealth: () => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.OFFICE}/health`)),
  generateReport: (type) => handleRequest(api.post(`${API_CONFIG.ENDPOINTS.OFFICE}/reports`, { type })),
  getFinancialStats: (period) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.OFFICE}/financial`, { params: { period } })),
};

// ==================== WAREHOUSE SERVICE ====================
export const realWarehouseAPI = {
  getInventory: () => handleRequest(api.get(API_CONFIG.ENDPOINTS.WAREHOUSE)),
  updateStock: (productId, count) => handleRequest(api.put(`${API_CONFIG.ENDPOINTS.WAREHOUSE}/products/${productId}/stock`, { count })),
  getLowStock: () => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.WAREHOUSE}/low-stock`)),
  addProduct: (product) => handleRequest(api.post(`${API_CONFIG.ENDPOINTS.WAREHOUSE}/products`, product)),
  getMovements: (productId) => handleRequest(api.get(`${API_CONFIG.ENDPOINTS.WAREHOUSE}/products/${productId}/movements`)),
};

// ==================== DEFAULT EXPORT ====================
// Для удобства можно экспортировать все API одним объектом
const allAPIs = {
  auth: realAuthAPI,
  clients: realClientsAPI,
  products: realProductsAPI,
  orders: realOrdersAPI,
  cart: realCartAPI,
  deliveries: realDeliveriesAPI,
  couriers: realCouriersAPI,
  collectors: realCollectorsAPI,
  office: realOfficeAPI,
  warehouse: realWarehouseAPI,
};

export default allAPIs;