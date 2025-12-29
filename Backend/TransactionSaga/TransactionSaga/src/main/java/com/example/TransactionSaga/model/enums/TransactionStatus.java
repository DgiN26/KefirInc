package com.example.TransactionSaga.model.enums;

public enum TransactionStatus {
    CREATED,        // Транзакция создана
    ACTIVE,         // В процессе сборки
    PAUSED,         // Приостановлена (товара нет)
    WAITING_CLIENT, // Ожидание решения клиента
    WAITING_OFFICE, // Ожидание обработки офисом
    COMPENSATING,   // В процессе отката
    COMPLETED,      // Успешно завершена
    CANCELLED,      // Отменена
    TIMEOUT         // Просрочена по таймауту
}