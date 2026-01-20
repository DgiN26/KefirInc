@echo off
chcp 65001 > nul

echo Остановка всех Java процессов...
taskkill /F /IM java.exe >nul 2>&1
echo Готово!
timeout /t 2