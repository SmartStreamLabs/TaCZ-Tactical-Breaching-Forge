package com.victoriomods.taczballisticbreaching;

enum DistantGunshotSoundType {
    RIFLE_POP(0),
    RIFLE_ECHO(1),
    PISTOL_POP(2),
    SNIPER_ECHO(3),
    SHOTGUN_ECHO(4),
    MACHINEGUN_FAR(5),
    EXTREME_ECHO(6),
    FAR_ECHO(7),
    DISTANT_ECHO(8),
    NEAR_ECHO(9);

    private final int id;

    DistantGunshotSoundType(int id) {
        this.id = id;
    }

    int id() {
        return id;
    }

    static DistantGunshotSoundType byId(int id) {
        for (DistantGunshotSoundType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return RIFLE_POP;
    }
}
