#!/bin/bash

echo "🛑 Остановка всех сервисов KEFIR..."

# Останавливаем процессы по PID файлам
for pid_file in Backend/*.pid; do
  if [ -f "$pid_file" ]; then
    pid=$(cat "$pid_file")
    if kill $pid 2>/dev/null; then
      echo "✅ Остановлен процесс $pid"
    fi
    rm "$pid_file"
  fi
done

# Убиваем все процессы maven
pkill -f "spring-boot:run" 2>/dev/null
pkill -f "mvnw" 2>/dev/null

echo "🎯 Все сервисы остановлены"
