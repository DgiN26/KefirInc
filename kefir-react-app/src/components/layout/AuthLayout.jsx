import React from 'react';
import { Outlet } from 'react-router-dom';
import './AuthLayout.css';

const AuthLayout = ({ themeMode }) => {
  return (
    <div className={`auth-layout ${themeMode}`}>
      <div className="auth-background">
        <div className="gradient-circle circle-1"></div>
        <div className="gradient-circle circle-2"></div>
        <div className="gradient-circle circle-3"></div>
      </div>
      
      <div className="auth-container">
        <div className="auth-card glass-card">
          <div className="auth-header">
            <div className="auth-logo">
              <span className="logo-icon">🚚</span>
              <h1 className="logo-text">KEFIR Logistics</h1>
            </div>
            <p className="auth-subtitle">Управление доставками</p>
          </div>
          
          <div className="auth-content">
            <Outlet />
          </div>
          
          <div className="auth-footer">
            <p>© 2024 KEFIR Logistics. Все права защищены.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;