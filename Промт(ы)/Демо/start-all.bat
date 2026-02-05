cd "C:\Users\2oleg\Downloads\Persona 5 Royal (2022)\KefirInc\demo-react-app\launcher-manager\scripts"

(
echo @echo off
echo chcp 65001 ^> nul
echo.
echo ====================================
echo 🚀 ЗАПУСК СИСТЕМЫ KEFIR (упрощенный)
echo ====================================
echo.
echo 📍 Запускаю Launcher Manager API...
echo API доступен на: http://localhost:3333
echo.
echo 📍 Проверьте:
echo 1. kefir-react-app запущен на порту 3000
echo 2. Демо-панель запущена на порту 3099
echo 3. Все бекенд сервисы остановлены
echo.
echo ====================================
echo ✅ Launcher Manager готов к работе!
echo Используйте API endpoints:
echo - /api/start-all
echo - /api/stop-all
echo - /api/status
echo - /api/start/{service}
echo - /api/stop/{service}
echo ====================================
echo.
echo Для запуска бекенд сервисов используйте исходный start.bat файл!
echo Папка бекендов: ..\..\..\Backend\
echo.
pause
) > start-all.bat