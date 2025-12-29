// src/components/layout/MainLayout.jsx
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import Navbar from './Navbar'; // Импортируем ваш готовый Navbar
import './MainLayout.css';

const MainLayout = ({ userRole, userData, onLogout, children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  // ДОБАВЬТЕ ЭТИ ЛОГИ
  console.log('=== MainLayout Debug ===');
  console.log('userRole:', userRole);
  console.log('current path:', location.pathname);
  console.log('children present:', !!children);
  console.log('children type:', typeof children);

  const handleLogout = () => {
    onLogout();
    navigate('/login');
  };

  console.log('=======================');

  return (
    <div className="main-layout">
      {/* ИСПОЛЬЗУЕМ ГОТОВЫЙ NAVBAR КОМПОНЕНТ - РАСКОММЕНТИРУЙТЕ! */}
      <Navbar userRole={userRole} onLogout={handleLogout} />
      
      <main className="app-main">
        {children}
      </main>
      
      <footer className="app-footer">
        <div className="footer-content">
          <p className="footer-text">© 2025 Кефир System. Все права защищены.</p>
          <div className="footer-status">
            <span className="status-dot"></span>
            <span>Система активна</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default MainLayout;