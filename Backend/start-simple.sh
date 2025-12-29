#!/bin/bash

echo "🚀 Запуск системы KEFIR Logistics..."
echo "======================================"

# Останавливаем все предыдущие процессы
pkill -f "spring-boot:run" 2>/dev/null

# Запускаем только существующие сервисы
cd ~/Desktop/Kefir/Backend

# API Gateway
if [ -d "ApiGateWay" ]; then
    echo "▶ Запуск API Gateway..."
    cd ApiGateWay
    # Создаем конфигурацию для отключения БД
    cat > src/main/resources/application-local.properties << 'PROP'
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
server.port=8080
logging.level.root=INFO
PROP
    mvn spring-boot:run -Dspring-boot.run.profiles=local > ../ApiGateWay.log 2>&1 &
    echo $! > ../ApiGateWay.pid
    echo "✅ API Gateway запущен (порт: 8080)"
    cd ..
fi

# Ждем запуска API Gateway
sleep 15

# Запускаем остальные сервисы, если они есть
for dir in User Sklad Delivery Collector Backet Office; do
    if [ -d "$dir" ]; then
        echo "▶ Запуск $dir Service..."
        cd "$dir"
        mvn spring-boot:run > "../${dir}.log" 2>&1 &
        echo $! > "../${dir}.pid"
        echo "✅ $dir Service запущен"
        cd ..
        sleep 5
    fi
done

echo ""
echo "======================================"
echo "🎉 Сервисы запущены!"
echo "======================================"
echo ""
echo "📡 Проверить состояние: ./check-services.sh"
echo "📊 Просмотреть логи: tail -f Backend/*.log"
