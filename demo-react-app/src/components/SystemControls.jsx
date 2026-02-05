import React, { useState } from 'react';
import { api } from '../services/api';
import './SystemControls.css';

const SystemControls = ({ onSystemStart, onStatusUpdate }) => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  
  const handleStartSystem = async () => {
    setLoading(true);
    setMessage('🔄 Начинаю запуск системы...');
    
    try {
      // 1. Освобождаем порты
      setMessage('🔧 Освобождаю порты...');
      await api.releasePorts();
      
      // 2. Запускаем систему
      setMessage('🚀 Запускаю бекенд-сервисы...');
      const result = await api.startCompleteSystem();
      
      setMessage(`✅ ${result.message || 'Система запущена!'}`);
      
      // 3. Обновляем статус
      if (onStatusUpdate) {
        setTimeout(() => onStatusUpdate(), 5000);
      }
      
      // 4. Вызываем callback если есть
      if (onSystemStart) {
        onSystemStart(result);
      }
      
      // 5. Автоматически открываем интерфейсы через 30 секунд
      setTimeout(() => {
        openAllUserInterfaces();
      }, 30000);
      
    } catch (error) {
      setMessage(`❌ Ошибка: ${error.message}`);
      console.error('System start error:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleStopSystem = async () => {
    setLoading(true);
    setMessage('🛑 Останавливаю систему...');
    
    try {
      const result = await api.stopSystem();
      setMessage(`✅ ${result.message || 'Система остановлена'}`);
      
      if (onStatusUpdate) {
        setTimeout(() => onStatusUpdate(), 3000);
      }
    } catch (error) {
      setMessage(`❌ Ошибка остановки: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };
  
  const handleRestartSystem = async () => {
    setLoading(true);
    setMessage('🔄 Перезапускаю систему...');
    
    try {
      const result = await api.restartSystem();
      setMessage(`✅ ${result.message || 'Система перезапущена'}`);
      
      if (onStatusUpdate) {
        setTimeout(() => onStatusUpdate(), 5000);
      }
      
      // Автоматически открываем интерфейсы
      setTimeout(() => {
        openAllUserInterfaces();
      }, 30000);
      
    } catch (error) {
      setMessage(`❌ Ошибка перезапуска: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };
  
  const openAllUserInterfaces = () => {
    const interfaces = [
      { name: 'Клиент', url: 'http://localhost:3000' },
      { name: 'Сборщик', url: 'http://localhost:3000/collector' },
      { name: 'Офис', url: 'http://localhost:3000/office' },
      { name: 'Админ', url: 'http://localhost:3000/admin' },
      { name: 'API Docs', url: 'http://localhost:8099/swagger-ui.html' }
    ];
    
    interfaces.forEach(ui => {
      window.open(ui.url, '_blank');
    });
    
    setMessage(prev => `${prev}\n🌐 Открыты все интерфейсы`);
  };
  
  const openSingleInterface = (url, name) => {
    window.open(url, '_blank');
    setMessage(`🌐 Открыт интерфейс: ${name}`);
  };
  
  return (
    <div className="system-controls">
      <h2>🚀 Управление системой</h2>
      
      <div className="controls-grid">
        <button 
          className="btn-start"
          onClick={handleStartSystem}
          disabled={loading}
        >
          {loading ? '🔄 Запуск...' : '🚀 ЗАПУСТИТЬ ВСЮ СИСТЕМУ'}
        </button>
        
        <button 
          className="btn-stop"
          onClick={handleStopSystem}
          disabled={loading}
        >
          🛑 ОСТАНОВИТЬ СИСТЕМУ
        </button>
        
        <button 
          className="btn-restart"
          onClick={handleRestartSystem}
          disabled={loading}
        >
          🔄 ПЕРЕЗАПУСТИТЬ
        </button>
        
        <button 
          className="btn-ports"
          onClick={() => api.forceReleasePorts()}
          disabled={loading}
        >
          🔧 ОСВОБОДИТЬ ПОРТЫ
        </button>
      </div>
      
      {message && (
        <div className="message-box">
          <pre>{message}</pre>
        </div>
      )}
      
      <div className="quick-interfaces">
        <h3>🌐 Быстрый доступ:</h3>
        <div className="interface-buttons">
          <button onClick={() => openSingleInterface('http://localhost:3000', 'Клиент')}>
            👤 Клиент
          </button>
          <button onClick={() => openSingleInterface('http://localhost:3000/collector', 'Сборщик')}>
            👷 Сборщик
          </button>
          <button onClick={() => openSingleInterface('http://localhost:3000/office', 'Офис')}>
            👨‍💼 Офис
          </button>
          <button onClick={() => openSingleInterface('http://localhost:8099', 'Launcher API')}>
            🧰 API
          </button>
          <button onClick={openAllUserInterfaces}>
            🎯 ВСЕ ИНТЕРФЕЙСЫ
          </button>
        </div>
      </div>
    </div>
  );
};

export default SystemControls;