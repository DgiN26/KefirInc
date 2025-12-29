// src/pages/HomePage.jsx
import React from 'react';
import { Link } from 'react-router-dom';

const HomePage = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-600 to-blue-500 flex items-center justify-center">
      <div className="text-center text-white p-8">
        <h1 className="text-5xl font-bold mb-4">KEFIR Logistics</h1>
        <p className="text-xl mb-8">Система управления доставками</p>
        
        <div className="space-x-4">
          <Link 
            to="/login" 
            className="inline-block bg-white text-purple-600 px-8 py-3 rounded-lg font-bold hover:bg-gray-100 transition"
          >
            Войти
          </Link>
          <Link 
            to="/register" 
            className="inline-block bg-transparent border-2 border-white text-white px-8 py-3 rounded-lg font-bold hover:bg-white/10 transition"
          >
            Регистрация
          </Link>
        </div>
        
        <div className="mt-12">
          <p className="text-lg mb-4">Демо аккаунты:</p>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4 max-w-4xl mx-auto">
            {[
              { role: '👑 Админ', login: 'admin / admin' },
              { role: '🛍️ Клиент', login: 'client / client' },
              { role: '🚴 Курьер', login: 'courier / courier' },
              { role: '📦 Сборщик', login: 'collector / collector' },
              { role: '🏢 Офис', login: 'office / office' },
            ].map((account, index) => (
              <div key={index} className="bg-white/10 backdrop-blur-sm p-4 rounded-lg">
                <div className="text-2xl mb-2">{account.role.split(' ')[0]}</div>
                <div className="text-sm">{account.login}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;