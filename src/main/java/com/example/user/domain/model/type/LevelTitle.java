package com.example.user.domain.model.type;

import lombok.Getter;

@Getter
public enum LevelTitle {
    // 조문객, 환자, 송장, 유령, 악귀
    MOURNER(1, 1.08),
    PATIENT(10, 1.10),
    CORPSE(20, 1.13),
    GHOST(30, 1.16),
    DEMON(40, 1.20);

    private final int requiredLevel;
    private final double growthRate;

    LevelTitle(int requiredLevel, double growthRate) {
        this.requiredLevel = requiredLevel;
        this.growthRate = growthRate;
    }

    public static LevelTitle fromLevel(int level) {
        if (level >= DEMON.requiredLevel) {
            return DEMON;
        }
        if (level >= GHOST.requiredLevel) {
            return GHOST;
        }
        if (level >= CORPSE.requiredLevel) {
            return CORPSE;
        }
        if (level >= PATIENT.requiredLevel) {
            return PATIENT;
        }
        return MOURNER;
    }

}
