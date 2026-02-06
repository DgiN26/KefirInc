import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import './ServiceMonitor.css';

const ServiceMonitor = ({ onOpenServiceControl, onStatusUpdate }) => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [filter, setFilter] = useState('all'); // all, running, stopped
  const [searchTerm, setSearchTerm] = useState('');

  // Стандартный набор сервисов KEFIR
  const defaultServices = [
    { name: 'ApiGateway', port: 8080, type: 'backend', displayName: 'API Gateway' },
    { name: 'Auth', port: 8097, type: 'backend', displayName: 'Authentication Service' },
    { name: 'User', port: 8081, type: 'backend', displayName: 'User Management' },
    { name: 'Sklad', port: 8082, type: 'backend', displayName: 'Warehouse Service' },
    { name: 'Collector', port: 8086, type: 'backend', displayName: 'Collector Service' },
    { name: 'Backet', port: 8083, type: 'backend', displayName: 'Shopping Cart' },
    { name: 'Office', port: 8085, type: 'backend', displayName: 'Office Management' },
    { name: 'Delivery', port: 8088, type: 'backend', displayName: 'Delivery Service' },
    { name: 'TransactionSaga', port: 8090, type: 'backend', displayName: 'Transaction Saga' },
    { name: 'KefirFrontend', port: 3000, type: 'frontend', displayName: 'Logistics Frontend' },
    { name: 'LauncherManager', port: 3333, type: 'launcher', displayName: 'Launcher API' }
  ];

  const fetchServicesStatus = async () => {
    try {
      const status = await api.getSystemStatus();
      
      // Если API вернул данные о сервисах
      if (status && status.services) {
        const updatedServices = status.services.map(apiService => {
          // Находим соответствующий сервис из defaultServices
          const defaultService = defaultServices.find(s => 
            s.name === apiService.name || s.port === apiService.port
          ) || apiService;
          
          return {
            name: apiService.name || defaultService.name,
            displayName: defaultService.displayName || apiService.name,
            port: apiService.port || defaultService.port,
            type: apiService.type || defaultService.type || 'backend',
            status: apiService.running ? 'running' : 'stopped',
            health: apiService.running ? 'healthy' : 'unhealthy',
            pid: apiService.pid || null,
            lastChecked: new Date().toLocaleTimeString(),
            running: apiService.running || false,
            ...apiService
          };
        });
        
        // Если API не вернул все сервисы, добавляем недостающие
        if (updatedServices.length < defaultServices.length) {
          defaultServices.forEach(defaultService => {
            if (!updatedServices.find(s => s.name === defaultService.name)) {
              updatedServices.push({
                ...defaultService,
                status: 'unknown',
                health: 'unknown',
                lastChecked: new Date().toLocaleTimeString(),
                running: false
              });
            }
          });
        }
        
        setServices(updatedServices);
      } else {
        // Если API не ответил, используем defaultServices с проверкой портов
        const updatedServices = await Promise.all(
          defaultServices.map(async (service) => {
            try {
              const isRunning = await checkPort(service.port);
              return {
                ...service,
                status: isRunning ? 'running' : 'stopped',
                health: isRunning ? 'healthy' : 'unhealthy',
                lastChecked: new Date().toLocaleTimeString(),
                running: isRunning
              };
            } catch (error) {
              return {
                ...service,
                status: 'error',
                health: 'error',
                lastChecked: new Date().toLocaleTimeString(),
                running: false,
                error: error.message
              };
            }
          })
        );
        setServices(updatedServices);
      }
    } catch (error) {
      console.error('Error fetching services status:', error);
      // Используем defaultServices с статусом ошибки
      const errorServices = defaultServices.map(service => ({
        ...service,
        status: 'error',
        health: 'error',
        lastChecked: new Date().toLocaleTimeString(),
        running: false,
        error: 'API недоступен'
      }));
      setServices(errorServices);
    } finally {
      setLoading(false);
    }
  };

  // Функция проверки порта
  const checkPort = (port) => {
    return new Promise((resolve, reject) => {
      // Эта функция будет заменена вызовом API
      // Пока используем заглушку
      setTimeout(() => {
        // Рандомный статус для демонстрации
        resolve(Math.random() > 0.5);
      }, 100);
    });
  };

  useEffect(() => {
    fetchServicesStatus();
    
    let interval;
    if (autoRefresh) {
      interval = setInterval(fetchServicesStatus, 5000); // Обновлять каждые 5 секунд
    }
    
    return () => clearInterval(interval);
  }, [autoRefresh]);

  const handleServiceAction = async (serviceName, action) => {
    try {
      setLoading(true);
      
      if (action === 'start') {
        await api.startService(serviceName);
      } else if (action === 'stop') {
        await api.stopService(serviceName);
      } else if (action === 'restart') {
        // Перезапуск: остановить, подождать, запустить
        await api.stopService(serviceName);
        await new Promise(resolve => setTimeout(resolve, 2000));
        await api.startService(serviceName);
      }
      
      // Обновляем статус после действия
      setTimeout(() => {
        fetchServicesStatus();
        if (onStatusUpdate) onStatusUpdate();
      }, 3000);
      
    } catch (error) {
      console.error(`Error ${action}ing service:`, error);
      alert(`Ошибка: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Фильтрация сервисов
  const filteredServices = services.filter(service => {
    // Фильтр по статусу
    if (filter === 'running' && service.status !== 'running') return false;
    if (filter === 'stopped' && service.status === 'running') return false;
    if (filter === 'error' && service.status !== 'error') return false;
    
    // Поиск по имени
    if (searchTerm && searchTerm.trim() !== '') {
      const term = searchTerm.toLowerCase();
      return (
        service.name.toLowerCase().includes(term) ||
        service.displayName.toLowerCase().includes(term) ||
        service.port.toString().includes(term) ||
        service.type.toLowerCase().includes(term)
      );
    }
    
    return true;
  });

  const runningCount = services.filter(s => s.status === 'running').length;
  const stoppedCount = services.filter(s => s.status === 'stopped').length;
  const errorCount = services.filter(s => s.status === 'error').length;
  const totalCount = services.length;

  const getServiceIcon = (type) => {
    switch (type) {
      case 'backend': return '⚙️';
      case 'frontend': return '⚛️';
      case 'launcher': return '🧰';
      case 'database': return '🗄️';
      case 'gateway': return '🚪';
      default: return '🔧';
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'running': return '#4CAF50';
      case 'stopped': return '#F44336';
      case 'error': return '#FF9800';
      case 'unknown': return '#9E9E9E';
      default: return '#9E9E9E';
    }
  };

  return (
    <div className="service-monitor">
      <div className="monitor-header">
        <h2>📊 Мониторинг сервисов</h2>
        <div className="monitor-controls">
          <div className="controls-left">
            <button 
              className="btn-refresh" 
              onClick={fetchServicesStatus} 
              disabled={loading}
            >
              {loading ? '🔄 Обновление...' : '🔄 Обновить'}
            </button>
            
            <div className="filter-buttons">
              <button 
                className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
                onClick={() => setFilter('all')}
              >
                Все ({totalCount})
              </button>
              <button 
                className={`filter-btn ${filter === 'running' ? 'active' : ''}`}
                onClick={() => setFilter('running')}
              >
                Запущены ({runningCount})
              </button>
              <button 
                className={`filter-btn ${filter === 'stopped' ? 'active' : ''}`}
                onClick={() => setFilter('stopped')}
              >
                Остановлены ({stoppedCount})
              </button>
              <button 
                className={`filter-btn ${filter === 'error' ? 'active' : ''}`}
                onClick={() => setFilter('error')}
              >
                Ошибки ({errorCount})
              </button>
            </div>
          </div>
          
          <div className="controls-right">
            <div className="search-box">
              <input
                type="text"
                placeholder="Поиск сервиса..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="search-input"
              />
              {searchTerm && (
                <button 
                  className="clear-search"
                  onClick={() => setSearchTerm('')}
                >
                  ✕
                </button>
              )}
            </div>
            
            <label className="auto-refresh-toggle">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
              />
              <span>Автообновление (5 сек)</span>
            </label>
          </div>
        </div>
        
        <div className="status-summary">
          <div className="summary-item running">
            <span className="summary-count">{runningCount}</span>
            <span className="summary-label">Запущено</span>
          </div>
          <div className="summary-item stopped">
            <span className="summary-count">{stoppedCount}</span>
            <span className="summary-label">Остановлено</span>
          </div>
          <div className="summary-item error">
            <span className="summary-count">{errorCount}</span>
            <span className="summary-label">Ошибки</span>
          </div>
          <div className="summary-item total">
            <span className="summary-count">{totalCount}</span>
            <span className="summary-label">Всего</span>
          </div>
        </div>
      </div>

      <div className="services-grid">
        {filteredServices.map((service, index) => (
          <div
            key={`${service.name}-${index}`}
            className={`service-card ${service.status} ${service.type}`}
            style={{ borderLeftColor: getStatusColor(service.status) }}
          >
            <div className="service-header">
              <div className="service-icon">
                {getServiceIcon(service.type)}
              </div>
              <div className="service-info">
                <h3 title={service.displayName}>{service.displayName}</h3>
                <div className="service-meta">
                  <span className="name">ID: {service.name}</span>
                  <span className="port">Порт: {service.port}</span>
                  <span className="type">Тип: {service.type}</span>
                </div>
              </div>
              <div className="status-indicator" style={{ backgroundColor: getStatusColor(service.status) }}>
                {service.status === 'running' && '✅'}
                {service.status === 'stopped' && '❌'}
                {service.status === 'error' && '⚠️'}
                {service.status === 'unknown' && '❓'}
              </div>
            </div>

            <div className="service-details">
              <div className="detail-row">
                <span>Статус:</span>
                <span className={`status-text ${service.status}`}>
                  {service.status === 'running' && 'Запущен'}
                  {service.status === 'stopped' && 'Остановлен'}
                  {service.status === 'error' && 'Ошибка'}
                  {service.status === 'unknown' && 'Неизвестно'}
                </span>
              </div>
              
              <div className="detail-row">
                <span>Здоровье:</span>
                <span className={`health-text ${service.health}`}>
                  {service.health === 'healthy' && 'Здоров'}
                  {service.health === 'unhealthy' && 'Проблемы'}
                  {service.health === 'error' && 'Ошибка'}
                  {service.health === 'unknown' && 'Неизвестно'}
                </span>
              </div>
              
              <div className="detail-row">
                <span>PID:</span>
                <span className="pid-value" title={service.pid || 'Неизвестен'}>
                  {service.pid ? `#${service.pid}` : '—'}
                </span>
              </div>
              
              <div className="detail-row">
                <span>Проверено:</span>
                <span className="check-time">{service.lastChecked}</span>
              </div>
              
              {service.error && (
                <div className="detail-row error-row">
                  <span>Ошибка:</span>
                  <span className="error-message" title={service.error}>
                    {service.error.length > 30 ? `${service.error.substring(0, 30)}...` : service.error}
                  </span>
                </div>
              )}
            </div>

            <div className="service-actions">
              {service.status === 'running' ? (
                <>
                  <button
                    className="btn-stop-service"
                    onClick={() => handleServiceAction(service.name, 'stop')}
                    disabled={loading}
                    title="Остановить сервис"
                  >
                    🛑 Стоп
                  </button>
                  
                  <button
                    className="btn-restart-service"
                    onClick={() => handleServiceAction(service.name, 'restart')}
                    disabled={loading}
                    title="Перезапустить сервис"
                  >
                    🔁 Рестарт
                  </button>
                </>
              ) : (
                <button
                  className="btn-start-service"
                  onClick={() => handleServiceAction(service.name, 'start')}
                  disabled={loading}
                  title="Запустить сервис"
                >
                  🚀 Старт
                </button>
              )}
              
              {/* Кнопка управления */}
              <button
                className="btn-control-service"
                onClick={() => {
                  if (onOpenServiceControl) {
                    onOpenServiceControl({
                      name: service.name,
                      displayName: service.displayName,
                      port: service.port,
                      type: service.type,
                      running: service.status === 'running',
                      pid: service.pid,
                      status: service.status,
                      health: service.health
                    });
                  }
                }}
                title="Расширенное управление"
              >
                🛠️ Управление
              </button>
              
              {/* Быстрые действия */}
              <div className="quick-actions">
                <button
                  className="btn-quick"
                  onClick={() => window.open(`http://localhost:${service.port}`, '_blank')}
                  disabled={service.status !== 'running' || !service.port}
                  title="Открыть в браузере"
                >
                  🌐
                </button>
                
                <button
                  className="btn-quick"
                  onClick={() => window.open(`http://localhost:3333/api/logs/${service.name}`, '_blank')}
                  title="Просмотреть логи"
                >
                  📋
                </button>
                
                <button
                  className="btn-quick"
                  onClick={() => api.killPort(service.port)}
                  title="Освободить порт"
                >
                  🔥
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {filteredServices.length === 0 && (
        <div className="no-services">
          <div className="no-services-icon">🔍</div>
          <h3>Сервисы не найдены</h3>
          <p>Попробуйте изменить фильтры или поисковый запрос</p>
          <button 
            className="btn-reset-filters"
            onClick={() => {
              setFilter('all');
              setSearchTerm('');
            }}
          >
            Сбросить фильтры
          </button>
        </div>
      )}

      <div className="system-health">
        <h3>📈 Общее состояние системы</h3>
        <div className="health-bar-container">
          <div 
            className="health-bar"
            style={{ width: `${(runningCount / totalCount) * 100}%` }}
          ></div>
        </div>
        <div className="health-stats">
          <div className="stat">
            <span className="stat-label">Работает:</span>
            <span className="stat-value running">{runningCount}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Остановлено:</span>
            <span className="stat-value stopped">{stoppedCount}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Ошибки:</span>
            <span className="stat-value error">{errorCount}</span>
          </div>
          <div className="stat">
            <span className="stat-label">Успешность:</span>
            <span className="stat-value percentage">
              {totalCount > 0 ? Math.round((runningCount / totalCount) * 100) : 0}%
            </span>
          </div>
        </div>
        
        <div className="system-actions">
          <button 
            className="btn-system-start"
            onClick={() => api.startCompleteSystem()}
            disabled={loading}
          >
            🚀 Запустить все сервисы
          </button>
          <button 
            className="btn-system-stop"
            onClick={() => api.stopSystem()}
            disabled={loading}
          >
            🛑 Остановить все сервисы
          </button>
          <button 
            className="btn-system-fix"
            onClick={() => api.autoFixPorts()}
            disabled={loading}
          >
            🔧 Авто-исправление
          </button>
        </div>
      </div>
    </div>
  );
};

export default ServiceMonitor;