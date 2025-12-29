// src/components/common/RoleGuard.jsx
import React from 'react';
import { Navigate } from 'react-router-dom';

const RoleGuard = ({ children, role }) => {
  const userData = JSON.parse(localStorage.getItem('userData') || 'null');
  
  if (!userData) {
    return <Navigate to="/login" />;
  }
  
  if (userData.role !== role) {
    // Если роль не совпадает, перенаправляем в соответствии с ролью
    switch(userData.role) {
      case 'admin':
        return <Navigate to="/admin" />;
      case 'courier':
        return <Navigate to="/courier" />;
      case 'collector':
        return <Navigate to="/collector" />;
      default:
        return <Navigate to="/client" />;
    }
  }
  
  return children;
};

export default RoleGuard;