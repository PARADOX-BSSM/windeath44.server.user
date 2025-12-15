package com.example.user.domain.model.type;

public enum Level {
    // 조문객, 환자, 송장, 유령, 악귀
    MOURNER,
    PATIENT,
    CORPSE,
    GHOST,
    DEMON;

    public static Level fromXp(long xp) {
        if (xp >= 200_000) {
            return DEMON;
        }
        if (xp >= 90_000) {
            return GHOST;
        }
        if (xp >= 35_000) {
            return CORPSE;
        }
        if (xp >= 10_000) {
            return PATIENT;
        }
        return MOURNER;
    }
}
