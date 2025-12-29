import React, { useState, useEffect } from 'react';
import api from '../services/api';

const ApiSwitcher = () => {
  const [isRealApi, setIsRealApi] = useState(api.isUsingRealAPI());
  const [apiStatus, setApiStatus] = useState('');

  // Проверяем статус API
  const checkApiStatus = async () => {
    try {
      // Пробуем сделать простой запрос
      const response = await fetch(process.env.REACT_APP_API_GATEWAY || 'http://localhost:8080');
      if (response.ok) {
        setApiStatus('✅ API доступен');
      } else {
        setApiStatus('⚠️ API недоступен');
      }
    } catch (error) {
      setApiStatus('❌ Ошибка подключения к API');
    }
  };

  useEffect(() => {
    checkApiStatus();
  }, []);

  const handleSwitchToReal = () => {
    localStorage.setItem('forceRealApi', 'true');
    api.forceUseRealAPI();
  };

  const handleSwitchToMock = () => {
    localStorage.setItem('forceRealApi', 'false');
    api.forceUseMockAPI();
  };

  return (
    <div style={{
      position: 'fixed',
      top: 10,
      right: 10,
      backgroundColor: '#f5f5f5',
      padding: '10px',
      border: '1px solid #ddd',
      borderRadius: '5px',
      zIndex: 1000,
      fontSize: '12px'
    }}>
      <div><strong>Режим API:</strong></div>
      <div>Текущий: <strong>{isRealApi ? 'Реальный' : 'Мок'}</strong></div>
      <div>Статус: {apiStatus}</div>
      <div style={{ marginTop: '10px', display: 'flex', gap: '5px' }}>
        <button 
          onClick={handleSwitchToReal}
          disabled={isRealApi}
          style={{
            padding: '5px',
            fontSize: '10px',
            backgroundColor: isRealApi ? '#4CAF50' : '#ccc',
            color: 'white',
            border: 'none',
            borderRadius: '3px',
            cursor: isRealApi ? 'default' : 'pointer'
          }}
        >
          Реальный API
        </button>
        <button 
          onClick={handleSwitchToMock}
          disabled={!isRealApi}
          style={{
            padding: '5px',
            fontSize: '10px',
            backgroundColor: !isRealApi ? '#2196F3' : '#ccc',
            color: 'white',
            border: 'none',
            borderRadius: '3px',
            cursor: !isRealApi ? 'default' : 'pointer'
          }}
        >
          Мок API
        </button>
      </div>
    </div>
  );
};

export default ApiSwitcher;