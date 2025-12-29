import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

console.log('API base URL:', API_URL);

const api = axios.create({
  baseURL: API_URL,  // Теперь здесь /api
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Единый interceptor для логирования
api.interceptors.request.use(
  config => {
    console.log(`[API] ${config.method.toUpperCase()} ${config.baseURL}${config.url}`);
    console.log('[API] Data:', config.data);
    
    const token = localStorage.getItem('token') || localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('[API] Token added');
    }
    
    return config;
  },
  error => {
    console.error('[API] Request error:', error);
    return Promise.reject(error);
  }
);

// Единый interceptor для обработки ответов
api.interceptors.response.use(
  response => {
    console.log(`[API] Response ${response.status}:`, response.data);
    return response;
  },
  error => {
    console.error('[API] Response error:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    
    // Обработка 401 ошибки
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      localStorage.removeItem('userData');
      localStorage.removeItem('userRole');
      window.location.href = '/login';
    }
    
    return Promise.reject(error);
  }
);

// Аутентификация API
export const authAPI = {
  login: async (credentials) => {
    try {
      const response = await api.post('/auth/login', { 
        username: credentials.username,
        password: credentials.password
      });
      
      console.log('Login response:', response.data);
      
      const responseData = response.data.body || response.data;
      const token = responseData.token || responseData.accessToken;
      const user = responseData.user || responseData;
      
      if (token) {
        localStorage.setItem('token', token);
      }
      if (user) {
        localStorage.setItem('user', JSON.stringify(user));
      }
      
      return responseData;
    } catch (error) {
      console.error('Login API error:', error);
      throw error;
    }
  },
  
  logout: async () => {
    try {
      const response = await api.post('/auth/logout');
      return response.data;
    } catch (error) {
      console.error('Logout error:', error);
      throw error;
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  },
  
  getProfile: async () => {
    const response = await api.get('/auth/profile');
    return response.data;
  },
  
  // Проверка доступности API
  check: async () => {
    try {
      const response = await api.get('/auth/check');
      return response.data;
    } catch (error) {
      console.error('API check failed:', error);
      throw error;
    }
  },
  
  // Регистрация пользователя
  register: async (userData) => {
    try {
      const payload = {
        username: userData.username,
        firstname: userData.name,
        email: userData.email,
        password: userData.password,
        city: userData.city || '',
        age: userData.age || null,
        magaz: userData.magaz || ''
      };
      
      console.log('Sending registration data:', payload);
      const response = await api.post('/clients/register', payload);
      return response.data;
    } catch (error) {
      console.error('Registration API error:', error);
      throw error;
    }
  }
};

// PostgreSQL Database API
export const databaseAPI = {
  testConnection: async () => {
    try {
      const response = await api.get('/database/test-connection');
      return response.data;
    } catch (error) {
      console.error('Database connection test failed:', error);
      return { connected: false, error: error.message };
    }
  },
  
  getStats: async () => {
    try {
      const response = await api.get('/database/stats');
      return response.data;
    } catch (error) {
      console.error('Failed to get database stats:', error);
      throw error;
    }
  },
  
  executeQuery: async (data) => {
    try {
      const response = await api.post('/database/query', data);
      return response.data;
    } catch (error) {
      console.error('Query execution failed:', error);
      throw error;
    }
  },
  
  createBackup: async () => {
    try {
      const response = await api.post('/database/backup');
      return response.data;
    } catch (error) {
      console.error('Backup creation failed:', error);
      throw error;
    }
  },
  
  restoreBackup: async (backupData) => {
    try {
      const response = await api.post('/database/restore', backupData);
      return response.data;
    } catch (error) {
      console.error('Backup restoration failed:', error);
      throw error;
    }
  }
};

// Suppliers API для PostgreSQL
export const suppliersAPI = {
  getAll: async () => {
    try {
      const response = await api.get('/suppliers');
      return response.data;
    } catch (error) {
      console.error('Error fetching suppliers:', error);
      return [];
    }
  },
  
  create: async (supplier) => {
    const response = await api.post('/suppliers', supplier);
    return response.data;
  },
  
  update: async (id, supplier) => {
    const response = await api.put(`/suppliers/${id}`, supplier);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await api.delete(`/suppliers/${id}`);
    return response.data;
  }
};

// Categories API для PostgreSQL
export const categoriesAPI = {
  getAll: async () => {
    try {
      const response = await api.get('/categories');
      return response.data;
    } catch (error) {
      console.error('Error fetching categories:', error);
      return [];
    }
  },
  
  create: async (category) => {
    const response = await api.post('/categories', category);
    return response.data;
  },
  
  update: async (id, category) => {
    const response = await api.put(`/categories/${id}`, category);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await api.delete(`/categories/${id}`);
    return response.data;
  }
};

// Клиенты API
export const clientsAPI = {
  getAll: async () => {
    const response = await api.get('/clients');
    return response.data;
  },
  
  getById: async (id) => {
    const response = await api.get(`/admin/clients/${id}`);
    return response.data;
  },
  
  create: async (clientData) => {
    const response = await api.post('/admin/clients', clientData);
    return response.data;
  },
  
  update: async (id, clientData) => {
    console.log('Updating client:', id);
    
    const payload = {};
    for (const key in clientData) {
      if (key !== 'createdAt' && key !== 'updatedAt') {
        payload[key] = clientData[key];
      }
    }
    
    console.log('Payload:', payload);
    
    try {
      const response = await api.put(`/admin/clients/${id}`, payload);
      console.log('Update successful:', response.data);
      return response.data;
    } catch (error) {
      console.error('Update failed:', error);
      throw error;
    }
  },
  
  delete: async (id) => {
    const response = await api.delete(`/admin/clients/${id}`);
    return response.data;
  }
};

// Продукты API
export const productsAPI = {
  getAll: async () => {
    const response = await api.get('/products');
    return response.data;
  },
  
  create: async (productData) => {
    const response = await api.post('/products', productData);
    return response.data;
  },
  
  update: async (id, productData) => {
    const response = await api.put(`/products/${id}`, productData);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await api.delete(`/products/${id}`);
    return response.data;
  },
  
  softDelete: async (id) => {
    const response = await api.delete(`/products/${id}/soft`);
    return response.data;
  }
};

// Курьеры API
export const couriersAPI = {
  getAll: async () => {
    const response = await api.get('/couriers');
    return response.data;
  },
  
  create: async (courierData) => {
    const response = await api.post('/couriers', courierData);
    return response.data;
  },
  
  update: async (id, courierData) => {
    const response = await api.put(`/couriers/${id}`, courierData);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await api.delete(`/couriers/${id}`);
    return response.data;
  }
};

// Доставки API
export const deliveriesAPI = {
  getAll: async () => {
    const response = await api.get('/deliveries');
    return response.data;
  },
  
  create: async (deliveryData) => {
    const response = await api.post('/deliveries', deliveryData);
    return response.data;
  },
  
  update: async (id, deliveryData) => {
    const response = await api.put(`/deliveries/${id}`, deliveryData);
    return response.data;
  },
  
  delete: async (id) => {
    const response = await api.delete(`/deliveries/${id}`);
    return response.data;
  }
};

// Склады API
export const warehouseAPI = {
  getAll: async () => {
    const response = await api.get('/warehouse');
    return response.data;
  },
  
  updateStock: async (id, stockData) => {
    const response = await api.put(`/warehouse/${id}/stock`, stockData);
    return response.data;
  }
};

// Dashboard API для статистики
export const dashboardAPI = {
  getStats: async () => {
    try {
      const response = await api.get('/dashboard/stats');
      return response.data;
    } catch (error) {
      console.error('Dashboard stats error:', error);
      return {
        total: 0,
        active: 0,
        lowStock: 0,
        outOfStock: 0
      };
    }
  },
  
  getRecentActivities: async () => {
    try {
      const response = await api.get('/dashboard/activities');
      return response.data;
    } catch (error) {
      console.error('Dashboard activities error:', error);
      return [];
    }
  }
};

// Users API
export const usersAPI = {
  getCurrentUser: async () => {
    try {
      const response = await api.get('/users/current');
      return response.data;
    } catch (error) {
      console.error('Failed to get current user:', error);
      return null;
    }
  },
  
  updateProfile: async (userData) => {
    const response = await api.put('/users/profile', userData);
    return response.data;
  }
};

// Функция для проверки доступности API
export const checkAPIHealth = async () => {
  try {
    const response = await api.get('/health');
    return response.status === 200;
  } catch (error) {
    console.log('API health check failed:', error.message);
    return false;
  }
};

// Основной экспорт
const apiService = {
  authAPI,
  databaseAPI,
  suppliersAPI,
  categoriesAPI,
  clientsAPI,
  productsAPI,
  couriersAPI,
  deliveriesAPI,
  warehouseAPI,
  dashboardAPI,
  usersAPI,
  checkAPIHealth
};

export default apiService;