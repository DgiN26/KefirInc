#!/bin/bash

echo "🚀 Запуск системы KEFIR Logistics..."
echo "======================================"

# Переходим в Backend
cd ~/Desktop/Kefir/Backend || { echo "❌ Папка Backend не найдена"; exit 1; }

# Функция запуска сервиса
start_service() {
    local dir=$1
    local port=$2
    local name=$3
    
    echo "▶ Запуск $name..."
    
    if [ -d "$dir" ]; then
        cd "$dir"
        
        # Для API Gateway создаем специальную конфигурацию
        if [ "$dir" == "ApiGateWay" ]; then
            echo "🔄 Создание конфигурации для API Gateway..."
            cat > src/main/resources/application-local.properties << 'PROP'
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
server.port=8080
logging.level.root=INFO
spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false
PROP
        fi
        
        # Запускаем сервис
        echo "🚀 Запуск $name..."
        mvn spring-boot:run -Dspring-boot.run.profiles=local > "../${dir}.log" 2>&1 &
        echo $! > "../${dir}.pid"
        echo "✅ $name запускается (PID: $!, порт: $port)"
        
        cd ..
    else
        echo "❌ Папка $dir не найдена"
    fi
}

# Запускаем сервисы с задержкой
start_service "ApiGateWay" "8080" "API Gateway"
sleep 15

start_service "User" "8081" "User Service"
sleep 5

start_service "Sklad" "8082" "Sklad Service"
sleep 5

start_service "Delivery" "8083" "Delivery Service"
sleep 5

start_service "Collector" "8084" "Collector Service"
sleep 5

start_service "Backet" "8085" "Backet Service"
sleep 5

start_service "Office" "8086" "Office Service"

echo ""
echo "======================================"
echo "🎉 Все сервисы запускаются!"
echo "======================================"
echo ""
echo "⏳ Подождите 30 секунд для полного запуска..."
echo "📊 Проверить логи: tail -f ~/Desktop/Kefir/Backend/*.log"
echo "🛑 Остановить все: pkill -f 'spring-boot:run'"
