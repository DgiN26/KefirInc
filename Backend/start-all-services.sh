#!/bin/bash
# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Порядок запуска сервисов (важен!)
SERVICES=(
  "ApiGateWay:8080:API Gateway"
  "Auth:8097:Auth Service"
  "User:8081:User Service"
  "Sklad:8082:Sklad Service"
  #"Delivery:8083:Delivery Service"
  #"Collector:8084:Collector Service"
  #"Backet:8085:Backet Service"
  #"Office:8086:Office Service"  
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
    
    # Проверяем, есть ли mvnw
    if [ -f "mvnw" ]; then
      # Запускаем в фоне
      ./mvnw spring-boot:run -Dspring-boot.run.profiles=local > "../${dir}.log" 2>&1 &
      PID=$!
      echo $PID > "../${dir}.pid"
      echo "PID: $PID"
      
      # Ждем немного
      sleep 5
      
      # Проверяем порт
      check_port $port "$name"
    else
      echo -e "${RED}❌ mvnw не найден в $dir${NC}"
    fi
    
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
echo "	Auth:8097:        http://localhost:8097"
echo "  User Service:     http://localhost:8081"
echo "  Sklad Service:    http://localhost:8082"
echo "  Delivery Service: http://localhost:8083"
echo "  Collector Service: http://localhost:8084"
echo "  Backet Service:   http://localhost:8085"
echo "  Office Service:   http://localhost:8086"
echo ""
echo "📊 Проверить логи: tail -f Backend/*.log"
echo "🛑 Остановить все: ./stop-all-services.sh"
