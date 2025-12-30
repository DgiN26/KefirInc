// src/App.js
import React, { useState, useEffect, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import './styles/global.css';

// Layout Components
import MainLayout from './components/layout/MainLayout';
// import OfficeLayout from './components/layout/OfficeLayout';

// Auth Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

// Client Pages
import ClientPortal from './pages/client/ClientPortal';
import ClientCart from './pages/client/ClientCart';
import ClientProfile from './pages/client/ClientProfile';

// Admin Pages
import AdminDashboard from './pages/admin/Dashboard';
import AdminClients from './pages/admin/Clients';
import AdminProducts from './pages/admin/Products';
import AdminCarts from './pages/admin/Carts';
import AdminWarehouse from './pages/admin/Warehouse';
import AdminCouriers from './pages/admin/Couriers';
import AdminDeliveries from './pages/admin/Deliveries';

// Office Pages - ЗАКОММЕНТИРОВАНО
// import OfficePage from './pages/office/OfficePage';
// import OfficeDeliveries from './pages/office/OfficeDeliveries';
// import OfficeOrders from './pages/office/OfficeOrders';
// import OfficeReports from './pages/office/OfficeReports';

// Worker Pages
import CourierApp from './pages/courier/CourierApp';
import CollectorApp from './pages/collector/CollectorApp';

// Utils
import apiService from './services/api';

// Константы ролей
const ROLES = {
  ADMIN: 'admin',
  CLIENT: 'client',
  COURIER: 'courier',
  COLLECTOR: 'collector',
 // OFFICE: 'office',
};

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(null); // null для начальной загрузки
  const [userRole, setUserRole] = useState('');
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true); // Начинаем с true для проверки
  const [error, setError] = useState(null);
  
  // Состояние для работы с PostgreSQL (из локального файла)
  const [dbConnected, setDbConnected] = useState(null);
  const [dbLoading, setDbLoading] = useState(false);
  const [dbError, setDbError] = useState('');

  // Функция для нормализации роли
  const normalizeRole = useCallback((role) => {
    if (!role) return '';
    return String(role).toLowerCase().trim();
  }, []);

  // Проверка подключения к PostgreSQL (ТОЛЬКО ПРИ ЗАПУСКЕ)
  const testDatabaseConnection = useCallback(async () => {
    try {
      setDbLoading(true);
      setDbError('');
      
      // Используем apiService для проверки БД
      const response = await apiService.databaseAPI.testConnection();
      const connected = response?.connected || false;
      
      setDbConnected(connected);
      
      if (!connected) {
        setDbError(response?.error || 'Не удалось подключиться к базе данных');
      }
      
      return connected;
    } catch (err) {
      console.warn('Database connection test failed:', err);
      
      // Проверяем, если это 404 ошибка (эндпоинт не существует)
      if (err.response && err.response.status === 404) {
        setDbError('Эндпоинт проверки БД не найден. Убедитесь, что сервер запущен.');
      } else {
        setDbError('Ошибка при проверке подключения к БД: ' + err.message);
      }
      
      setDbConnected(false);
      return false;
    } finally {
      setDbLoading(false);
    }
  }, []);

  // Очистка данных авторизации
  const clearAuthData = useCallback(() => {
    setIsAuthenticated(false);
    setUserRole('');
    setUserData(null);
    setError(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
  }, []);

  // Проверка авторизации из хранилища и сервера
  const checkAuthFromStorageAndServer = useCallback(async () => {
    // Сначала проверяем sessionStorage
    const sessionToken = sessionStorage.getItem('token');
    const sessionUser = sessionStorage.getItem('user');
    
    // Затем localStorage
    const localToken = localStorage.getItem('token');
    const localUser = localStorage.getItem('user');
    
    // Используем токен из sessionStorage, если есть, иначе из localStorage
    const token = sessionToken || localToken;
    const storedUser = sessionUser || localUser;
    
    if (!token || !storedUser) {
      clearAuthData();
      return false;
    }
    
    try {
      // Парсим данные пользователя
      const parsedUserData = JSON.parse(storedUser);
      const normalizedRole = normalizeRole(parsedUserData.role);
      
      // Проверяем токен на сервере (если есть соответствующий эндпоинт)
      // Если эндпоинта нет, просто используем данные из хранилища
      try {
        // Проверяем, существует ли эндпоинт проверки токена
        const response = await apiService.authAPI.checkAuth(token);
        
        if (response && response.valid) {
          // Обновляем данные пользователя с сервера, если есть
          const userDataFromServer = response.user || parsedUserData;
          
          // Сохраняем обновленные данные
          sessionStorage.setItem('user', JSON.stringify(userDataFromServer));
          if (localToken) {
            localStorage.setItem('user', JSON.stringify(userDataFromServer));
          }
          
          setIsAuthenticated(true);
          setUserData(userDataFromServer);
          setUserRole(normalizeRole(userDataFromServer.role));
          setError(null);
          return true;
        } else {
          // Токен невалиден, очищаем данные
          clearAuthData();
          return false;
        }
      } catch (apiError) {
        // Если эндпоинт проверки не доступен (404 или другая ошибка),
        // используем данные из localStorage как резервный вариант
        console.log('Auth check API not available, using stored data:', apiError.message);
        
        setIsAuthenticated(true);
        setUserData(parsedUserData);
        setUserRole(normalizedRole);
        setError(null);
        return true;
      }
      
    } catch (error) {
      console.error('Error parsing user data:', error);
      clearAuthData();
      return false;
    }
  }, [normalizeRole, clearAuthData]);

  // Инициализация приложения
  useEffect(() => {
    const initializeApp = async () => {
      try {
        setLoading(true);
        
        // Проверяем авторизацию
        const hasAuth = await checkAuthFromStorageAndServer();
        
        // Проверяем подключение к БД только ОДИН РАЗ при запуске
        await testDatabaseConnection();
        
        if (hasAuth) {
          console.log('User authenticated from storage/server');
        }
        
      } catch (error) {
        console.error('App initialization error:', error);
      } finally {
        setLoading(false);
      }
    };

    initializeApp();
  }, [checkAuthFromStorageAndServer, testDatabaseConnection]);

  // Отладка состояния
  useEffect(() => {
    console.log('App state updated:', {
      isAuthenticated,
      userRole,
      loading,
      dbConnected,
      dbLoading,
      dbError,
      hasSessionToken: !!sessionStorage.getItem('token'),
      hasLocalToken: !!localStorage.getItem('token'),
      userData: userData
    });
  }, [isAuthenticated, userRole, loading, dbConnected, dbLoading, dbError, userData]);

  // Тестирование всех эндпоинтов при загрузке (опционально)
  useEffect(() => {
    const testEndpoints = async () => {
      const endpoints = [
        '/api/database/test-connection',
        '/api/auth/check',
        '/api/health',
        '/api/products',
        '/api/clients'
      ];
      
      for (const endpoint of endpoints) {
        try {
          const response = await fetch(`http://localhost:8080${endpoint}`);
          console.log(`${endpoint}: ${response.status}`);
        } catch (error) {
          // Не выводим ошибку в консоль для эндпоинтов, которых может не быть
          if (endpoint !== '/api/auth/check') {
            console.log(`${endpoint}: ${error.message}`);
          }
        }
      }
    };
    
    // Вызываем только если нужно отладить
    // testEndpoints();
  }, []);

  // Обработчик входа - используем apiService
  const handleLogin = async (credentials) => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('Attempting login with:', credentials);
      
      // Используем apiService вместо fetch
      const response = await apiService.authAPI.login(credentials);
      
      // apiService.authAPI.login уже возвращает данные
      const token = response.token || response.accessToken;
      const user = response.user || response;
      
      if (!token || !user) {
        throw new Error('Неверный ответ от сервера при входе');
      }
      
      // Сохраняем данные в sessionStorage (основное) и localStorage (резервное)
      sessionStorage.setItem('token', token);
      sessionStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('token', token); // Резервная копия
      localStorage.setItem('user', JSON.stringify(user)); // Резервная копия
      
      console.log('Login data saved:', { token, user });
      
      const role = normalizeRole(user?.role || '');
      
      setIsAuthenticated(true);
      setUserData(user);
      setUserRole(role);
      
      console.log('Login successful, user authenticated:', { role, user });
      
      return { success: true, user, role };
      
    } catch (error) {
      console.error('Login catch error:', error);
      
      // Более информативное сообщение об ошибке
      let errorMessage = 'Ошибка входа';
      if (error.response) {
        if (error.response.status === 401) {
          errorMessage = 'Неверный email или пароль';
        } else if (error.response.status === 404) {
          errorMessage = 'Сервер авторизации не найден. Проверьте подключение к серверу.';
        } else {
          errorMessage = `Ошибка сервера: ${error.response.status}`;
        }
      } else if (error.request) {
        errorMessage = 'Не удалось подключиться к серверу. Проверьте сеть.';
      } else {
        errorMessage = error.message || 'Ошибка входа';
      }
      
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Обработчик выхода - используем apiService
  const handleLogout = useCallback(async () => {
    console.log('Logging out...');
    
    // Вызываем logout API через apiService, если доступен
    try {
      await apiService.authAPI.logout();
    } catch (err) {
      console.log('Logout API error (ignoring):', err.message);
      // Продолжаем даже если API недоступно
    } finally {
      clearAuthData();
      // Перенаправляем на страницу логина
      window.location.href = '/login';
    }
  }, [clearAuthData]);

  // Обработчик регистрации - используем apiService
  const handleRegister = async (userData) => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('Registering user:', userData);
      
      // Используем apiService вместо fetch
      const response = await apiService.authAPI.register(userData);
      
      console.log('Register API response:', response);
      
      // Проверяем успешность регистрации
      if (response.success !== false && !response.error) {
        // После успешной регистрации предлагаем залогиниться
        // Не логиним автоматически
        alert('Регистрация успешна! Теперь вы можете войти в систему.');
        return { success: true, message: 'Регистрация успешна', data: response };
      } else {
        throw new Error(response.error || response.message || 'Ошибка регистрации');
      }
      
    } catch (error) {
      console.error('Registration failed:', error);
      
      // Более информативное сообщение об ошибке
      let errorMessage = 'Ошибка регистрации';
      if (error.response) {
        if (error.response.status === 400) {
          errorMessage = 'Некорректные данные регистрации';
        } else if (error.response.status === 409) {
          errorMessage = 'Пользователь с таким email уже существует';
        } else if (error.response.status === 404) {
          errorMessage = 'Сервер регистрации не найден';
        }
      }
      
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Функция для получения редиректа по роли
  const getRoleRedirect = useCallback((role) => {
    if (!role) return '/login';
    
    const normalizedRole = normalizeRole(role);
    
    switch(normalizedRole) {
      case ROLES.ADMIN:
        return '/admin';
      case ROLES.COURIER:
        return '/courier';
      case ROLES.COLLECTOR:
        return '/collector';
      //case ROLES.OFFICE:
        //return '/client'; // Изменено: вместо офиса редирект на клиента
      case ROLES.CLIENT:
        return '/client';
      default:
        return '/login';
    }
  }, [normalizeRole]);

  // Защищенный роут компонент с проверкой БД
  const ProtectedRoute = ({ children, allowedRoles = [], requireDB = false }) => {
    console.log('ProtectedRoute render:', {
      isAuthenticated,
      userRole,
      loading,
      allowedRoles,
      requireDB,
      dbConnected,
      dbLoading
    });

    if (isAuthenticated === null || loading) {
      return (
        <div className="flex-center" style={{ 
          height: '100vh',
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
        }}>
          <div className="text-center text-white">
            <div style={{
              width: '50px',
              height: '50px',
              margin: '0 auto 20px',
              border: '3px solid rgba(255, 255, 255, 0.3)',
              borderTop: '3px solid white',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
            <div>Загрузка...</div>
            {dbLoading && <div>Проверка подключения к базе данных...</div>}
          </div>
        </div>
      );
    }

    if (!isAuthenticated) {
      console.log('ProtectedRoute: Not authenticated, redirecting to login');
      return <Navigate to="/login" replace />;
    }

    // Проверка доступности БД если требуется
    if (requireDB && dbConnected === false && !dbLoading) {
      return (
        <MainLayout 
          userRole={userRole}
          userData={userData}
          onLogout={handleLogout}
          dbConnected={dbConnected}
        >
          <div className="container mt-4">
            <div className="alert alert-danger">
              <h4 className="alert-heading">⚠️ База данных недоступна</h4>
              <p>Для доступа к этой странице требуется подключение к PostgreSQL.</p>
              <p>Пожалуйста, проверьте подключение к базе данных и повторите попытку.</p>
              <hr />
              <p className="mb-0">
                <button 
                  className="btn btn-sm btn-outline-primary"
                  onClick={testDatabaseConnection}
                  disabled={dbLoading}
                >
                  {dbLoading ? 'Проверка...' : 'Проверить подключение'}
                </button>
              </p>
            </div>
          </div>
        </MainLayout>
      );
    }

    const currentUserRole = userRole || '';
    const normalizedUserRole = normalizeRole(currentUserRole);
    const normalizedAllowedRoles = allowedRoles.map(role => 
      normalizeRole(role)
    );

    console.log('Checking role access:', {
      normalizedUserRole,
      normalizedAllowedRoles,
      hasAccess: normalizedAllowedRoles.length === 0 || normalizedAllowedRoles.includes(normalizedUserRole)
    });

    if (normalizedAllowedRoles.length > 0 && !normalizedAllowedRoles.includes(normalizedUserRole)) {
      const redirectPath = getRoleRedirect(normalizedUserRole);
      console.log(`ProtectedRoute: Role ${normalizedUserRole} not allowed, redirecting to ${redirectPath}`);
      return <Navigate to={redirectPath} replace />;
    }

    console.log('Access granted');
    return (
      <MainLayout 
        userRole={normalizedUserRole}
        userData={userData}
        onLogout={handleLogout}
        dbConnected={dbConnected}
        dbError={dbError}
      >
        {children}
      </MainLayout>
    );
  };

  // Защищенный роут для Office (с OfficeLayout вместо MainLayout) - ЗАКОММЕНТИРОВАНО
  /*
  const ProtectedOfficeRoute = ({ children, allowedRoles = [ROLES.OFFICE] }) => {
    if (isAuthenticated === null || loading) {
      return (
        <div className="flex-center" style={{ 
          height: '100vh',
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
        }}>
          <div className="text-center text-white">
            <div style={{
              width: '50px',
              height: '50px',
              margin: '0 auto 20px',
              border: '3px solid rgba(255, 255, 255, 0.3)',
              borderTop: '3px solid white',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
            <div>Загрузка...</div>
          </div>
        </div>
      );
    }

    if (!isAuthenticated) {
      console.log('ProtectedOfficeRoute: Not authenticated, redirecting to login');
      return <Navigate to="/login" replace />;
    }

    const currentUserRole = userRole || '';
    const normalizedUserRole = normalizeRole(currentUserRole);
    const normalizedAllowedRoles = allowedRoles.map(role => 
      normalizeRole(role)
    );

    if (normalizedAllowedRoles.length > 0 && !normalizedAllowedRoles.includes(normalizedUserRole)) {
      const redirectPath = getRoleRedirect(normalizedUserRole);
      console.log(`ProtectedOfficeRoute: Role ${normalizedUserRole} not allowed, redirecting to ${redirectPath}`);
      return <Navigate to={redirectPath} replace />;
    }

    return (
      <OfficeLayout 
        userRole={normalizedUserRole}
        userData={userData}
        onLogout={handleLogout}
      >
        {children}
      </OfficeLayout>
    );
  };
  */

  // Показываем лоадер при начальной загрузке
  if (loading && isAuthenticated === null) {
    return (
      <div className="flex-center" style={{ 
        height: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
      }}>
        <div className="text-center text-white">
          <div style={{
            width: '50px',
            height: '50px',
            margin: '0 auto 20px',
            border: '3px solid rgba(255, 255, 255, 0.3)',
            borderTop: '3px solid white',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
          }} />
          <div>Загрузка приложения...</div>
          <div>Проверка подключения к базе данных...</div>
        </div>
        <style>{`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  // Компонент для отображения статуса БД
  const DatabaseStatusAlert = () => {
    if (dbConnected !== false || dbLoading) return null;
    
    return (
      <div className="alert alert-warning alert-dismissible fade show mb-0" role="alert">
        <strong>⚠️ База данных недоступна:</strong> {dbError || 'Не удалось подключиться к PostgreSQL'}
        <button 
          type="button" 
          className="btn-close" 
          onClick={() => setDbError('')}
        ></button>
        <div className="mt-2">
          <button 
            className="btn btn-sm btn-outline-primary"
            onClick={testDatabaseConnection}
            disabled={dbLoading}
          >
            {dbLoading ? 'Проверка...' : 'Повторить попытку'}
          </button>
        </div>
      </div>
    );
  };

  // Компонент для отображения ошибок
  const ErrorBanner = () => {
    if (!error) return null;
    
    return (
      <div className="error-banner" style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        backgroundColor: '#f44336',
        color: 'white',
        padding: '10px 20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        zIndex: 1000
      }}>
        <span>❌ {error}</span>
        <button 
          onClick={() => setError(null)} 
          style={{
            background: 'none',
            border: 'none',
            color: 'white',
            fontSize: '20px',
            cursor: 'pointer'
          }}
        >
          ×
        </button>
      </div>
    );
  };

  return (
    <Router>
      <div className="App">
        {/* Статус подключения к БД */}
        <DatabaseStatusAlert />
        
        <ErrorBanner />
        
        <Routes>
          {/* Public Routes - доступны без авторизации */}
          <Route path="/login" element={
            isAuthenticated ? (
              <Navigate to={getRoleRedirect(userRole)} replace />
            ) : (
              <Login 
                onLogin={handleLogin} 
                loading={loading}
                dbConnected={dbConnected}
                dbError={dbError}
              />
            )
          } />
          
          <Route path="/register" element={
            isAuthenticated ? (
              <Navigate to={getRoleRedirect(userRole)} replace />
            ) : (
              <Register 
                onRegister={handleRegister}
                loading={loading}
                error={error}
              />
            )
          } />
          
          {/* Client Routes */}
          <Route path="/client" element={
            <ProtectedRoute allowedRoles={[ROLES.CLIENT]}>
              <ClientPortal />
            </ProtectedRoute>
          } />
          
          <Route path="/client/cart" element={
            <ProtectedRoute allowedRoles={[ROLES.CLIENT]}>
              <ClientCart />
            </ProtectedRoute>
          } />
          
          <Route path="/client/profile" element={
            <ProtectedRoute allowedRoles={[ROLES.CLIENT]}>
              <ClientProfile />
            </ProtectedRoute>
          } />
          
          {/* Admin Routes */}
          <Route path="/admin" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminDashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/clients" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminClients />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/products" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminProducts />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/carts" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminCarts />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/warehouse" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminWarehouse />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/couriers" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminCouriers />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/deliveries" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]} requireDB={true}>
              <AdminDeliveries />
            </ProtectedRoute>
          } />
          
          {/* Office Routes - ЗАКОММЕНТИРОВАНО */}
          {/*
          <Route path="/office" element={
            <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
              <OfficePage />
            </ProtectedOfficeRoute>
          } />
          
          <Route path="/office/deliveries" element={
            <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
              <OfficeDeliveries />
            </ProtectedOfficeRoute>
          } />
          
          <Route path="/office/orders" element={
            <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
              <OfficeOrders />
            </ProtectedOfficeRoute>
          } />
          
          <Route path="/office/reports" element={
            <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
              <OfficeReports />
            </ProtectedOfficeRoute>
          } />
          */}
          
          {/* Worker Routes */}
          <Route path="/courier" element={
            <ProtectedRoute allowedRoles={[ROLES.COURIER]}>
              <CourierApp />
            </ProtectedRoute>
          } />
          
          <Route path="/collector" element={
            <ProtectedRoute allowedRoles={[ROLES.COLLECTOR]}>
              <CollectorApp />
            </ProtectedRoute>
          } />
          
          {/* Default Route - всегда на логин */}
          <Route path="/" element={
            <Navigate to="/login" replace />
          } />

          {/* 404 Route */}
          <Route path="*" element={
            <Navigate to="/login" replace />
          } />
        </Routes>
      </div>
    </Router>
  );
}

export default App;