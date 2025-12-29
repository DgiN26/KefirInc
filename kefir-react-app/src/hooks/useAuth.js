import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

export const useAuth = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [userRole, setUserRole] = useState('');
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);

  const normalizeRole = useCallback((role) => {
    if (!role) return '';
    return String(role).toUpperCase().trim();
  }, []);

  const login = useCallback(async (credentials) => {
    try {
      setLoading(true);
      const response = await api.authAPI.login(credentials);
      
      localStorage.setItem('authToken', response.token);
      localStorage.setItem('userData', JSON.stringify(response.user));
      localStorage.setItem('userRole', normalizeRole(response.user.role));
      
      setIsAuthenticated(true);
      setUserData(response.user);
      setUserRole(normalizeRole(response.user.role));
      
      return response;
    } catch (error) {
      throw error;
    } finally {
      setLoading(false);
    }
  }, [normalizeRole]);

  const register = useCallback(async (userData) => {
    try {
      setLoading(true);
      const response = await api.authAPI.register(userData);
      return response;
    } catch (error) {
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('authToken');
      localStorage.removeItem('userData');
      localStorage.removeItem('userRole');
      setIsAuthenticated(false);
      setUserRole('');
      setUserData(null);
    }
  }, []);

  useEffect(() => {
    const checkAuth = async () => {
      const token = localStorage.getItem('authToken');
      const storedUserData = localStorage.getItem('userData');
      
      if (token && storedUserData) {
        try {
          const parsedUserData = JSON.parse(storedUserData);
          setIsAuthenticated(true);
          setUserData(parsedUserData);
          setUserRole(normalizeRole(parsedUserData.role));
        } catch (error) {
          setIsAuthenticated(false);
        }
      } else {
        setIsAuthenticated(false);
      }
      setLoading(false);
    };

    checkAuth();
  }, [normalizeRole]);

  return {
    isAuthenticated,
    userRole,
    userData,
    loading,
    login,
    logout,
    register,
  };
};