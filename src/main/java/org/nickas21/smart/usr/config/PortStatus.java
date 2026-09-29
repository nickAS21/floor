package org.nickas21.smart.usr.config;

public enum PortStatus {
    ACTIVE,     // Дані йдуть (менше 20 хв)
    ACTIVE_INVERTER, // <--- Новий тимчасовий статус (Дані з інвертора)
    STANDBY,    // Пауза (20-60 хв), сокет закриваємо для реконнекту
    OFFLINE     // Труба (більше 60 хв), прилади вимикаємо
}