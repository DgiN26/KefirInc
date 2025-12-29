// src/hooks/useDatabase.js
import { useState, useCallback } from 'react';
import api from '../services/api';

export const useDatabase = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState('');

  // Проверка подключения к PostgreSQL
  const testConnection = useCallback(async () => {
    try {
      const response = await api.databaseAPI.testConnection();
      setIsConnected(response.connected || false);
      setConnectionError(response.error || '');
      return response.connected;
    } catch (error) {
      console.error('Database connection test failed:', error);
      setIsConnected(false);
      setConnectionError('Не удалось подключиться к базе данных');
      return false;
    }
  }, []);

  // Выполнение SQL запроса (только для чтения)
  const executeQuery = useCallback(async (query) => {
    try {
      const response = await api.databaseAPI.executeQuery({ query });
      return response;
    } catch (error) {
      console.error('Query execution failed:', error);
      throw error;
    }
  }, []);

  // Получение статистики базы данных
  const getDatabaseStats = useCallback(async () => {
    try {
      const response = await api.databaseAPI.getStats();
      return response;
    } catch (error) {
      console.error('Failed to get database stats:', error);
      throw error;
    }
  }, []);

  return {
    isConnected,
    connectionError,
    testConnection,
    executeQuery,
    getDatabaseStats
  };
};