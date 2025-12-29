import React from 'react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import './DashboardLayout.css';

const DashboardLayout = ({ children, userRole, onLogout }) => {
  // Конфигурация боковой панели по роли
  const getSidebarItems = () => {
    const items = {
      admin: [
        { path: '/admin', label: 'Обзор', icon: '📊' },
        { path: '/admin/clients', label: 'Клиенты', icon: '👥', badge: '3' },
        { path: '/admin/products', label: 'Товары', icon: '📦' },
        { path: '/admin/orders', label: 'Заказы', icon: '📋', badge: '12' },
        { path: '/admin/deliveries', label: 'Доставки', icon: '🚚' },
        { path: '/admin/analytics', label: 'Аналитика', icon: '📈' },
        { path: '/admin/settings', label: 'Настройки', icon: '⚙️' },
      ],
      office: [
        { path: '/office', label: 'Панель', icon: '🏠' },
        { path: '/office/orders', label: 'Заказы', icon: '📋', badge: '24' },
        { path: '/office/deliveries', label: 'Доставки', icon: '🚚', badge: '8' },
        { path: '/office/clients', label: 'Клиенты', icon: '👤' },
        { path: '/office/reports', label: 'Отчеты', icon: '📊' },
        { path: '/office/schedule', label: 'Расписание', icon: '🗓️' },
      ],
    };
    
    return items[userRole] || [];
  };

  return (
    <div className="dashboard-layout">
      <Navbar userRole={userRole} onLogout={onLogout} />
      
      <div className="dashboard-content">
        <Sidebar items={getSidebarItems()} user={{ name: 'Алексей', role: userRole }} />
        
        <main className="dashboard-main">
          <div className="dashboard-container">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;