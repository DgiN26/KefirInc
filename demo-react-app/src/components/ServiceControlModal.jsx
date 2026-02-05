// demo-react-app/src/components/ServiceControlModal.jsx
import React, { useState, useEffect, useRef } from 'react';
import './ServiceControlModal.css';

const ServiceControlModal = ({ isOpen, onClose, service }) => {
  const [logs, setLogs] = useState('');
  const [loading, setLoading] = useState(false);
  const [actionLog, setActionLog] = useState('');
  const refreshIntervalRef = useRef(null);

  // Если модальное окно закрыто или нет сервиса - возвращаем null сразу
  if (!isOpen || !service) {
    // Очищаем интервал если есть
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
    }
    return null;
  }

  // Функция получения логов
  const fetchLogs = async () => {
    try {
      // Если это общий обзор системы
      if (service.name === 'SystemOverview') {
        setLogs('=== ОБЗОР СИСТЕМЫ ===\n\nДля просмотра логов конкретного сервиса выберите его из списка мониторинга.');
        return;
      }

      const response = await fetch(`http://localhost:3333/api/logs/${service.name}`);
      if (!response.ok) {
        setLogs(`Ошибка загрузки логов: ${response.status} ${response.statusText}`);
        return;
      }
      
      const data = await response.json();
      setLogs(data.logs || 'Логи отсутствуют');
      setActionLog('✅ Логи загружены');
    } catch (error) {
      setLogs(`Ошибка подключения: ${error.message}`);
      setActionLog('❌ Ошибка загрузки логов');
    }
  };

  // Перезапуск сервиса
  const restartService = async () => {
    if (!service.name || service.name === 'SystemOverview') {
      setActionLog('❌ Нельзя перезапустить системный обзор');
      return;
    }

    if (!confirm(`Перезапустить сервис "${service.displayName || service.name}"?`)) return;
    
    setLoading(true);
    setActionLog('🔄 Перезапуск сервиса...');
    
    try {
      // 1. Останавливаем
      setActionLog(`⏸️  Остановка ${service.name}...`);
      const stopResponse = await fetch(`http://localhost:3333/api/stop/${service.name}`, {
        method: 'POST'
      });
      const stopResult = await stopResponse.json();
      
      if (!stopResult.success) {
        throw new Error(stopResult.message || 'Ошибка остановки');
      }
      
      setActionLog(`✅ Остановлен. Ждем 3 секунды...`);
      await new Promise(resolve => setTimeout(resolve, 3000));
      
      // 2. Запускаем
      setActionLog(`🚀 Запуск ${service.name}...`);
      const startResponse = await fetch(`http://localhost:3333/api/start/${service.name}`, {
        method: 'POST'
      });
      const startResult = await startResponse.json();
      
      if (!startResult.success) {
        throw new Error(startResult.message || 'Ошибка запуска');
      }
      
      setActionLog(`✅ Сервис "${service.displayName || service.name}" перезапущен`);
      
      // 3. Обновляем логи через 5 секунд
      setTimeout(() => {
        fetchLogs();
        setActionLog('🔄 Обновление статуса...');
      }, 5000);
      
    } catch (error) {
      setActionLog(`❌ Ошибка: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Просмотр логов
  const viewLogs = () => {
    fetchLogs();
  };

  // Освободить порт
  const forceKillPort = async () => {
    if (!service.port || service.port === 0) {
      setActionLog('❌ Нельзя освободить порт 0');
      return;
    }

    if (!confirm(`Принудительно освободить порт ${service.port}?\nЭто убьет ВСЕ процессы на этом порту.`)) return;
    
    setLoading(true);
    setActionLog(`🔥 Принудительная очистка порта ${service.port}...`);
    
    try {
      const response = await fetch(`http://localhost:3333/api/kill-port/${service.port}`, {
        method: 'POST'
      });
      const result = await response.json();
      
      if (result.success) {
        setActionLog(`✅ Порт ${service.port} освобожден`);
      } else {
        setActionLog(`⚠️ ${result.message || 'Неизвестная ошибка'}`);
      }
      
      // Обновляем логи
      setTimeout(() => {
        fetchLogs();
      }, 2000);
      
    } catch (error) {
      setActionLog(`❌ Ошибка: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Проверка здоровья сервиса
  const checkHealth = async () => {
    if (!service.port || service.port === 0) return;
    
    setActionLog('🔍 Проверка здоровья сервиса...');
    try {
      const response = await fetch(`http://localhost:${service.port}/health`, {
        method: 'GET'
      }).catch(() => null);
      
      if (response && response.ok) {
        setActionLog('✅ Сервис здоров и отвечает');
      } else {
        setActionLog('⚠️ Сервис не отвечает на health-check');
      }
    } catch (error) {
      setActionLog('❌ Ошибка проверки здоровья');
    }
  };

  // Автообновление логов
  const toggleAutoRefresh = () => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
      setActionLog('⏸️  Автообновление выключено');
    } else {
      refreshIntervalRef.current = setInterval(fetchLogs, 10000);
      setActionLog('🔄 Автообновление включено (10 сек)');
    }
  };

  // Загружаем логи при открытии и очищаем при закрытии
  useEffect(() => {
    if (isOpen && service) {
      fetchLogs();
    }

    // Очищаем интервал при размонтировании
    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
        refreshIntervalRef.current = null;
      }
    };
  }, [isOpen, service]); // Добавляем зависимости

  // Определяем заголовок модального окна
  const getModalTitle = () => {
    if (service.name === 'SystemOverview') {
      return '🛠️ Управление системой KEFIR';
    }
    return `🛠️ Управление сервисом: ${service.displayName || service.name}`;
  };

  // Определяем доступные действия
  const canRestart = service.name && service.name !== 'SystemOverview';
  const canKillPort = service.port && service.port > 0;
  const canCheckHealth = service.port && service.port > 0 && service.type !== 'system';

  return (
    <div className="service-control-modal">
      <div className="modal-overlay" onClick={onClose}></div>
      
      <div className="modal-content">
        <div className="modal-header">
          <h2>{getModalTitle()}</h2>
          <button className="close-btn" onClick={onClose} disabled={loading}>×</button>
        </div>
        
        <div className="modal-body">
          {/* Информация о сервисе */}
          <div className="service-info">
            <div className="info-row">
              <span className="label">Имя:</span>
              <span className="value">{service.displayName || service.name || 'Неизвестно'}</span>
            </div>
            
            {service.port > 0 && (
              <div className="info-row">
                <span className="label">Порт:</span>
                <span className="value">{service.port}</span>
              </div>
            )}
            
            <div className="info-row">
              <span className="label">Статус:</span>
              <span className={`value ${service.running ? 'running' : 'stopped'}`}>
                {service.running ? '✅ Запущен' : '❌ Остановлен'}
              </span>
            </div>
            
            {service.pid && (
              <div className="info-row">
                <span className="label">PID:</span>
                <span className="value">{service.pid}</span>
              </div>
            )}
            
            {service.type && (
              <div className="info-row">
                <span className="label">Тип:</span>
                <span className="value">
                  {service.type === 'frontend' ? 'Фронтенд' : 
                   service.type === 'backend' ? 'Бекенд' : 
                   service.type === 'launcher' ? 'Launcher' : 
                   service.type === 'system' ? 'Система' : service.type}
                </span>
              </div>
            )}
          </div>
          
          {/* Кнопки управления */}
          <div className="control-buttons">
            {canRestart && (
              <button 
                className="btn-restart"
                onClick={restartService}
                disabled={loading}
              >
                {loading ? '🔄 Выполняется...' : '🔁 Перезапустить'}
              </button>
            )}
            
            <button 
              className="btn-logs"
              onClick={viewLogs}
              disabled={loading}
            >
              📋 Просмотр логов
            </button>
            
            {canKillPort && (
              <button 
                className="btn-kill"
                onClick={forceKillPort}
                disabled={loading}
              >
                🔥 Освободить порт
              </button>
            )}
            
            {canCheckHealth && (
              <button 
                className="btn-health"
                onClick={checkHealth}
                disabled={loading}
              >
                💚 Проверить здоровье
              </button>
            )}
            
            <button 
              className="btn-refresh"
              onClick={toggleAutoRefresh}
            >
              {refreshIntervalRef.current ? '⏸️ Выкл. автообнов.' : '🔄 Вкл. автообнов.'}
            </button>
          </div>
          
          {/* Лог действий */}
          {actionLog && (
            <div className="action-log">
              <h4>📝 Журнал действий:</h4>
              <div className="log-message">{actionLog}</div>
            </div>
          )}
          
          {/* Логи сервиса */}
          <div className="service-logs">
            <div className="logs-header">
              <h4>📄 {service.name === 'SystemOverview' ? 'Информация о системе' : 'Логи сервиса'}</h4>
              <div className="logs-actions">
                <button onClick={fetchLogs} disabled={loading}>
                  🔄 Обновить
                </button>
                <button onClick={() => navigator.clipboard.writeText(logs)}>
                  📋 Копировать
                </button>
                <button onClick={() => setLogs('')}>
                  🧹 Очистить
                </button>
              </div>
            </div>
            <div className="logs-content">
              <pre>{logs || 'Загрузка логов...'}</pre>
            </div>
            {service.name !== 'SystemOverview' && (
              <div className="logs-info">
                <small>Последние 1000 строк логов. Полные логи доступны через API.</small>
              </div>
            )}
          </div>
        </div>
        
        <div className="modal-footer">
          <div className="footer-info">
            {service.name !== 'SystemOverview' && (
              <div className="quick-links">
                <button 
                  className="btn-link"
                  onClick={() => window.open(`http://localhost:${service.port}`, '_blank')}
                  disabled={!service.running}
                >
                  🌐 Открыть в браузере
                </button>
                <button 
                  className="btn-link"
                  onClick={() => window.open(`http://localhost:3333/api/status`, '_blank')}
                >
                  📊 Весь статус
                </button>
              </div>
            )}
          </div>
          <button className="btn-close" onClick={onClose} disabled={loading}>
            {loading ? 'Закрыть (ожидание...)' : 'Закрыть'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ServiceControlModal;