package com.kefir.logistics.launcher_service.model.enums;

public enum DemoScenarioType {
    NORMAL_PROCESS("Нормальный процесс заказа"),
    SINGLE_MISSING_ITEM("Один отсутствующий товар"),
    CASCADE_ERRORS("Каскадные ошибки (Главный демо)"),
    CLIENT_DEMANDS_ALL("Клиент требует все товары"),
    NIGHTMARE_SCENARIO("Ночной кошмар (магазины закрыты)");

    private final String description;

    DemoScenarioType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}