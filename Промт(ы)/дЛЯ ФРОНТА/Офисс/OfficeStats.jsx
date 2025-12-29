// src/components/office/OfficeStats.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';

const OfficeStats = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      // Заглушка статистики
      setStats({
        activeProblems: 5,
        notifiedClients: 3,
        waitingClient: 2,
        resolvedToday: 12,
        todayOrders: 24,
        activeUsers: 156,
        totalProducts: 842
      });
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    { title: 'Клиентов', value: '42', change: '+5 за неделю', icon: '👥' },
    { title: 'Товаров', value: '156', change: '+12 за неделю', icon: '📦' },
    { title: 'Доставок', value: '18', change: '+8 сегодня', icon: '🚚' },
    { title: 'Сборщиков', value: '8', change: '+2 за месяц', icon: '👷' },
  ];

  const managementItems = [
    { icon: '💾', label: 'Резервное копирование' },
    { icon: '📈', label: 'Аналитика системы' },
    { icon: '🔔', label: 'Уведомления' },
    { icon: '🔒', label: 'Безопасность' },
    { icon: '⚙️', label: 'Настройки системы' },
    { icon: '❓', label: 'Помощь и поддержка' },
    { icon: '📤', label: 'Экспорт данных' },
    { icon: '👑', label: 'Управление ролями' },
    { icon: '🚪', label: 'Выйти из системы' },
  ];

  if (loading) {
    return (
      <div className="text-center py-10">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-black"></div>
        <p className="mt-2 text-gray-500">Загрузка статистики...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-xl font-bold text-black mb-6">📊 Системная статистика</h2>
        
        {/* Карточки статистики */}
        <div className="grid grid-cols-2 gap-4">
          {statCards.map((stat, index) => (
            <div
              key={index}
              className="bg-white border-2 border-black rounded-xl p-4"
            >
              <div className="flex justify-between items-start">
                <div>
                  <p className="text-3xl font-bold text-black">{stat.value}</p>
                  <p className="text-sm text-gray-600 mt-1">{stat.title}</p>
                  <p className="text-xs text-green-600 font-medium mt-1">{stat.change}</p>
                </div>
                <div className="text-2xl">{stat.icon}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Итог за сегодня */}
        <div className="mt-6 p-4 bg-gray-50 border border-gray-300 rounded-lg">
          <p className="text-black font-medium">
            Сегодня: 12 новых заказов, 8 доставок завершено
          </p>
        </div>
      </div>

      {/* Блок управления системой */}
      <div>
        <h3 className="text-lg font-bold text-black mb-4">⚙️ Управление системой</h3>
        <div className="grid grid-cols-3 gap-3">
          {managementItems.map((item, index) => (
            <button
              key={index}
              className="bg-white hover:bg-gray-50 border-2 border-gray-300 hover:border-black rounded-lg p-3 flex flex-col items-center transition-colors"
            >
              <span className="text-xl mb-2">{item.icon}</span>
              <span className="text-xs text-center text-gray-800">{item.label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default OfficeStats;