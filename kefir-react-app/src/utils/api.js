// src/utils/api.js
const API_BASE_URL = 'http://localhost:8080'; // УБРАЛИ /api!

// Утилита для обработки ошибок
const handleResponse = async (response) => {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `HTTP ${response.status}`);
  }
  return response.json();
};

export const api = {
  async get(endpoint, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, { // УБРАЛИ / перед endpoint
      ...options,
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
    return handleResponse(response);
  },

  async post(endpoint, data, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, { // УБРАЛИ / перед endpoint
      ...options,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  async put(endpoint, data, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, { // УБРАЛИ / перед endpoint
      ...options,
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  async delete(endpoint, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, { // УБРАЛИ / перед endpoint
      ...options,
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
    return handleResponse(response);
  },
  
  // Методы для авторизации
  authAPI: {
    login: async (credentials) => {
      return api.post('/api/auth/login', credentials); // ДОБАВИЛИ /api/ в начало!
    },
    
    register: async (userData) => {
      return api.post('/api/clients/register', userData); // ДОБАВИЛИ /api/ в начало!
    },
    
    logout: async (token) => {
      return api.post('/api/auth/logout', {}, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    },
    
    validate: async (token) => {
      return api.post('/api/auth/validate', { token });
    }
  },
  
  // Методы для работы с базой данных
  databaseAPI: {
    testConnection: async () => {
      try {
        return await api.get('/api/database/test-connection'); // ДОБАВИЛИ /api/ в начало!
      } catch (error) {
        console.warn('Database connection test failed:', error);
        return {
          connected: false,
          message: 'Failed to connect to database',
          error: error.message
        };
      }
    },
    
    getStats: async () => {
      return api.get('/api/database/stats'); // ДОБАВИЛИ /api/ в начало!
    }
  },
  
  // Методы для клиентов
  clientsAPI: {
    getAll: async () => {
      return api.get('/api/clients'); // ДОБАВИЛИ /api/ в начало!
    },
    
    getById: async (id) => {
      return api.get(`/api/clients/${id}`); // ДОБАВИЛИ /api/ в начало!
    },
    
    getProfile: async (id) => {
      return api.get(`/api/clients/${id}/profile`); // ДОБАВИЛИ /api/ в начало!
    },
    
    update: async (id, data) => {
      return api.put(`/api/clients/${id}`, data); // ДОБАВИЛИ /api/ в начало!
    },
    
    delete: async (id) => {
      return api.delete(`/api/clients/${id}`); // ДОБАВИЛИ /api/ в начало!
    }
  },
  
  // Методы для продуктов
  productsAPI: {
    getAll: async () => {
      return api.get('/api/products'); // ДОБАВИЛИ /api/ в начало!
    },
    
    getById: async (id) => {
      return api.get(`/api/products/${id}`); // ДОБАВИЛИ /api/ в начало!
    },
    
    create: async (data) => {
      return api.post('/api/products', data); // ДОБАВИЛИ /api/ в начало!
    },
    
    update: async (id, data) => {
      return api.put(`/api/products/${id}`, data); // ДОБАВИЛИ /api/ в начало!
    },
    
    delete: async (id) => {
      return api.delete(`/api/products/${id}`); // ДОБАВИЛИ /api/ в начало!
    },
    
    search: async (query) => {
      return api.get(`/api/products/search?query=${encodeURIComponent(query)}`);
    },
    
    getByCategory: async (category) => {
      return api.get(`/api/products/category/${encodeURIComponent(category)}`);
    }
  },
  
  // Методы для корзин
  cartAPI: {
    create: async (clientId) => {
      return api.post(`/api/clients/${clientId}/cart`, {});
    },
    
    addItem: async (cartId, productId, quantity, price) => {
      return api.post(`/api/cart/${cartId}/add`, {
        productId,
        quantity,
        price
      });
    },
    
    getClientCarts: async (clientId) => {
      return api.get(`/api/cart/client/${clientId}`);
    },
    
    getClientCartsFull: async (clientId) => {
      return api.get(`/api/cart/client/${clientId}/full`);
    },
    
    checkout: async (cartId) => {
      return api.post(`/api/cart/${cartId}/checkout`, {});
    }
  },
  
  // Методы для заказов
  ordersAPI: {
    create: async (orderData, token) => {
      return api.post('/api/orders', orderData, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    },
    
    getAll: async () => {
      return api.get('/api/orders');
    },
    
    getById: async (orderId) => {
      return api.get(`/api/orders/${orderId}`);
    },
    
    cancel: async (orderId) => {
      return api.post(`/api/orders/${orderId}/cancel`, {});
    },
    
    getMyOrders: async (token) => {
      return api.get('/api/cart/my-orders', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    }
  },
  
  // Методы для office
  officeAPI: {
    getActiveProblems: async () => {
      return api.get('/api/office/problems/active');
    },
    
    notifyClient: async (data) => {
      return api.post('/api/office/notify-client', data);
    },
    
    makeDecision: async (data) => {
      return api.post('/api/office/make-decision', data);
    },
    
    getOrderInfo: async (orderId) => {
      return api.get(`/api/office/order/${orderId}/full-info`);
    },
    
    test: async () => {
      return api.get('/api/office/test');
    },
    
    debugDatabase: async () => {
      return api.get('/api/office/debug/database');
    }
  },
  
  // Методы для доставки
  deliveryAPI: {
    create: async (data) => {
      return api.post('/api/deliveries', data);
    },
    
    getClientDeliveries: async (clientId) => {
      return api.get(`/api/deliveries/client/${clientId}`);
    },
    
    getActive: async () => {
      return api.get('/api/deliveries/active');
    }
  },
  
  // Методы для сборщиков
  collectorAPI: {
    getAll: async () => {
      return api.get('/api/collector/collectors');
    },
    
    getTasks: async () => {
      return api.get('/api/collector/tasks');
    },
    
    getCollectorTasks: async (collectorId) => {
      return api.get(`/api/collector/tasks/collector/${collectorId}`);
    }
  },
  
  // Утилиты для работы с токенами
  utils: {
    saveToken: (token) => {
      if (token) {
        localStorage.setItem('token', token);
        sessionStorage.setItem('token', token);
      }
    },
    
    getToken: () => {
      return localStorage.getItem('token') || sessionStorage.getItem('token');
    },
    
    removeToken: () => {
      localStorage.removeItem('token');
      sessionStorage.removeItem('token');
    },
    
    isAuthenticated: () => {
      return !!api.utils.getToken();
    },
    
    getUserInfo: () => {
      const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    },
    
    saveUserInfo: (user) => {
      if (user) {
        localStorage.setItem('user', JSON.stringify(user));
        sessionStorage.setItem('user', JSON.stringify(user));
      }
    },
    
    removeUserInfo: () => {
      localStorage.removeItem('user');
      sessionStorage.removeItem('user');
    },
    
    logout: () => {
      api.utils.removeToken();
      api.utils.removeUserInfo();
    }
  }
};

export default api;