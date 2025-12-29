import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { filterClients, sortClients } from '../utils/helpers/clientHelpers';
import { normalizeRole } from '../utils/constants/roles';

export const useClients = () => {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadClients = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const response = await api.clientsAPI.getAll();
      setClients(response);
    } catch (err) {
      console.error('Error loading clients:', err);
      setError('Ошибка при загрузке клиентов');
      setClients([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadClients();
  }, [loadClients]);

  const getFilteredAndSortedClients = useCallback((searchTerm, orderBy, order) => {
    const filtered = filterClients(clients, searchTerm);
    return sortClients(filtered, orderBy, order, normalizeRole);
  }, [clients]);

  return {
    clients,
    loading,
    error,
    loadClients,
    getFilteredAndSortedClients
  };
};