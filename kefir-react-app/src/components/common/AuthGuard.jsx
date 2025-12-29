// src/components/common/AuthGuard.jsx
import React from 'react';
import { Navigate } from 'react-router-dom';

const AuthGuard = ({ children, requiredRole }) => {
  const token = localStorage.getItem('authToken');
  const userData = JSON.parse(localStorage.getItem('userData') || 'null');
  
  if (!token || !userData) {
    return <Navigate to="/login" />;
  }
  
  if (requiredRole && userData.role !== requiredRole) {
    return <Navigate to="/login" />;
  }
  
  return children;
};

export default AuthGuard;