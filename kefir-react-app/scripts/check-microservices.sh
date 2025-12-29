# scripts/check-microservices.sh
#!/bin/bash

echo "🔍 Проверка доступности микросервисов KEFIR..."

SERVICES=(
  "API Gateway:8080"
  "User Service:8081" 
  "Sklad Service:8082"
  "Delivery Service:8083"
  "Collector Service:8084"
  "Backet Service:8085"
  "Office Service:8086"
)

echo "📡 Основные endpoints:"
echo "API Gateway: http://localhost:8080"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "Eureka Dashboard: http://localhost:8761"

echo ""
echo "🧪 Проверка доступности:"

for service in "${SERVICES[@]}"; do
  name=$(echo $service | cut -d: -f1)
  port=$(echo $service | cut -d: -f2)
  
  if curl -s --head --connect-timeout 3 "http://localhost:$port/actuator/health" > /dev/null; then
    echo "✅ $name (порт $port) - ДОСТУПЕН"
  else
    echo "❌ $name (порт $port) - НЕ ДОСТУПЕН"
  fi
done

echo ""
echo "🚀 Для запуска всех сервисов выполните:"
echo "cd ~/Desktop/Kefir/Backend"
echo "./run-all-services.sh  # если у вас есть скрипт"