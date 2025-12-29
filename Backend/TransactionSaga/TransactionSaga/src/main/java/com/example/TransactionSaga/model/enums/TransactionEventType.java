package com.example.TransactionSaga.model.enums;

public enum TransactionEventType {
    // События инициализации
    TRANSACTION_CREATED("Транзакция создана"),
    TRANSACTION_STARTED("Транзакция начата"),

    // События процесса
    ITEM_SCANNED("Товар отсканирован"),
    ITEM_VERIFIED("Товар проверен"),

    // События проблем
    PROBLEM_DETECTED("Обнаружена проблема"),
    TRANSACTION_PAUSED("Транзакция приостановлена"),
    CLIENT_NOTIFIED("Клиент уведомлен"),
    CLIENT_RESPONDED("Клиент ответил"),

    // События решений
    CLIENT_DECISION_RECEIVED("Получено решение клиента"),
    OFFICE_DECISION_RECEIVED("Получено решение офиса"),

    // События возобновления
    TRANSACTION_RESUMED("Транзакция возобновлена"),

    // События компенсации
    COMPENSATION_STARTED("Начата компенсация"),
    COMPENSATION_STEP_COMPLETED("Шаг компенсации завершен"),
    COMPENSATION_COMPLETED("Компенсация завершена"),

    // События завершения
    TRANSACTION_COMPLETED("Транзакция завершена"),
    TRANSACTION_CANCELLED("Транзакция отменена"),
    TRANSACTION_TIMEOUT("Транзакция просрочена"),

    // Системные события
    RETRY_ATTEMPT("Попытка повтора"),
    STATUS_CHANGED("Статус изменен"),
    ERROR_OCCURRED("Произошла ошибка");

    private final String description;

    TransactionEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isErrorEvent() {
        return this == ERROR_OCCURRED || this == PROBLEM_DETECTED;
    }

    public boolean isCompletionEvent() {
        return this == TRANSACTION_COMPLETED ||
                this == TRANSACTION_CANCELLED ||
                this == TRANSACTION_TIMEOUT;
    }
}