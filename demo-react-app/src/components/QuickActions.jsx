import React from 'react';
import { api } from '../services/api';
import './QuickActions.css';

const QuickActions = ({ onStatusUpdate }) => {
  const quickActions = [
    {
      name: '🔍 Быстрая проверка',
      action: async () => {
        const result = await api.quickCheck();
        alert(`Быстрая проверка:\n${JSON.stringify(result, null, 2)}`);
      },
      color: '#2196F3'
    },
    {
      name: '🔧 Авто-фикс портов',
      action: async () => {
        const result = await api.autoFixPorts();
        alert(`Авто-фикс выполнен:\n${JSON.stringify(result, null, 2)}`);
        if (onStatusUpdate) onStatusUpdate();
      },
      color: '#FF9800'
    },
    {
      name: '📋 Список сервисов',
      action: async () => {
        const result = await api.listAllServices();
        alert(`Список сервисов:\n${JSON.stringify(result, null, 2)}`);
      },
      color: '#9C27B0'
    },
    {
      name: '📊 Статус Launcher',
      action: () => window.open('http://localhost:3333/api/status', '_blank'),
      color: '#009688'
    },
    {
      name: '📚 Документация',
      action: () => window.open('http://localhost:3333/', '_blank'),
      color: '#3F51B5'
    },
    {
      name: '🔄 Полная проверка',
      action: async () => {
        const result = await api.fullSystemCheck();
        alert(`Полная проверка системы:\n${JSON.stringify(result, null, 2)}`);
        if (onStatusUpdate) onStatusUpdate();
      },
      color: '#4CAF50'
    }
  ];

  return (
    <div className="quick-actions">
      <h3>⚡ Быстрые действия</h3>
      <div className="actions-grid">
        {quickActions.map((action, index) => (
          <button
            key={index}
            className="quick-action-btn"
            onClick={action.action}
            style={{ backgroundColor: action.color }}
          >
            {action.name}
          </button>
        ))}
      </div>
    </div>
  );
};

export default QuickActions;