// src/App.js
import React, { useState, useEffect, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import './styles/global.css';

// Layout Components
import MainLayout from './components/layout/MainLayout';
import OfficeLayout from './components/layout/OfficeLayout';

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

// Константы ролей
const ROLES = {
  ADMIN: 'admin',
  CLIENT: 'client',
  COURIER: 'courier',
  COLLECTOR: 'collector',
  OFFICE: 'office',
};

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false); // Всегда false при старте
  const [userRole, setUserRole] = useState('');
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(false); // Начинаем с false, не ждем проверки
  const [error, setError] = useState(null);

  // Функция для нормализации роли
  const normalizeRole = useCallback((role) => {
    if (!role) return '';
    return String(role).toLowerCase().trim();
  }, []);

  // Очистка данных авторизации
  const clearAuthData = useCallback(() => {
    setIsAuthenticated(false);
    setUserRole('');
    setUserData(null);
    setError(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token'); // Очищаем и sessionStorage на всякий случай
    sessionStorage.removeItem('user');
  }, []);

  // Обработчик входа
  const handleLogin = async (credentials) => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('Attempting login with:', credentials);
      
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials)
      });
      
      const data = await response.json();
      console.log('Login API response:', data);
      
      if (response.ok) {
        const responseData = data.body || data;
        const token = responseData.token || responseData.accessToken;
        const user = responseData.user || responseData;
        
        // Сохраняем данные только в sessionStorage, не в localStorage
        if (token) {
          sessionStorage.setItem('token', token);
          console.log('Token saved to sessionStorage');
        }
        if (user) {
          sessionStorage.setItem('user', JSON.stringify(user));
          console.log('User data saved to sessionStorage:', user);
        }
        
        const role = (user?.role || '').toLowerCase();
        
        setIsAuthenticated(true);
        setUserData(user);
        setUserRole(role);
        
        console.log('Login successful, user authenticated:', { role, user });
        
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
  const handleLogout = useCallback(() => {
    console.log('Logging out...');
    
    // Вызываем logout API если нужно
    try {
      fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${sessionStorage.getItem('token')}`
        }
      }).catch(err => console.log('Logout API error (ignoring):', err));
    } finally {
      clearAuthData();
      // Перенаправляем на страницу логина
      window.location.href = '/login';
    }
  }, [clearAuthData]);

  // Обработчик регистрации
  const handleRegister = async (userData) => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('Registering user:', userData);
      
      const response = await fetch('http://localhost:8080/api/clients/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(userData)
      });
      
      const data = await response.json();
      console.log('Register API response:', data);
      
      if (response.ok || response.status === 201) {
        // После успешной регистрации предлагаем залогиниться
        // Не логиним автоматически
        alert('Регистрация успешна! Теперь вы можете войти в систему.');
        return { success: true, message: 'Регистрация успешна' };
        
      } else {
        throw new Error(data.error || data.message || `Ошибка ${response.status}`);
      }
      
    } catch (error) {
      console.error('Registration failed:', error);
      setError(error.message || 'Ошибка регистрации');
      throw error;
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
      case ROLES.OFFICE:
        return '/office';
      case ROLES.CLIENT:
        return '/client';
      default:
        return '/login';
    }
  }, [normalizeRole]);

  // Защищенный роут компонент
  const ProtectedRoute = ({ children, allowedRoles = [] }) => {
    // Показываем лоадер только если идет процесс входа
    if (loading) {
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

    // Всегда проверяем, аутентифицирован ли пользователь
    if (!isAuthenticated) {
      console.log('ProtectedRoute: Not authenticated, redirecting to login');
      return <Navigate to="/login" replace />;
    }

    const currentUserRole = userRole || '';
    const normalizedUserRole = normalizeRole(currentUserRole);
    const normalizedAllowedRoles = allowedRoles.map(role => 
      normalizeRole(role)
    );

    if (normalizedAllowedRoles.length > 0 && !normalizedAllowedRoles.includes(normalizedUserRole)) {
      console.log(`ProtectedRoute: Role ${normalizedUserRole} not allowed, redirecting to ${getRoleRedirect(normalizedUserRole)}`);
      const redirectPath = getRoleRedirect(normalizedUserRole);
      return <Navigate to={redirectPath} replace />;
    }

    return (
      <MainLayout 
        userRole={normalizedUserRole}
        userData={userData}
        onLogout={handleLogout}
      >
        {children}
      </MainLayout>
    );
  };

  // Защищенный роут для Office (с OfficeLayout вместо MainLayout)
  const ProtectedOfficeRoute = ({ children, allowedRoles = [ROLES.OFFICE] }) => {
    if (loading) {
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
      console.log(`ProtectedOfficeRoute: Role ${normalizedUserRole} not allowed, redirecting to ${getRoleRedirect(normalizedUserRole)}`);
      const redirectPath = getRoleRedirect(normalizedUserRole);
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
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminDashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/clients" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminClients />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/products" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminProducts />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/carts" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminCarts />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/warehouse" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminWarehouse />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/couriers" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminCouriers />
            </ProtectedRoute>
          } />
          
          <Route path="/admin/deliveries" element={
            <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
              <AdminDeliveries />
            </ProtectedRoute>
          } />
          
          {/* Office Routes */}
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