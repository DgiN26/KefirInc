package com.example.delivery;

    enum DeliveryStatus {
        PENDING,        // Ожидает назначения
        ASSIGNED,       // Назначен курьеру
        PICKED_UP,      // Забран со склада
        IN_TRANSIT,     // В пути
        OUT_FOR_DELIVERY, // Доставляется
        DELIVERED,      // Доставлен
        FAILED,         // Не удалось доставить
        CANCELLED       // Отменен
    }

