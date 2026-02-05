import React from 'react';
import './UserInterfaces.css';

const UserInterfaces = () => {
  const interfaces = [
    {
      id: 'client',
      name: '👤 Клиентское приложение',
      url: 'http://localhost:3000',
      description: 'Создание заказов, отслеживание доставки, история покупок',
      role: 'Клиент',
      port: 3000,
      color: '#4CAF50',
      icon: '🛒'
    },
    {
      id: 'collector',
      name: '👷 Интерфейс сборщика',
      url: 'http://localhost:3000/collector',
      description: 'Сборка заказов на складе, сканирование товаров, отчеты',
      role: 'Сборщик',
      port: 3000,
      color: '#FF9800',
      icon: '📦'
    },
    {
      id: 'office',
      name: '👨‍💼 Офисный интерфейс',
      url: 'http://localhost:3000/office',
      description: 'Управление проблемами, звонки клиентам, мониторинг заказов',
      role: 'Офис-менеджер',
      port: 3000,
      color: '#2196F3',
      icon: '📞'
    },
    {
      id: 'admin',
      name: '📊 Административная панель',
      url: 'http://localhost:3000/admin',
      description: 'Статистика, управление пользователями, системные настройки',
      role: 'Администратор',
      port: 3000,
      color: '#9C27B0',
      icon: '⚙️'
    },
    {
      id: 'launcher',
      name: '🧰 Launcher API',
      url: 'http://localhost:8099',
      description: 'API управления системой, статус сервисов, логи',
      role: 'Разработчик',
      port: 8099,
      color: '#607D8B',
      icon: '🔧'
    },
    {
      id: 'swagger',
      name: '📚 Swagger UI',
      url: 'http://localhost:8099/swagger-ui.html',
      description: 'Документация API, тестирование endpoints',
      role: 'Разработчик',
      port: 8099,
      color: '#009688',
      icon: '📖'
    },
    {
      id: 'demo',
      name: '🎯 Демо-панель',
      url: 'http://localhost:3099',
      description: 'Это приложение - управление демонстрацией',
      role: 'Демонстратор',
      port: 3099,
      color: '#FF5722',
      icon: '🎬'
    }
  ];

  const openInterface = (url, name) => {
    window.open(url, '_blank');
    console.log(`Открыт интерфейс: ${name}`);
  };

  const openAllInterfaces = () => {
    interfaces.forEach(ui => {
      window.open(ui.url, '_blank');
    });
  };

  return (
    <div className="user-interfaces">
      <div className="interfaces-header">
        <h2>🖥️ Пользовательские интерфейсы</h2>
        <div className="header-actions">
          <button className="btn-open-all" onClick={openAllInterfaces}>
            🌐 ОТКРЫТЬ ВСЕ ИНТЕРФЕЙСЫ
          </button>
          <div className="interfaces-count">
            <span className="count">Всего: {interfaces.length}</span>
          </div>
        </div>
      </div>

      <div className="interfaces-grid">
        {interfaces.map((ui) => (
          <div
            key={ui.id}
            className="interface-card"
            style={{ borderTopColor: ui.color }}
          >
            <div className="card-header">
              <div className="interface-icon" style={{ color: ui.color }}>
                {ui.icon}
              </div>
              <div className="interface-title">
                <h3>{ui.name}</h3>
                <div className="interface-meta">
                  <span className="role">Роль: {ui.role}</span>
                  <span className="port">Порт: {ui.port}</span>
                </div>
              </div>
            </div>

            <div className="card-body">
              <p className="description">{ui.description}</p>
              <div className="url-display">
                <span className="url-label">URL:</span>
                <code className="url-value">{ui.url}</code>
              </div>
            </div>

            <div className="card-footer">
              <button
                className="btn-open-interface"
                onClick={() => openInterface(ui.url, ui.name)}
                style={{ backgroundColor: ui.color }}
              >
                Открыть {ui.name}
              </button>
              
              <div className="quick-actions">
                <button
                  className="btn-quick"
                  onClick={() => navigator.clipboard.writeText(ui.url)}
                  title="Скопировать URL"
                >
                  📋
                </button>
                <button
                  className="btn-quick"
                  onClick={() => window.open(`${ui.url}/health`, '_blank')}
                  title="Проверить здоровье"
                >
                  💚
                </button>
                {ui.id === 'launcher' && (
                  <button
                    className="btn-quick"
                    onClick={() => window.open(`${ui.url}/api/v1/services/system-status`, '_blank')}
                    title="Статус системы"
                  >
                    📊
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="interface-groups">
        <div className="group">
          <h3>👥 По ролям:</h3>
          <div className="role-buttons">
            <button onClick={() => {
              interfaces
                .filter(ui => ui.role === 'Клиент')
                .forEach(ui => openInterface(ui.url, ui.name));
            }}>
              👤 Клиентские
            </button>
            <button onClick={() => {
              interfaces
                .filter(ui => ui.role === 'Сборщик' || ui.role === 'Офис-менеджер')
                .forEach(ui => openInterface(ui.url, ui.name));
            }}>
              👨‍💼 Операционные
            </button>
            <button onClick={() => {
              interfaces
                .filter(ui => ui.role.includes('Разработчик') || ui.role === 'Администратор')
                .forEach(ui => openInterface(ui.url, ui.name));
            }}>
              👨‍💻 Технические
            </button>
          </div>
        </div>

        <div className="group">
          <h3>⚡ Быстрый запуск:</h3>
          <div className="quick-launch">
            <button onClick={() => {
              openInterface('http://localhost:3000', 'Клиент');
              openInterface('http://localhost:3000/collector', 'Сборщик');
              openInterface('http://localhost:3000/office', 'Офис');
            }}>
              🏢 Полный рабочий процесс
            </button>
            <button onClick={() => {
              openInterface('http://localhost:8099', 'API');
              openInterface('http://localhost:8099/swagger-ui.html', 'Swagger');
            }}>
              🔧 Разработка и отладка
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserInterfaces;