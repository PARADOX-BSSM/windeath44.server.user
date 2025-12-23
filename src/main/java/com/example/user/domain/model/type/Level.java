package com.example.user.domain.model.type;

import lombok.AllArgsConstructor;

public enum Level {
    // 조문객, 환자, 송장, 유령, 악귀
    MOURNER(0L),
    PATIENT(10_000L),
    CORPSE(35_000L),
    GHOST(90_000L),
    DEMON(200_000L);

    private final long minXp;

    Level(long minXp) {
        this.minXp = minXp;
    }

    public static Level fromXp(long xp) {
        if (xp >= DEMON.minXp) {
            return DEMON;
        }
        if (xp >= GHOST.minXp) {
            return GHOST;
        }
        if (xp >= CORPSE.minXp) {
            return CORPSE;
        }
        if (xp >= PATIENT.minXp) {
            return PATIENT;
        }
        return MOURNER;
    }


    public static long calculateXpToNextLevel(long currentXp) {
        long safeXp = Math.max(currentXp, 0);
        Level currentLevel = fromXp(safeXp);
        Level nextLevel = currentLevel.getNextLevel();
        
        if (currentLevel == nextLevel) {
            return 0; // 이미 최고 레벨
        }
        
        return nextLevel.minXp - safeXp;
    }

    public Level getNextLevel() {
        return switch (this) {
            case MOURNER -> PATIENT;
            case PATIENT -> CORPSE;
            case CORPSE -> GHOST;
            case GHOST -> DEMON;
            case DEMON -> DEMON;
        };
    }
}
