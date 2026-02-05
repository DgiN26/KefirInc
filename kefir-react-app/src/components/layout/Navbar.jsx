// src/components/layout/Navbar.jsx
import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import './Navbar.css';

const Navbar = ({ userRole, onLogout }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  
  // Константы для ролей
  const ROLES = {
    ADMIN: 'admin',
    OFFICE: 'office',
    CLIENT: 'client',
    COURIER: 'courier',
    COLLECTOR: 'collector'
  };
  
  // НОРМАЛИЗАЦИЯ РОЛИ (БЫЛО ПРОПУЩЕНО)
  const normalizedRole = userRole ? userRole.toLowerCase() : '';
  
  const navConfig = {
    [ROLES.ADMIN]: [
      { path: '/admin', label: 'Главная', icon: '🏠', color: '#4CAF50', exact: true },
      { path: '/admin/clients', label: 'Клиенты', icon: '👥', color: '#2196F3' },
      { path: '/admin/products', label: 'Товары', icon: '📦', color: '#FF9800' },
      { path: '/admin/deliveries', label: 'Доставки', icon: '🚚', color: '#9C27B0' },
      { path: '/admin/couriers', label: 'Курьеры', icon: '🚴', color: '#00BCD4' },
      { path: '/admin/warehouse', label: 'Склад', icon: '🏭', color: '#795548' },
    ],
    [ROLES.OFFICE]: [
      { path: '/office', label: 'Панель', icon: '📊', color: '#3F51B5', exact: true },
      { path: '/office/orders', label: 'Заказы', icon: '📋', color: '#E91E63' },
      { path: '/office/deliveries', label: 'Доставки', icon: '🚚', color: '#009688' },
      { path: '/office/reports', label: 'Отчеты', icon: '📈', color: '#FF5722' },
      { path: '/office/payments', label: 'Платежи', icon: '💰', color: '#8BC34A' },
    ],
    [ROLES.CLIENT]: [
      { path: '/client', label: 'Магазин', icon: '🛍️', color: '#2196F3', exact: true },
<<<<<<< HEAD
      { path: '/client/cart', label: 'Корзина', icon: '🛒', color: '#FF9800' },
      // ✅ ДОБАВЛЕНА ПОДДЕРЖКА:
      { path: '/client/support', label: 'Поддержка', icon: '📞', color: '#9C27B0' },
=======
      { path: '/client/cart', label: 'История заказов', icon: '🛒', color: '#FF9800' },
>>>>>>> 564aab1fe7bb07a8b7dbf2263c3f15f5d97137da
      { path: '/client/profile', label: 'Профиль', icon: '👤', color: '#4CAF50' },
      { path: '/client/notification', label: 'Уведомление', icon: '🔔', color: '#4CAF50'}
    ],
    [ROLES.COURIER]: [
      { path: '/courier', label: 'Панель', icon: '🚴', color: '#00BCD4', exact: true },
      { path: '/courier/orders', label: 'Заказы', icon: '📦', color: '#FF9800', asButton: true },
      { path: '/courier/routes', label: 'Маршруты', icon: '🗺️', color: '#9C27B0', asButton: true },
      { path: '/courier/stats', label: 'Статистика', icon: '📊', color: '#4CAF50', asButton: true },
    ],
    [ROLES.COLLECTOR]: [
      { path: '/collector', label: 'Панель', icon: '📦', color: '#795548', exact: true },
      { path: '/collector/tasks', label: 'Задачи', icon: '✅', color: '#2196F3', asButton: true },
      { path: '/collector/products', label: 'Товары', icon: '📋', color: '#FF9800', asButton: true },
    ],
  };

  const roleBrands = {
    [ROLES.ADMIN]: { 
      text: '🏢 Админ-панель', 
      redirect: '/admin',
      bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    },
    [ROLES.OFFICE]: { 
      text: '📊 Панель офиса', 
      redirect: '/office',
      bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
    },
    [ROLES.CLIENT]: { 
      text: '🛍️ KEFIR Store', 
      redirect: '/client',
      bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
    },
    [ROLES.COURIER]: { 
      text: '🚴 Приложение курьера', 
      redirect: '/courier',
      bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
    },
    [ROLES.COLLECTOR]: { 
      text: '📦 Приложение сборщика', 
      redirect: '/collector',
      bgColor: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
    },
  };

  const handleLogoutClick = () => {
    onLogout();
    navigate('/login');
  };

  const getNavItems = () => {
    const items = navConfig[normalizedRole] || [];
    return items.map((item, index) => {
      const isActive = item.exact 
        ? location.pathname === item.path
        : location.pathname.startsWith(item.path);
      
      if (item.asButton) {
        return (
          <button
            key={index}
            className={`nav-btn ${isActive ? 'nav-btn-active' : ''}`}
            onClick={() => navigate(item.path)}
            style={{
              '--btn-color': item.color,
            }}
          >
            <span className="nav-btn-icon">{item.icon}</span>
            <span className="nav-btn-label">{item.label}</span>
            {isActive && <span className="nav-btn-indicator"></span>}
          </button>
        );
      }
      
      return (
        <Link
          key={index}
          className={`nav-link ${isActive ? 'nav-link-active' : ''}`}
          to={item.path}
          style={{
            '--link-color': item.color,
          }}
        >
          <span className="nav-link-icon">{item.icon}</span>
          <span className="nav-link-label">{item.label}</span>
          {isActive && <span className="nav-link-indicator"></span>}
        </Link>
      );
    });
  };

  const getUserData = () => {
    try {
      const stored = localStorage.getItem('userData');
      return stored ? JSON.parse(stored) : {};
    } catch (error) {
      console.error('Error parsing user data:', error);
      return {};
    }
  };

  const getBrandInfo = () => roleBrands[normalizedRole] || { 
    text: 'KEFIR Logistics', 
    redirect: '/',
    bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  };

  const getUserDisplayName = () => {
    const userData = getUserData();
    return userData.name || userData.username || 'Пользователь';
  };

  const brandInfo = getBrandInfo();

  const toggleMobileMenu = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  return (
    <nav className="navbar" style={{ background: brandInfo.bgColor }}>
      <div className="navbar-container">
        {/* Бренд/логотип */}
        <div className="navbar-brand">
          <Link to={brandInfo.redirect} className="brand-link">
            <span className="brand-text">{brandInfo.text}</span>
            <span className="brand-badge">{normalizedRole}</span>
          </Link>
        </div>

        {/* Основная навигация для десктопа */}
        <div className="navbar-nav-desktop">
          {getNavItems()}
        </div>

        {/* Пользовательская панель */}
        <div className="navbar-user-panel">
          <div className="user-info">
            <div className="user-avatar">
              {getUserDisplayName().charAt(0).toUpperCase()}
            </div>
            <div className="user-details">
              <span className="user-name">{getUserDisplayName()}</span>
              <span className="user-role">{normalizedRole}</span>
            </div>
          </div>
          
          <button 
            className="logout-btn"
            onClick={handleLogoutClick}
            title="Выйти из системы"
          >
            <span className="logout-icon">🚪</span>
            <span className="logout-text">Выйти</span>
          </button>
        </div>

        {/* Мобильное меню */}
        <button 
          className={`mobile-menu-toggle ${mobileMenuOpen ? 'open' : ''}`}
          onClick={toggleMobileMenu}
          aria-label="Открыть меню"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>

        {/* Мобильная навигация */}
        <div className={`navbar-nav-mobile ${mobileMenuOpen ? 'open' : ''}`}>
          <div className="mobile-user-info">
            <div className="mobile-user-avatar">
              {getUserDisplayName().charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="mobile-user-name">{getUserDisplayName()}</div>
              <div className="mobile-user-role">{normalizedRole}</div>
            </div>
          </div>
          
          <div className="mobile-nav-links">
            {getNavItems()}
          </div>
          
          <button 
            className="mobile-logout-btn"
            onClick={handleLogoutClick}
          >
            <span className="mobile-logout-icon">🚪</span>
            Выйти из системы
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;