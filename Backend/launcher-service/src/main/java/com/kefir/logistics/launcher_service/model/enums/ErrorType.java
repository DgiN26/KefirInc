package com.kefir.logistics.launcher_service.model.enums;

public enum ErrorType {
    PRODUCT_NOT_FOUND("Товар отсутствует на складе"),
    LOW_STOCK("Недостаточное количество товара"),
    TRANSACTION_TIMEOUT("Таймаут транзакции"),
    WAREHOUSE_CLOSED("Склад закрыт"),
    DELIVERY_FAILED("Ошибка доставки"),
    PAYMENT_FAILED("Ошибка оплаты");

    private final String description;

    ErrorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}