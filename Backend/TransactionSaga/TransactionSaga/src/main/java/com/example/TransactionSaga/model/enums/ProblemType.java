package com.example.TransactionSaga.model.enums;

public enum ProblemType {
    ITEM_NOT_FOUND("Товар не найден на складе"),
    ITEM_DAMAGED("Товар поврежден"),
    WRONG_ITEM("Неверный товар"),
    QUANTITY_MISMATCH("Несоответствие количества"),
    LOCATION_ERROR("Ошибка расположения"),
    SYSTEM_ERROR("Системная ошибка"),
    QUALITY_ISSUE("Проблема с качеством"),
    EXPIRED_ITEM("Просроченный товар");

    private final String description;

    ProblemType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCritical() {
        return this == ITEM_NOT_FOUND || this == SYSTEM_ERROR;
    }

    public boolean requiresClientDecision() {
        return this == ITEM_NOT_FOUND || this == ITEM_DAMAGED || this == WRONG_ITEM;
    }
}