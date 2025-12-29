bat
@echo off
echo 🔧 Финальный запуск Collector...
echo.

cd Backend\Collector

echo 1. Очистка...
call mvn clean -q

echo 2. Создание конфигурации...
(
echo server.port=8086
echo spring.autoconfigure.exclude[0]=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
echo spring.autoconfigure.exclude[1]=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
echo spring.autoconfigure.exclude[2]=org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
) > src\main\resources\application.properties

echo 3. Компиляция...
call mvn compile -q

echo 4. Запуск...
echo.
echo 📍 Collector будет доступен на: http://localhost:8086
echo.
echo Если не запускается, попробуйте:
echo mvn spring-boot:run -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
echo.

mvn spring-boot:run