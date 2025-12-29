import api from '../../services/api';
import { normalizeRole } from '../constants/roles';

/**
 * Создает полный объект User для отправки на сервер
 */
export const createFullUserObject = (clientData, updates = {}) => {
  const baseUser = {
    username: clientData.username || `user_${clientData.id}`,
    email: clientData.email || `client${clientData.id}@example.com`,
    name: clientData.name || clientData.firstname || `Client ${clientData.id}`,
    firstname: clientData.firstname || clientData.name || `Client ${clientData.id}`,
    city: clientData.city || '',
    role: normalizeRole(clientData.role || 'CLIENT'),
    status: clientData.status || 'active',
    // Добавляем переданные обновления
    ...updates
  };

  // Удаляем системные поля
  const systemFields = ['id', 'createdAt', 'updatedAt', 'password'];
  systemFields.forEach(field => {
    if (field in baseUser && updates[field] === undefined) {
      delete baseUser[field];
    }
  });

  return baseUser;
};

/**
 * Обновляет клиента на сервере
 */
export const updateClientWithFullData = async (clientId, updates) => {
  try {
    // Получаем текущие данные
    const currentData = await api.clientsAPI.getById(clientId);
    
    // Создаем полный объект
    const fullData = createFullUserObject(currentData, updates);
    
    // Отправляем обновление
    const response = await api.clientsAPI.update(clientId, fullData);
    return response;
  } catch (error) {
    console.error('Error updating client:', error);
    throw error;
  }
};

/**
 * Обновляет одно поле клиента
 */
export const updateClientField = async (clientId, field, value) => {
  return updateClientWithFullData(clientId, { [field]: value });
};

/**
 * Фильтрация клиентов
 */
export const filterClients = (clients, searchTerm) => {
  if (!searchTerm.trim()) return clients;
  
  const term = searchTerm.toLowerCase();
  return clients.filter(client =>
    client.username?.toLowerCase().includes(term) ||
    client.email?.toLowerCase().includes(term) ||
    client.firstname?.toLowerCase().includes(term) ||
    client.city?.toLowerCase().includes(term) ||
    (client.role && client.role.toLowerCase().includes(term))
  );
};

/**
 * Сортировка клиентов
 */
export const sortClients = (clients, orderBy, order, normalizeRole) => {
  return [...clients].sort((a, b) => {
    let aValue = a[orderBy];
    let bValue = b[orderBy];
    
    if (orderBy === 'role') {
      aValue = normalizeRole(aValue);
      bValue = normalizeRole(bValue);
    }
    
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      aValue = aValue.toLowerCase();
      bValue = bValue.toLowerCase();
    }
    
    if (aValue == null && bValue == null) return 0;
    if (aValue == null) return 1;
    if (bValue == null) return -1;
    
    if (aValue < bValue) return order === 'asc' ? -1 : 1;
    if (aValue > bValue) return order === 'asc' ? 1 : -1;
    return 0;
  });
};

/**
 * Валидация данных клиента
 */
export const validateClientData = (data, isCreate = false) => {
  const errors = {};
  
  if (!data.username?.trim()) {
    errors.username = 'Имя пользователя обязательно';
  }
  
  if (!data.email?.trim()) {
    errors.email = 'Email обязателен';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
    errors.email = 'Некорректный email';
  }
  
  if (isCreate && !data.password?.trim()) {
    errors.password = 'Пароль обязателен для нового клиента';
  } else if (data.password && data.password.length < 6) {
    errors.password = 'Пароль должен быть не менее 6 символов';
  }
  
  return errors;
};

/**
 * Подготовка данных клиента для отправки
 */
export const prepareClientData = (formData, isCreate = false) => {
  const data = {
    username: formData.username.trim(),
    email: formData.email.trim(),
    name: formData.firstname?.trim() || formData.username.trim(),
    firstname: formData.firstname?.trim() || formData.username.trim(),
    city: formData.city?.trim() || '',
    role: formData.role || 'CLIENT',
    status: formData.status || 'active'
  };
  
  // Пароль только если указан
  if (formData.password?.trim()) {
    data.password = formData.password.trim();
  }
  
  return data;
};