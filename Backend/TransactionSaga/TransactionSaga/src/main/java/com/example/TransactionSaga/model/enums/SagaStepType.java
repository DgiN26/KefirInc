package com.example.TransactionSaga.model.enums;

public enum SagaStepType {
    // Инициализация
    VALIDATE_ORDER("Проверка заказа"),
    RESERVE_INVENTORY("Резервирование товаров"),
    CREATE_PICKING_TASK("Создание задачи сборки"),

    // Основной процесс
    START_PICKING("Начало сборки"),
    SCAN_ITEM("Сканирование товара"),
    VERIFY_ITEM("Проверка товара"),

    // Обработка проблем
    REPORT_PROBLEM("Сообщение о проблеме"),
    NOTIFY_OFFICE("Уведомление офиса"),
    NOTIFY_CLIENT("Уведомление клиента"),
    AWAIT_CLIENT_DECISION("Ожидание решения клиента"),
    PROCESS_CLIENT_DECISION("Обработка решения клиента"),

    // Завершение
    UPDATE_INVENTORY("Обновление остатков"),
    CREATE_DELIVERY("Создание доставки"),
    FINALIZE_TRANSACTION("Завершение транзакции"),

    // Компенсация
    RELEASE_INVENTORY("Освобождение товаров"),
    CANCEL_PICKING_TASK("Отмена задачи сборки"),
    CANCEL_DELIVERY("Отмена доставки"),
    REFUND_PAYMENT("Возврат оплаты");

    private final String description;

    SagaStepType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompensationStep() {
        return this == RELEASE_INVENTORY ||
                this == CANCEL_PICKING_TASK ||
                this == CANCEL_DELIVERY ||
                this == REFUND_PAYMENT;
    }

    public boolean isCriticalStep() {
        return this == RESERVE_INVENTORY ||
                this == CREATE_PICKING_TASK ||
                this == FINALIZE_TRANSACTION;
    }
}