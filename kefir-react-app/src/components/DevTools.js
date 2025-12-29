import React, { useState, useEffect } from 'react';
import api from '../services/api';

const DevTools = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [apiStatus, setApiStatus] = useState('Проверка...');
  const [apiResponse, setApiResponse] = useState(null);

  useEffect(() => {
    checkApiHealth();
  }, []);

  const checkApiHealth = async () => {
    try {
      const isAvailable = await api.checkRealApiStatus();
      setApiStatus(isAvailable ? '✅ Доступен' : '❌ Недоступен');
    } catch (error) {
      setApiStatus('⚠️ Ошибка проверки');
    }
  };

  const testRealApi = async () => {
    try {
      const response = await fetch(
        process.env.REACT_APP_API_GATEWAY || 'http://localhost:8080',
        { method: 'GET' }
      );
      setApiResponse({
        status: response.status,
        statusText: response.statusText,
        ok: response.ok
      });
    } catch (error) {
      setApiResponse({ error: error.message });
    }
  };

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        style={{
          position: 'fixed',
          bottom: '10px',
          right: '10px',
          padding: '5px 10px',
          backgroundColor: '#007bff',
          color: 'white',
          border: 'none',
          borderRadius: '3px',
          cursor: 'pointer',
          fontSize: '12px',
          zIndex: 1000
        }}
      >
        Dev Tools
      </button>
    );
  }

  return (
    <div style={{
      position: 'fixed',
      bottom: '10px',
      right: '10px',
      width: '300px',
      backgroundColor: '#f8f9fa',
      border: '1px solid #dee2e6',
      borderRadius: '5px',
      padding: '15px',
      boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
      zIndex: 1000
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
        <h4 style={{ margin: 0 }}>Инструменты разработчика</h4>
        <button onClick={() => setIsOpen(false)} style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer' }}>×</button>
      </div>
      
      <div style={{ marginBottom: '10px' }}>
        <strong>Текущий режим:</strong> {api.isUsingRealAPI() ? 'Реальный API' : 'Мок API'}
      </div>
      
      <div style={{ marginBottom: '10px' }}>
        <strong>Статус реального API:</strong> {apiStatus}
      </div>
      
      {apiResponse && (
        <div style={{ marginBottom: '10px', fontSize: '11px', backgroundColor: '#e9ecef', padding: '5px', borderRadius: '3px' }}>
          <pre>{JSON.stringify(apiResponse, null, 2)}</pre>
        </div>
      )}
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
        <button 
          onClick={api.switchToRealAPI}
          disabled={api.isUsingRealAPI()}
          style={{ padding: '8px', backgroundColor: '#28a745', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
        >
          Переключиться на реальный API
        </button>
        
        <button 
          onClick={api.switchToMockAPI}
          disabled={!api.isUsingRealAPI()}
          style={{ padding: '8px', backgroundColor: '#17a2b8', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
        >
          Переключиться на мок API
        </button>
        
        <button 
          onClick={checkApiHealth}
          style={{ padding: '8px', backgroundColor: '#6c757d', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
        >
          Проверить статус API
        </button>
        
        <button 
          onClick={testRealApi}
          style={{ padding: '8px', backgroundColor: '#ffc107', color: 'black', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
        >
          Тест реального API
        </button>
        
        <button 
          onClick={api.resetApiMode}
          style={{ padding: '8px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '3px', cursor: 'pointer' }}
        >
          Сбросить настройки
        </button>
      </div>
    </div>
  );
};

export default DevTools;