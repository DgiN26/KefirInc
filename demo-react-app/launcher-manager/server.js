// demo-react-app/launcher-manager/server.js
const express = require('express');
const cors = require('cors');
const { exec } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(cors());
app.use(express.json());

const BASE_DIR = path.join(__dirname, '..', '..'); // KefirInc directory
const BACKEND_DIR = path.join(BASE_DIR, 'Backend');
const KEFIR_FRONTEND_DIR = path.join(BASE_DIR, 'kefir-react-app');
const SCRIPTS_DIR = path.join(__dirname, 'scripts');
const PIDS_DIR = path.join(__dirname, 'pids');
const LOGS_DIR = path.join(__dirname, 'logs');

// Создаем необходимые директории
[PIDS_DIR, LOGS_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// Конфигурация сервисов
const SERVICES = [
  { name: 'ApiGateway', port: 8080, type: 'backend' },
  { name: 'Auth', port: 8097, type: 'backend' },
  { name: 'User', port: 8081, type: 'backend' },
  { name: 'Sklad', port: 8082, type: 'backend' },
  { name: 'Delivery', port: 8088, type: 'backend' },
  { name: 'Collector', port: 8086, type: 'backend' },
  { name: 'Backet', port: 8083, type: 'backend' },
  { name: 'Office', port: 8085, type: 'backend' },
  { name: 'TransactionSaga', port: 8090, type: 'backend' },
  { name: 'KefirFrontend', port: 3000, type: 'frontend', dir: 'kefir-react-app' }
];

// 1. Запуск всей системы
app.post('/api/start-all', (req, res) => {
  console.log('🚀 Запуск всей системы...');
  
  exec(`"${SCRIPTS_DIR}/start-backend-fixed.bat"`, { cwd: SCRIPTS_DIR }, (error, stdout, stderr) => {
    if (error) {
      console.error('❌ Ошибка запуска:', stderr);
      return res.status(500).json({ 
        success: false, 
        error: stderr,
        message: 'Ошибка запуска системы' 
      });
    }
    
    console.log('✅ Система запущена:', stdout);
    res.json({ 
      success: true, 
      message: 'Система запускается...',
      output: stdout 
    });
  });
});

// 2. Остановка всей системы
app.post('/api/stop-all', (req, res) => {
  exec(`"${SCRIPTS_DIR}/stop-all.bat"`, { cwd: SCRIPTS_DIR }, (error, stdout, stderr) => {
    res.json({ 
      success: !error, 
      message: error ? stderr : 'Система остановлена',
      output: stdout 
    });
  });
});

// 3. Запуск отдельного сервиса
app.post('/api/start/:serviceName', (req, res) => {
  const serviceName = req.params.serviceName;
  const service = SERVICES.find(s => s.name === serviceName);
  
  if (!service) {
    return res.status(404).json({ error: 'Сервис не найден' });
  }
  
  exec(`"${SCRIPTS_DIR}/start-service.bat" ${serviceName}`, { cwd: SCRIPTS_DIR }, (error, stdout, stderr) => {
    res.json({ 
      success: !error, 
      message: error ? stderr : `Сервис ${serviceName} запущен`,
      output: stdout 
    });
  });
});

// 4. Остановка отдельного сервиса
app.post('/api/stop/:serviceName', (req, res) => {
  const serviceName = req.params.serviceName;
  
  exec(`"${SCRIPTS_DIR}/stop-service.bat" ${serviceName}`, { cwd: SCRIPTS_DIR }, (error, stdout, stderr) => {
    res.json({ 
      success: !error, 
      message: error ? stderr : `Сервис ${serviceName} остановлен`,
      output: stdout 
    });
  });
});

// 5. Принудительное освобождение порта
app.post('/api/kill-port/:port', (req, res) => {
  const port = req.params.port;
  
  exec(`"${SCRIPTS_DIR}/kill-port.bat" ${port}`, { cwd: SCRIPTS_DIR }, (error, stdout, stderr) => {
    res.json({ 
      success: !error, 
      message: error ? stderr : `Порт ${port} освобожден`,
      output: stdout 
    });
  });
});

// 6. Получение логов сервиса
app.get('/api/logs/:serviceName', (req, res) => {
  const serviceName = req.params.serviceName;
  const logFile = path.join(LOGS_DIR, `${serviceName}.log`);
  
  if (!fs.existsSync(logFile)) {
    return res.json({ logs: 'Логи отсутствуют' });
  }
  
  try {
    const logs = fs.readFileSync(logFile, 'utf8');
    res.json({ logs });
  } catch (error) {
    res.status(500).json({ error: 'Ошибка чтения логов' });
  }
});

// 7. Статус всех сервисов
app.get('/api/status', async (req, res) => {
  const statuses = [];
  
  for (const service of SERVICES) {
    const isRunning = await checkPort(service.port);
    const pid = getPid(service.name);
    
    statuses.push({
      ...service,
      running: isRunning,
      pid: pid,
      lastChecked: new Date().toISOString()
    });
  }
  
  res.json({ 
    services: statuses,
    timestamp: new Date().toISOString(),
    total: SERVICES.length,
    running: statuses.filter(s => s.running).length
  });
});

// Вспомогательные функции
function checkPort(port) {
  return new Promise((resolve) => {
    const net = require('net');
    const socket = new net.Socket();
    
    socket.setTimeout(1000);
    socket.on('connect', () => {
      socket.destroy();
      resolve(true);
    });
    
    socket.on('timeout', () => {
      socket.destroy();
      resolve(false);
    });
    
    socket.on('error', () => {
      resolve(false);
    });
    
    socket.connect(port, 'localhost');
  });
}

function getPid(serviceName) {
  const pidFile = path.join(PIDS_DIR, `${serviceName}.pid`);
  if (fs.existsSync(pidFile)) {
    try {
      return fs.readFileSync(pidFile, 'utf8').trim();
    } catch (e) {
      return null;
    }
  }
  return null;
}

app.listen(3333, () => {
  console.log('🚀 Launcher Manager запущен на http://localhost:3333');
  console.log('📁 Скрипты:', SCRIPTS_DIR);
  console.log('📁 PID файлы:', PIDS_DIR);
});