// scripts/check-env.js
console.log('=== Проверка переменных окружения ===');
console.log('REACT_APP_USE_REAL_API:', process.env.REACT_APP_USE_REAL_API);
console.log('REACT_APP_API_GATEWAY:', process.env.REACT_APP_API_GATEWAY);
console.log('NODE_ENV:', process.env.NODE_ENV);

// Проверяем, загружены ли переменные из .env
require('dotenv').config();
console.log('\nПосле загрузки .env:');
console.log('REACT_APP_USE_REAL_API:', process.env.REACT_APP_USE_REAL_API);