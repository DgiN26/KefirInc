// src/utils/api.js
const API_BASE_URL = 'http://localhost:8080/api';

export const api = {
  async get(endpoint) {
    const response = await fetch(`${API_BASE_URL}/${endpoint}`);
    return response.json();
  },

  async post(endpoint, data) {
    const response = await fetch(`${API_BASE_URL}/${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return response.json();
  },

  async put(endpoint, data) {
    const response = await fetch(`${API_BASE_URL}/${endpoint}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return response.json();
  },

  async delete(endpoint) {
    const response = await fetch(`${API_BASE_URL}/${endpoint}`, {
      method: 'DELETE',
    });
    return response.json();
  },
  
  // Добавьте методы для удобства
  authAPI: {
    login: async (credentials) => {
      return api.post('auth/login', credentials);
    },
    
    register: async (userData) => {
      // ИСПРАВЬТЕ ЭТОТ ПУТЬ!
      return api.post('clients/register', userData); // ПРАВИЛЬНЫЙ ПУТЬ!
    },
    
    logout: async () => {
      return api.post('auth/logout', {});
    }
  }
};