// src/components/common/Navbar.jsx
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Navbar = () => {
  const navigate = useNavigate();
  const userData = JSON.parse(localStorage.getItem('userData') || 'null');
  
  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    navigate('/login');
  };
  
  //if (!userData) return null;
  
  return (
    <nav className="navbar navbar-expand-lg navbar-light bg-white shadow-sm">
      <div className="container">
        <Link className="navbar-brand" to="/">
          🛍️ KEFIR Logistics
        </Link>
        
        <div className="navbar-nav ms-auto">
          {userData.role === 'client' && (
            <>
              <Link className="nav-link" to="/client">Магазин</Link>
              <Link className="nav-link" to="/client/cart">Корзина</Link>
              <Link className="nav-link" to="/client/profile">Профиль</Link>
            </>
          )}
          
          {userData.role === 'admin' && (
            <>
              <Link className="nav-link" to="/admin">Дашборд</Link>
              <Link className="nav-link" to="/admin/clients">Клиенты</Link>
              <Link className="nav-link" to="/admin/products">Товары</Link>
            </>
          )}
          
          <button 
            className="btn btn-outline-danger ms-2" 
            onClick={handleLogout}
          >
            Выйти
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;