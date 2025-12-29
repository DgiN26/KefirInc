// src/components/office/OfficeSidebar.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';

const OfficeSidebar = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <aside className="w-64 bg-white border-r border-gray-300 flex flex-col h-screen">
      {/* Логотип */}
      <div className="p-6 border-b border-gray-300">
        <div className="flex items-center justify-center">
          <div className="relative">
            <span className="text-5xl font-bold text-black" style={{ 
              fontFamily: "'Comic Sans MS', cursive",
              letterSpacing: '1px'
            }}>
              K
              <span className="text-yellow-500 text-4xl animate-pulse inline-block mx-1">
                ☀️
              </span>
              FIR
            </span>
          </div>
        </div>
        <div className="text-center mt-2">
          <p className="text-lg text-gray-700" style={{ 
            fontFamily: "'Comic Sans MS', cursive" 
          }}>
            Office
          </p>
        </div>
      </div>

      {/* Пустое пространство */}
      <div className="flex-1"></div>

      {/* Кнопка выхода */}
      <div className="p-4 border-t border-gray-300">
        <div className="flex justify-end">
          <button
            onClick={handleLogout}
            className="px-4 py-2 bg-black hover:bg-gray-800 text-white rounded-lg font-medium flex items-center gap-2"
            style={{ fontFamily: "'Comic Sans MS', cursive" }}
          >
            <span>🚪</span>
            <span>Выйти</span>
          </button>
        </div>
      </div>
    </aside>
  );
};

export default OfficeSidebar;