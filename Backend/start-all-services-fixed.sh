#!/bin/bash

echo "🚀 Запуск системы KEFIR Logistics..."
echo "======================================"

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Переходим в директорию Backend
cd ~/Desktop/Kefir/Backend || { echo -e "${RED}❌ Папка Backend не найдена${NC}"; exit 1; }

# Порядок запуска сервисов (важен!)
SERVICES=(
  "ApiGateWay:8080:API Gateway"
  "User:8081:User Service"
  "Sklad:8082:Sklad Service"
  "Backet:8085:Backet Service"
  "Collector:8084:Collector Service"
  "Delivery:8083:Delivery Service"
  "Office:8086:Office Service"
)

# Функция для проверки порта
check_port() {
  local port=$1
  local service=$2
  for i in {1..30}; do
    if nc -z localhost $port 2>/dev/null; then
      echo -e "${GREEN}✅ $service запущен на порту $port${NC}"
      return 0
    fi
    sleep 2
  done
  echo -e "${RED}❌ $service не запустился на порту $port${NC}"
  return 1
}

# Запускаем каждый сервис в фоне
for service_info in "${SERVICES[@]}"; do
  dir=$(echo $service_info | cut -d: -f1)
  port=$(echo $service_info | cut -d: -f2)
  name=$(echo $service_info | cut -d: -f3)
  
  echo -e "\n${YELLOW}▶ Запуск $name...${NC}"
  
  if [ -d "$dir" ]; then
    cd "$dir"
    
    # Сначала собираем проект
    echo "🔄 Сборка проекта..."
    mvn clean compile -q
    
    # Создаем временный application-local.properties для отключения БД
    if [ "$dir" == "ApiGateWay" ]; then
      cat > src/main/resources/application-local.properties << 'PROP'
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
server.port=8080
logging.level.root=INFO
PROP
    fi
    
    # Запускаем в фоне
    mvn spring-boot:run -Dspring-boot.run.profiles=local > "../${dir}.log" 2>&1 &
    PID=$!
    echo $PID > "../${dir}.pid"
    echo "PID: $PID"
    
    # Ждем немного
    sleep 10
    
    # Проверяем порт
    check_port $port "$name"
    
    cd ..
  else
    echo -e "${RED}❌ Папка $dir не найдена${NC}"
  fi
done

echo -e "\n${GREEN}======================================${NC}"
echo -e "${GREEN}🎉 Все сервисы запущены!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "📡 Доступные сервисы:"
echo "  API Gateway:      http://localhost:8080"
echo "  User Service:     http://localhost:8081"
echo "  Sklad Service:    http://localhost:8082"
echo "  Backet Service:   http://localhost:8085"
echo "  Collector Service: http://localhost:8084"
echo "  Delivery Service: http://localhost:8083"
echo "  Office Service:   http://localhost:8086"
echo ""
echo "📊 Проверить логи: tail -f ~/Desktop/Kefir/Backend/*.log"
echo "🛑 Остановить все: ./stop-all-services.sh"
