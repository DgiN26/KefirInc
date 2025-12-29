package com.example.TransactionSaga.model.enums;

public enum ClientDecision {
    CONTINUE("Продолжить без отсутствующего товара", "continue"),
    CANCEL("Отменить весь заказ", "cancel"),
    WAIT("Подождать появления товара", "wait"),
    PARTIAL("Частичная отмена", "partial"),
    SUBSTITUTE("Заменить товар", "substitute");

    private final String description;
    private final String code;

    ClientDecision(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public boolean requiresCompensation() {
        return this == CANCEL || this == PARTIAL;
    }

    public boolean allowsContinuation() {
        return this == CONTINUE || this == SUBSTITUTE;
    }
}