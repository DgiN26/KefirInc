# ~/Desktop/Kefir/Makefile
.PHONY: start-all stop-all build-all

start-all:
	@echo "🚀 Запуск всех микросервисов KEFIR..."
	cd Backend/ApiGateWay && ./mvnw spring-boot:run &
	cd Backend/User && ./mvnw spring-boot:run &
	cd Backend/Sklad && ./mvnw spring-boot:run &
	cd Backend/Delivery && ./mvnw spring-boot:run &
	cd Backend/Collector && ./mvnw spring-boot:run &
	cd Backend/Backet && ./mvnw spring-boot:run &
	cd Backend/Office && ./mvnw spring-boot:run &
	
stop-all:
	@echo "🛑 Остановка всех сервисов..."
	pkill -f "spring-boot:run"

build-all:
	@echo "🏗️  Сборка всех микросервисов..."
	cd Backend && find . -name "pom.xml" -exec dirname {} \; | xargs -I {} sh -c 'cd {} && ./mvnw clean package'

check-ports:
	@echo "🔍 Проверка занятых портов..."
	netstat -tulpn | grep -E ':8080|:8081|:8082|:8083|:8084|:8085|:8086|:8761'