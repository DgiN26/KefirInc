// src/components/ProtectedRoute.jsx
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import LoadingScreen from '../ui/LoadingScreen';
import MainLayout from './layout/MainLayout';
import OfficeLayout from './layout/OfficeLayout';

const ProtectedRoute = ({ 
  isAuthenticated, 
  userRole, 
  getRoleRedirect,
  allowedRoles = [],
  useOfficeLayout = false,
  requireDB = false,
  dbConnected = null,
  dbLoading = false
}) => {
  if (isAuthenticated === null) {
    return <LoadingScreen message="Проверка авторизации..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Проверка ролей
  if (allowedRoles.length > 0 && !allowedRoles.includes(userRole)) {
    const redirectPath = getRoleRedirect(userRole);
    return <Navigate to={redirectPath} replace />;
  }

  // Проверка БД если требуется
  if (requireDB && dbConnected === false && !dbLoading) {
    return (
      <MainLayout>
        <div className="container mt-4">
          <div className="alert alert-danger">
            <h4 className="alert-heading">⚠️ База данных недоступна</h4>
            <p>Для доступа к этой странице требуется подключение к PostgreSQL.</p>
            <p>Пожалуйста, проверьте подключение к базе данных и повторите попытку.</p>
          </div>
        </div>
      </MainLayout>
    );
  }

  // Выбор layout
  const Layout = useOfficeLayout ? OfficeLayout : MainLayout;

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
};

export default ProtectedRoute;