import React from 'react';
import { NavLink } from 'react-router-dom';
import './Sidebar.css';

const Sidebar = ({ items = [], user }) => {
  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-user">
          <div className="sidebar-avatar">
            {user?.name?.charAt(0) || 'U'}
          </div>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name">{user?.name || 'Пользователь'}</div>
            <div className="sidebar-user-role">{user?.role || 'Гость'}</div>
          </div>
        </div>
      </div>
      
      <nav className="sidebar-nav">
        {items.map((item, index) => (
          <NavLink
            key={index}
            to={item.path}
            className={({ isActive }) => 
              `sidebar-link ${isActive ? 'active' : ''}`
            }
          >
            <span className="sidebar-link-icon">{item.icon}</span>
            <span className="sidebar-link-text">{item.label}</span>
            {item.badge && (
              <span className="sidebar-link-badge">{item.badge}</span>
            )}
          </NavLink>
        ))}
      </nav>
      
      <div className="sidebar-footer">
        <button className="sidebar-action">
          <span>⚙️</span>
          Настройки
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;