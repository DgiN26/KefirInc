import React, { useState, useEffect } from 'react';
import SystemControls from './SystemControls';
import ServiceMonitor from './ServiceMonitor';
import DemoScenarios from './DemoScenarios';
import UserInterfaces from './UserInterfaces';
import QuickActions from './QuickActions';
import ServiceControlModal from './ServiceControlModal';
import { api } from '../services/api';
import './Dashboard.css';

const Dashboard = () => {
  const [systemStatus, setSystemStatus] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');
  const [lastUpdate, setLastUpdate] = useState(null);
  const [selectedService, setSelectedService] = useState(null);
  const [showServiceModal, setShowServiceModal] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchSystemStatus = async () => {
    try {
      const status = await api.getSystemStatus();
      setSystemStatus(status);
      setLastUpdate(new Date().toLocaleTimeString());
    } catch (error) {
      console.error('Error fetching system status:', error);
      // Создаем заглушку если API не отвечает
      setSystemStatus({
        systemReady: false,
        runningServices: 0,
        totalServices: 10,
        services: []
      });
    }
  };

  useEffect(() => {
    fetchSystemStatus();
    const interval = setInterval(fetchSystemStatus, 10000); // Обновлять каждые 10 секунд
    return () => clearInterval(interval);
  }, []);

  const handleSystemStart = (result) => {
    console.log('System started:', result);
    fetchSystemStatus();
  };

  // Функция для открытия модального окна управления сервисом
  const openServiceControl = (service) => {
    setSelectedService(service);
    setShowServiceModal(true);
  };

  // Функция для открытия общего управления системой
  const openSystemControl = () => {
    openServiceControl({
      name: 'SystemOverview',
      port: 0,
      type: 'system',
      running: systemStatus?.systemReady || false
    });
  };

  const tabs = [
    { id: 'overview', name: '📊 Обзор', icon: '🏠' },
    { id: 'control', name: '🚀 Управление', icon: '⚙️' },
    { id: 'monitor', name: '📈 Мониторинг', icon: '👁️' },
    { id: 'demos', name: '🎬 Демо', icon: '🎯' },
    { id: 'interfaces', name: '🖥️ Интерфейсы', icon: '💻' }
  ];

  const renderTabContent = () => {
    switch (activeTab) {
      case 'overview':
        return (
          <div className="overview-tab">
            <div className="overview-header">
              <h1>🧰 KEFIR Demo Control Panel</h1>
              <p className="subtitle">Центр управления демонстрацией логистической системы</p>
              
              {systemStatus && (
                <div className="system-overview">
                  <div className={`overview-card ${systemStatus.systemReady ? 'ready' : 'not-ready'}`}>
                    <div className="card-icon">
                      {systemStatus.systemReady ? '✅' : '⚠️'}
                    </div>
                    <div className="card-content">
                      <h3>Состояние системы</h3>
                      <p>{systemStatus.systemReady ? 'Готова к демонстрации' : 'Требуется настройка'}</p>
                      <div className="status-details">
                        <span className="service-count">
                          Сервисы: {systemStatus.runningServices || 0}/{systemStatus.totalServices || 10}
                        </span>
                        <span className="update-time">
                          Обновлено: {lastUpdate}
                        </span>
                      </div>
                    </div>
                    
                    {/* Кнопка управления системой */}
                    <button 
                      className="btn-service-control"
                      onClick={openSystemControl}
                      disabled={loading}
                    >
                      {loading ? '🔄 Загрузка...' : '🛠️ Управление сервисами'}
                    </button>
                  </div>
                  
                  <QuickActions onStatusUpdate={fetchSystemStatus} />
                </div>
              )}
            </div>
            
            <div className="overview-grid">
              <div className="overview-section">
                <SystemControls 
                  onSystemStart={handleSystemStart}
                  onStatusUpdate={fetchSystemStatus}
                  onOpenServiceControl={openServiceControl}
                />
              </div>
              <div className="overview-section">
                <DemoScenarios />
              </div>
            </div>
          </div>
        );
        
      case 'control':
        return (
          <SystemControls 
            onSystemStart={handleSystemStart}
            onStatusUpdate={fetchSystemStatus}
            onOpenServiceControl={openServiceControl}
          />
        );
        
      case 'monitor':
        return (
          <ServiceMonitor 
            onOpenServiceControl={openServiceControl}
            onStatusUpdate={fetchSystemStatus}
          />
        );
        
      case 'demos':
        return <DemoScenarios />;
        
      case 'interfaces':
        return <UserInterfaces />;
        
      default:
        return (
          <SystemControls 
            onSystemStart={handleSystemStart}
            onStatusUpdate={fetchSystemStatus}
            onOpenServiceControl={openServiceControl}
          />
        );
    }
  };

  return (
    <div className="dashboard">
      <div className="dashboard-sidebar">
        <div className="sidebar-header">
          <div className="logo">
            <span className="logo-icon">🚚</span>
            <span className="logo-text">KEFIR</span>
          </div>
          <div className="system-info">
            {systemStatus && (
              <>
                <div className="info-item">
                  <span className="label">Статус:</span>
                  <span className={`value ${systemStatus.systemReady ? 'ready' : 'not-ready'}`}>
                    {systemStatus.systemReady ? '✅ Готов' : '⚠️ Не готов'}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Сервисы:</span>
                  <span className="value">
                    {systemStatus.runningServices || 0}/{systemStatus.totalServices || 10}
                  </span>
                </div>
                <div className="info-item">
                  <span className="label">Launcher:</span>
                  <span className="value">
                    {systemStatus.timestamp ? '✅ Онлайн' : '❌ Оффлайн'}
                  </span>
                </div>
              </>
            )}
          </div>
        </div>
        
        <nav className="sidebar-nav">
          {tabs.map(tab => (
            <button
              key={tab.id}
              className={`nav-item ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span className="nav-icon">{tab.icon}</span>
              <span className="nav-text">{tab.name}</span>
            </button>
          ))}
        </nav>
        
        <div className="sidebar-footer">
          <div className="footer-info">
            <p>Порт: 3099</p>
            <p>Версия: 2.0.0</p>
            <p>API: 3333</p>
            <button 
              className="btn-refresh"
              onClick={() => {
                setLoading(true);
                fetchSystemStatus().finally(() => setLoading(false));
              }}
              disabled={loading}
            >
              {loading ? '🔄 Обновление...' : '🔄 Обновить'}
            </button>
          </div>
        </div>
      </div>
      
      <div className="dashboard-main">
        <div className="main-header">
          <div className="header-left">
            <h2>{tabs.find(t => t.id === activeTab)?.name || 'Панель управления'}</h2>
            {lastUpdate && (
              <span className="last-update">Обновлено: {lastUpdate}</span>
            )}
          </div>
          <div className="header-right">
            <button 
              className="btn-help"
              onClick={() => window.open('http://localhost:3333/api/status', '_blank')}
            >
              📊 Статус API
            </button>
            <button 
              className="btn-logs"
              onClick={() => window.open('http://localhost:3333/api/logs/System', '_blank')}
            >
              📋 Логи системы
            </button>
            <button 
              className="btn-service-manager"
              onClick={openSystemControl}
            >
              🛠️ Управление
            </button>
          </div>
        </div>
        
        <div className="main-content">
          {renderTabContent()}
        </div>
        
        <div className="main-footer">
          <div className="footer-stats">
            <span>KEFIR Logistics Demo System</span>
            <span>•</span>
            <span>Launcher: порт 3333</span>
            <span>•</span>
            <span>Frontend: порт 3000</span>
            <span>•</span>
            <span>Демо-панель: порт 3099</span>
          </div>
          <div className="footer-actions">
            <button 
              className="btn-small"
              onClick={() => window.open('http://localhost:3000', '_blank')}
            >
              🌐 Открыть логистику
            </button>
            <button 
              className="btn-small"
              onClick={() => window.open('http://localhost:3333', '_blank')}
            >
              🔧 API управления
            </button>
            <button 
              className="btn-small"
              onClick={() => window.open('http://localhost:3333/api/status', '_blank')}
            >
              📈 Статус
            </button>
          </div>
        </div>
      </div>
      
      {/* Модальное окно управления сервисом */}
      <ServiceControlModal
        isOpen={showServiceModal}
        onClose={() => setShowServiceModal(false)}
        service={selectedService}
      />
    </div>
  );
};

export default Dashboard;