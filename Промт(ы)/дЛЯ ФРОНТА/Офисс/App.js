// src/App.js
import React, { useState, useEffect, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import './styles/global.css';

// Layout Components
import MainLayout from './components/layout/MainLayout';
import OfficeLayout from './components/layout/OfficeLayout';
import CollectorLayout from './components/layout/CollectorLayout';

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

// Office Pages
import OfficePage from './pages/office/OfficePage';
import OfficeDeliveries from './pages/office/OfficeDeliveries';
import OfficeOrders from './pages/office/OfficeOrders';
import OfficeReports from './pages/office/OfficeReports';

// Worker Pages
import CourierApp from './pages/courier/CourierApp';
import CollectorApp from './pages/collector/CollectorApp';

// Utils
import api from './services/api';

// Константы ролей
const ROLES = {
  ADMIN: 'admin',
  CLIENT: 'client',
  COURIER: 'courier',
  COLLECTOR: 'collector',
  OFFICE: 'office',
};

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(null);
  const [userRole, setUserRole] = useState('');
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Состояние для работы с PostgreSQL
  const [dbConnected, setDbConnected] = useState(null);
  const [dbLoading, setDbLoading] = useState(false);
  const [dbError, setDbError] = useState('');

  // Флаг, что приложение проинициализировано
  const [initialized, setInitialized] = useState(false);

  // Функция для нормализации роли
  const normalizeRole = useCallback((role) => {
    if (!role) return '';
    return String(role).toLowerCase().trim();
  }, []);

  // Проверка подключения к PostgreSQL
  const testDatabaseConnection = useCallback(async () => {
    try {
      setDbLoading(true);
      setDbError('');
      
      const response = await api.databaseAPI?.testConnection?.();
      const connected = response?.connected || false;
      
      setDbConnected(connected);
      
      if (!connected) {
        setDbError(response?.error || 'Не удалось подключиться к базе данных');
      }
      
      return connected;
    } catch (err) {
      console.warn('Database connection test failed:', err);
      setDbConnected(false);
      setDbError('Ошибка при проверке подключения к БД');
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
    sessionStorage.removeItem('redirectAfterLogin');
  }, []);

  // Проверка авторизации по localStorage
  const checkAuthFromStorage = useCallback(() => {
    const token = localStorage.getItem('token');
    const storedUserData = localStorage.getItem('user');
    
    if (token && storedUserData) {
      try {
        const parsedUserData = JSON.parse(storedUserData);
        const normalizedRole = normalizeRole(parsedUserData.role);
        
        setIsAuthenticated(true);
        setUserData(parsedUserData);
        setUserRole(normalizedRole);
        setError(null);
        
        return true;
      } catch (error) {
        console.error('Error parsing user data:', error);
        clearAuthData();
        return false;
      }
    } else {
      setIsAuthenticated(false);
      return false;
    }
  }, [normalizeRole, clearAuthData]);

  // Эффект для принудительного редиректа на логин при старте
  useEffect(() => {
    // Сохраняем текущий путь для возможного редиректа после логина
    const currentPath = window.location.pathname;
    if (!['/login', '/register'].includes(currentPath)) {
      sessionStorage.setItem('redirectAfterLogin', currentPath);
    }
    
    // Принудительно устанавливаем isAuthenticated в false при старте
    // чтобы всегда показывать логин первым
    if (isAuthenticated === null) {
      const token = localStorage.getItem('token');
      if (token) {
        // Если есть токен, проверяем его, но не редиректим автоматически
        checkAuthFromStorage();
      } else {
        setIsAuthenticated(false);
      }
    }
  }, []);

  // Инициализация приложения
  useEffect(() => {
    const initializeApp = async () => {
      try {
        setLoading(true);
        
        console.log('App initialization started');
        
        // Проверяем подключение к БД
        await testDatabaseConnection();
        
        // Помечаем инициализацию как завершенную
        setInitialized(true);
        
      } catch (error) {
        console.error('App initialization error:', error);
      } finally {
        setLoading(false);
      }
    };

    // Небольшая задержка перед инициализацией для UX
    const timer = setTimeout(() => {
      initializeApp();
    }, 500);

    return () => clearTimeout(timer);
  }, [testDatabaseConnection]);

  // Обработчик входа
  const handleLogin = async (credentials) => {
    try {
      setLoading(true);
      setError(null);
      
      // Предупреждение если БД не подключена
      if (dbConnected === false) {
        console.warn('Database is not connected. Login may fail.');
      }

      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials)
      });
      
      const data = await response.json();
      
      if (response.ok) {
        const responseData = data.body || data;
        const token = responseData.token || responseData.accessToken;
        const user = responseData.user || responseData;
        
        if (token) {
          localStorage.setItem('token', token);
        }
        if (user) {
          localStorage.setItem('user', JSON.stringify(user));
        }
        
        const role = (user?.role || '').toLowerCase();
        
        setIsAuthenticated(true);
        setUserData(user);
        setUserRole(role);
        
        // Возвращаем успех, перенаправление сделает роутинг
        return { success: true, user, role };
        
      } else if (response.status === 403) {
        const error = new Error(data.error || 'Доступ запрещен');
        error.status = 'banned';
        error.isBanned = true;
        throw error;
        
      } else {
        throw new Error(data.error || `Ошибка ${response.status}`);
      }
      
    } catch (error) {
      console.error('Login catch error:', error);
      setError(error.message || 'Ошибка входа');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // Обработчик выхода
  const handleLogout = useCallback(async () => {
    try {
      try {
        await api.authAPI.logout();
      } catch (logoutError) {
        console.log('Logout API error (ignoring):', logoutError);
      }
    } finally {
      clearAuthData();
      window.location.href = '/login';
    }
  }, [clearAuthData]);

  // Обработчик регистрации
  const handleRegister = useCallback(async (userData) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await api.authAPI.register(userData);
      
      if (!response || !response.token) {
        throw new Error('Регистрация не удалась');
      }
      
      localStorage.setItem('token', response.token);
      
      if (response.user) {
        const normalizedRole = normalizeRole(response.user.role);
        localStorage.setItem('user', JSON.stringify(response.user));
        
        setIsAuthenticated(true);
        setUserData(response.user);
        setUserRole(normalizedRole);
      }
      
      return response;
      
    } catch (error) {
      console.error('Registration failed:', error);
      
      let errorMessage = 'Ошибка регистрации';
      if (error.response) {
        errorMessage = error.response.data?.error || error.response.data?.message || errorMessage;
      } else if (error.request) {
        errorMessage = 'Нет ответа от сервера';
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setError(errorMessage);
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [normalizeRole]);

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
      case ROLES.OFFICE:
        return '/office';
      case ROLES.CLIENT:
        return '/client';
      default:
        return '/login';
    }
  }, [normalizeRole]);

  // Защищенный роут компонент
  const ProtectedRoute = ({ children, allowedRoles = [], requireDB = false }) => {
    // Если еще идет инициализация, показываем лоадер
    if (!initialized || isAuthenticated === null || loading) {
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
          </div>
        </div>
      );
    }

    // Если не авторизован - на логин
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

    if (normalizedAllowedRoles.length > 0 && !normalizedAllowedRoles.includes(normalizedUserRole)) {
      console.log(`ProtectedRoute: Role ${normalizedUserRole} not allowed, redirecting`);
      const redirectPath = getRoleRedirect(normalizedUserRole);
      return <Navigate to={redirectPath} replace />;
    }

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

  // Защищенный роут для Office (с OfficeLayout вместо MainLayout)
  const ProtectedOfficeRoute = ({ children, allowedRoles = [ROLES.OFFICE] }) => {
    // Если еще идет инициализация, показываем лоадер
    if (!initialized || isAuthenticated === null || loading) {
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
      console.log(`ProtectedOfficeRoute: Role ${normalizedUserRole} not allowed, redirecting`);
      const redirectPath = getRoleRedirect(normalizedUserRole);
      return <Navigate to={redirectPath} replace />;
    }

    return (
      <OfficeLayout onLogout={handleLogout}>
        {children}
      </OfficeLayout>
    );
  };

  // Защищенный роут для Collector (без шапки MainLayout)
  const ProtectedCollectorRoute = ({ children, allowedRoles = [ROLES.COLLECTOR] }) => {
    // Если еще идет инициализация, показываем лоадер
    if (!initialized || isAuthenticated === null || loading) {
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
          </div>
        </div>
      );
    }

    if (!isAuthenticated) {
      console.log('ProtectedCollectorRoute: Not authenticated, redirecting to login');
      return <Navigate to="/login" replace />;
    }

    const currentUserRole = userRole || '';
    const normalizedUserRole = normalizeRole(currentUserRole);
    const normalizedAllowedRoles = allowedRoles.map(role => 
      normalizeRole(role)
    );

    if (normalizedAllowedRoles.length > 0 && !normalizedAllowedRoles.includes(normalizedUserRole)) {
      console.log(`ProtectedCollectorRoute: Role ${normalizedUserRole} not allowed, redirecting`);
      const redirectPath = getRoleRedirect(normalizedUserRole);
      return <Navigate to={redirectPath} replace />;
    }

    return (
      <CollectorLayout onLogout={handleLogout}>
        {children}
      </CollectorLayout>
    );
  };

  // Лоадер при начальной загрузке
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

  return (
    <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <div className="App">
        {/* Статус подключения к БД */}
        <DatabaseStatusAlert />
        
        {error && (
          <div className="error-banner">
            <span>❌ {error}</span>
            <button onClick={() => setError(null)}>×</button>
          </div>
        )}
        
        <Routes>
          {/* ✅ Public Routes - доступны без авторизации */}
          <Route path="/login" element={
            // Показываем логин ВСЕГДА при первом заходе
            // даже если есть токен в localStorage
            isAuthenticated && initialized ? (
              <Navigate to={getRoleRedirect(userRole)} replace />
            ) : (
              <Login 
                onLogin={handleLogin} 
                loading={loading}
              />
            )
          } />
          
          <Route path="/register" element={
            isAuthenticated && initialized ? (
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
          
         {/* Office Routes */}
<Route path="/office" element={
  <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
    <OfficePage onLogout={handleLogout} /> {/* Добавь onLogout здесь */}
  </ProtectedOfficeRoute>
} />

<Route path="/office/deliveries" element={
  <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
    <OfficeDeliveries onLogout={handleLogout} />
  </ProtectedOfficeRoute>
} />

<Route path="/office/orders" element={
  <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
    <OfficeOrders onLogout={handleLogout} />
  </ProtectedOfficeRoute>
} />

<Route path="/office/reports" element={
  <ProtectedOfficeRoute allowedRoles={[ROLES.OFFICE]}>
    <OfficeReports onLogout={handleLogout} />
  </ProtectedOfficeRoute>
} />
          
          {/* Worker Routes */}
          <Route path="/courier" element={
            <ProtectedRoute allowedRoles={[ROLES.COURIER]}>
              <CourierApp />
            </ProtectedRoute>
          } />
          
          <Route path="/collector" element={
            <ProtectedCollectorRoute allowedRoles={[ROLES.COLLECTOR]}>
              <CollectorApp />
            </ProtectedCollectorRoute>
          } />
          
          {/* ✅ DEFAULT ROUTE - ВСЕГДА на логин */}
          <Route path="/" element={
            <Navigate to="/login" replace />
          } />

          {/* ✅ 404 ROUTE - на логин если страница не найдена */}
          <Route path="*" element={
            <Navigate to="/login" replace />
          } />
        </Routes>
      </div>
    </Router>
  );
}

export default App;