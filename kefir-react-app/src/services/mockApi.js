// src/services/mockApi.js
// Моковые данные для разработки
const mockClients = [
  { id: 1, username: 'ivanov', email: 'ivanov@mail.com', city: 'Москва', status: 'ACTIVE' },
  { id: 2, username: 'petrov', email: 'petrov@mail.com', city: 'Санкт-Петербург', status: 'ACTIVE' },
  { id: 3, username: 'sidorov', email: 'sidorov@mail.com', city: 'Казань', status: 'INACTIVE' },
  { id: 4, username: 'smirnov', email: 'smirnov@mail.com', city: 'Новосибирск', status: 'ACTIVE' },
];

const mockProducts = [
  { id: 1, name: 'Ноутбук Lenovo', price: 45990, count: 10, category: 'Электроника' },
  { id: 2, name: 'Мышь беспроводная', price: 1490, count: 25, category: 'Электроника' },
  { id: 3, name: 'Клавиатура механическая', price: 5490, count: 15, category: 'Электроника' },
  { id: 4, name: 'Футболка мужская', price: 1290, count: 50, category: 'Одежда' },
];



// МОКОВЫЕ API методы (для разработки)
export const mockAuthAPI = {
  login: async (credentials) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const demoUsers = [
          { username: 'admin', password: 'admin', role: 'admin', name: 'Администратор', id: 1 },
          { username: 'client', password: 'client', role: 'client', name: 'Иван Иванов', id: 2 },
          { username: 'courier', password: 'courier', role: 'courier', name: 'Курьер Петр', id: 3 },
          { username: 'collector', password: 'collector', role: 'collector', name: 'Сборщик Алексей', id: 4 },
        ];
        
        const user = demoUsers.find(u => 
          u.username === credentials.username && u.password === credentials.password
        );
        
        if (user) {
          resolve({
            data: {
              token: 'demo-token-' + Date.now(),
              user: user
            }
          });
        } else {
          reject(new Error('Неверный логин или пароль'));
        }
      }, 500);
    });
  },
  
  logout: () => {
    return Promise.resolve({ data: { success: true } });
  }
};

export const mockClientsAPI = {
  getAll: async () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          data: mockClients
        });
      }, 500);
    });
  },
  
  getById: async (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const client = mockClients.find(c => c.id === id);
        resolve({
          data: client || null
        });
      }, 300);
    });
  },
  
  create: async (client) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const newClient = {
          ...client,
          id: mockClients.length + 1,
          status: client.status || 'ACTIVE'
        };
        mockClients.push(newClient);
        resolve({
          data: newClient
        });
      }, 500);
    });
  },
  
  update: async (id, clientData) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockClients.findIndex(c => c.id === id);
        if (index !== -1) {
          mockClients[index] = { ...mockClients[index], ...clientData };
          resolve({
            data: mockClients[index]
          });
        } else {
          reject(new Error('Клиент не найден'));
        }
      }, 500);
    });
  },
  
  delete: async (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const index = mockClients.findIndex(c => c.id === id);
        if (index !== -1) {
          mockClients.splice(index, 1);
        }
        resolve({
          data: { success: true }
        });
      }, 500);
    });
  }
};

export const mockProductsAPI = {
  getAll: async () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          data: mockProducts
        });
      }, 500);
    });
  },
  
  getById: async (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const product = mockProducts.find(p => p.id === id);
        resolve({
          data: product || null
        });
      }, 300);
    });
  },
  
  create: async (product) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const newProduct = {
          ...product,
          id: mockProducts.length + 1
        };
        mockProducts.push(newProduct);
        resolve({
          data: newProduct
        });
      }, 500);
    });
  },
  
  update: async (id, productData) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockProducts.findIndex(p => p.id === id);
        if (index !== -1) {
          mockProducts[index] = { ...mockProducts[index], ...productData };
          resolve({
            data: mockProducts[index]
          });
        } else {
          reject(new Error('Товар не найден'));
        }
      }, 500);
    });
  },
  
  delete: async (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const index = mockProducts.findIndex(p => p.id === id);
        if (index !== -1) {
          mockProducts.splice(index, 1);
        }
        resolve({
          data: { success: true }
        });
      }, 500);
    });
  }
};

export const mockOrdersAPI = {
  getAll: () => Promise.resolve({ data: [] }),
  create: () => Promise.resolve({ data: {} })
};

export const mockCartAPI = {
  getCart: () => Promise.resolve({ data: { items: [] } }),
  addToCart: () => Promise.resolve({ data: {} }),
  clearCart: () => Promise.resolve({ data: { success: true } })
};

export const mockDeliveriesAPI = {
  getAll: () => Promise.resolve({ data: [] }),
  create: () => Promise.resolve({ data: {} })
};

export const mockCouriersAPI = {
  getAll: () => Promise.resolve({ 
    data: [
      { id: 1, name: 'Петр Курьеров', phone: '+7 (900) 123-45-67', status: 'ACTIVE', vehicle: 'Велосипед' },
      { id: 2, name: 'Иван Доставкин', phone: '+7 (900) 765-43-21', status: 'BUSY', vehicle: 'Автомобиль' }
    ] 
  })
};

export const mockCollectorsAPI = {
  getAll: () => Promise.resolve({ 
    data: [
      { id: 1, name: 'Алексей Сборщиков', location: 'Склад A', status: 'ACTIVE' },
      { id: 2, name: 'Сергей Упаковкин', location: 'Склад B', status: 'BUSY' }
    ] 
  })
};

export const mockWarehouseAPI = {
  getInventory: () => Promise.resolve({ data: [] })
};

export const mockOfficeAPI = {
  getStats: () => Promise.resolve({ 
    data: { 
      users: 42, 
      orders: 156, 
      revenue: 125000,
      activeClients: 28,
      deliveredOrders: 120 
    } 
  }),
  
  getSystemHealth: () => Promise.resolve({ 
    data: { 
      status: 'OK', 
      uptime: '99.9%',
      database: 'connected',
      services: 'all_running',
      lastCheck: new Date().toISOString()
    } 
  }),
  
  generateReport: (type) => Promise.resolve({ 
    data: { 
      reportId: Math.floor(Math.random() * 1000), 
      status: 'generated',
      type: type || 'daily',
      downloadUrl: '/reports/report-001.pdf',
      generatedAt: new Date().toISOString()
    } 
  }),
  
  getFinancialStats: (period) => Promise.resolve({ 
    data: { 
      period: period || 'monthly',
      profit: 50000,
      expenses: 20000,
      revenue: 125000,
      taxes: 15000,
      netIncome: 60000
    } 
  }),
};
