// src/setupProxy.js - ИСПРАВЛЕННАЯ ВЕРСИЯ
const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Аутентификация через API Gateway
  app.use(
    '/api/auth',
    createProxyMiddleware({
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
      onProxyReq: (proxyReq, req, res) => {
        console.log(`[PROXY] ${req.method} ${req.path} → Auth Service (8080)`);
      }
    })
  );
  
  // Клиенты через сервис клиентов (8081)
  app.use(
    '/api/clients',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
      onProxyReq: (proxyReq, req, res) => {
        console.log(`[PROXY] ${req.method} ${req.path} → Clients Service (8081)`);
      }
    })
  );
  
  // Другие сервисы...
  app.use(
    '/api/products',
    createProxyMiddleware({
      target: 'http://localhost:8082', // если есть
      changeOrigin: true,
      secure: false,
    })
  );
  
  // Все остальное через API Gateway
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
      onProxyReq: (proxyReq, req, res) => {
        console.log(`[PROXY] ${req.method} ${req.path} → API Gateway (8080)`);
      }
    })
  );
};