// demo-react-app/src/services/api.js
const API_BASE = 'http://localhost:3333/api';

export const api = {
  // ============ ОСНОВНЫЕ МЕТОДЫ ============
  
  // Запуск всей системы
  startCompleteSystem: async () => {
    try {
      const response = await fetch(`${API_BASE}/start-all`, {
        method: 'POST'
      });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка подключения к Launcher Manager'
      };
    }
  },

  // Остановка всей системы
  stopSystem: async () => {
    try {
      const response = await fetch(`${API_BASE}/stop-all`, {
        method: 'POST'
      });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка подключения к Launcher Manager'
      };
    }
  },

  // Перезапуск системы
  restartSystem: async () => {
    try {
      // 1. Останавливаем
      await fetch(`${API_BASE}/stop-all`, { method: 'POST' });
      
      // 2. Ждем 5 секунд
      await new Promise(resolve => setTimeout(resolve, 5000));
      
      // 3. Запускаем
      const response = await fetch(`${API_BASE}/start-all`, { method: 'POST' });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка перезапуска системы'
      };
    }
  },

  // ============ УПРАВЛЕНИЕ СЕРВИСАМИ ============
  
  // Запуск отдельного сервиса
  startService: async (serviceName) => {
    try {
      const response = await fetch(`${API_BASE}/start/${serviceName}`, {
        method: 'POST'
      });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: `Ошибка запуска сервиса ${serviceName}`
      };
    }
  },

  // Остановка отдельного сервиса
  stopService: async (serviceName) => {
    try {
      const response = await fetch(`${API_BASE}/stop/${serviceName}`, {
        method: 'POST'
      });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: `Ошибка остановки сервиса ${serviceName}`
      };
    }
  },

  // ============ РАБОТА С ПОРТАМИ ============
  
  // Освобождение порта (kill процесса)
  killPort: async (port) => {
    try {
      const response = await fetch(`${API_BASE}/kill-port/${port}`, {
        method: 'POST'
      });
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: `Ошибка освобождения порта ${port}`
      };
    }
  },

  // Освобождение всех портов KEFIR
  releasePorts: async () => {
    try {
      const ports = [8080, 8097, 8081, 8082, 8088, 8086, 8083, 8085, 8090, 3000];
      const results = [];
      
      for (const port of ports) {
        try {
          await fetch(`${API_BASE}/kill-port/${port}`, {
            method: 'POST'
          });
          results.push({ port, success: true });
        } catch (error) {
          results.push({ port, success: false, error: error.message });
        }
      }
      
      return {
        success: true,
        message: 'Порты освобождены',
        results: results
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка освобождения портов'
      };
    }
  },

  // Авто-фикс портов (остановить всё + освободить порты)
  autoFixPorts: async () => {
    try {
      // 1. Останавливаем всё
      const stopResponse = await fetch(`${API_BASE}/stop-all`, { method: 'POST' });
      const stopResult = await stopResponse.json();
      
      // 2. Ждем 3 секунды
      await new Promise(resolve => setTimeout(resolve, 3000));
      
      // 3. Освобождаем порты
      const ports = [8080, 8097, 8081, 8082, 8088, 8086, 8083, 8085, 8090, 3000];
      const portResults = [];
      
      for (const port of ports) {
        try {
          await fetch(`${API_BASE}/kill-port/${port}`, {
            method: 'POST'
          });
          portResults.push({ port, success: true });
        } catch (error) {
          portResults.push({ port, success: false, error: error.message });
        }
      }
      
      return {
        success: true,
        message: 'Авто-фикс выполнен. Все сервисы остановлены, порты освобождены.',
        stopResult: stopResult,
        portResults: portResults
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка авто-фикса портов'
      };
    }
  },

  // ============ МОНИТОРИНГ И ИНФОРМАЦИЯ ============
  
  // Статус всех сервисов
  getSystemStatus: async () => {
    try {
      const response = await fetch(`${API_BASE}/status`);
      const data = await response.json();
      
      // Добавляем вычисляемые поля для совместимости
      return {
        ...data,
        systemReady: data.running > 0,  // Хотя бы один сервис запущен
        runningServices: data.running,
        totalServices: data.total,
        services: data.services.reduce((acc, service) => {
          acc[service.name] = service.running;
          return acc;
        }, {})
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        services: {},
        runningServices: 0,
        totalServices: 0,
        systemReady: false,
        message: 'Ошибка получения статуса системы'
      };
    }
  },

  // Быстрая проверка (alias для getSystemStatus)
  quickCheck: async () => {
    return await api.getSystemStatus();
  },

  // Логи сервиса
  getServiceLogs: async (serviceName) => {
    try {
      const response = await fetch(`${API_BASE}/logs/${serviceName}`);
      return await response.json();
    } catch (error) {
      return {
        success: false,
        error: error.message,
        logs: 'Ошибка получения логов'
      };
    }
  },

  // Список всех сервисов
  listAllServices: async () => {
    try {
      const response = await fetch(`${API_BASE}/status`);
      const data = await response.json();
      
      return {
        success: true,
        services: data.services.map(s => ({
          name: s.name,
          displayName: s.name,
          port: s.port,
          type: s.type,
          running: s.running,
          pid: s.pid
        })),
        total: data.total,
        running: data.running
      };
    } catch (error) {
      // Возвращаем стандартный список если API недоступно
      return {
        success: false,
        services: [
          { name: 'ApiGateway', port: 8080, type: 'backend', displayName: 'API Gateway' },
          { name: 'Auth', port: 8097, type: 'backend', displayName: 'Authentication Service' },
          { name: 'User', port: 8081, type: 'backend', displayName: 'User Management' },
          { name: 'Sklad', port: 8082, type: 'backend', displayName: 'Warehouse Service' },
          { name: 'Delivery', port: 8088, type: 'backend', displayName: 'Delivery Service' },
          { name: 'Collector', port: 8086, type: 'backend', displayName: 'Collector Service' },
          { name: 'Backet', port: 8083, type: 'backend', displayName: 'Shopping Cart' },
          { name: 'Office', port: 8085, type: 'backend', displayName: 'Office Management' },
          { name: 'TransactionSaga', port: 8090, type: 'backend', displayName: 'Transaction Saga' },
          { name: 'KefirFrontend', port: 3000, type: 'frontend', displayName: 'Logistics Frontend' }
        ],
        total: 10,
        running: 0,
        message: 'Используется кэшированный список сервисов'
      };
    }
  },

  // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============
  
  // Проверка здоровья Launcher Manager
  checkLauncherHealth: async () => {
    try {
      const response = await fetch(`${API_BASE}/status`);
      return {
        success: response.ok,
        message: response.ok ? 'Launcher Manager работает' : 'Launcher Manager не отвечает',
        status: response.status
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Launcher Manager недоступен'
      };
    }
  },

  // Полная проверка системы
  fullSystemCheck: async () => {
    try {
      // 1. Проверяем Launcher Manager
      const launcherHealth = await api.checkLauncherHealth();
      
      // 2. Получаем статус сервисов
      const systemStatus = await api.getSystemStatus();
      
      // 3. Проверяем ключевые порты
      const criticalPorts = [
        { name: 'API Gateway', port: 8080 },
        { name: 'Auth Service', port: 8097 },
        { name: 'Frontend', port: 3000 },
        { name: 'Launcher API', port: 3333 }
      ];
      
      const portChecks = await Promise.all(
        criticalPorts.map(async (service) => {
          try {
            const response = await fetch(`http://localhost:${service.port}`, { 
              method: 'HEAD',
              mode: 'no-cors'
            }).catch(() => null);
            return {
              ...service,
              reachable: true
            };
          } catch {
            return {
              ...service,
              reachable: false
            };
          }
        })
      );
      
      return {
        success: launcherHealth.success && systemStatus.running > 0,
        launcherHealth,
        systemStatus,
        portChecks,
        timestamp: new Date().toISOString(),
        recommendations: portChecks.filter(p => !p.reachable).map(p => 
          `⚠️ ${p.name} (порт ${p.port}) недоступен`
        )
      };
    } catch (error) {
      return {
        success: false,
        error: error.message,
        message: 'Ошибка полной проверки системы'
      };
    }
  }
};