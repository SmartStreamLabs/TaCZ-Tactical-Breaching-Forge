package com.victoriomods.taczballisticbreaching;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

enum CasingType {
    RIFLE(0, Items.GOLD_NUGGET, 0.034D, 0.34D),
    PISTOL(1, Items.GOLD_NUGGET, 0.031D, 0.38D),
    SHOTGUN(2, Items.RED_DYE, 0.038D, 0.28D);

    private final int id;
    private final Item fallbackItem;
    private final double gravity;
    private final double bounce;

    CasingType(int id, Item fallbackItem, double gravity, double bounce) {
        this.id = id;
        this.fallbackItem = fallbackItem;
        this.gravity = gravity;
        this.bounce = bounce;
    }

    int id() {
        return id;
    }

    Item fallbackItem() {
        return fallbackItem;
    }

    double gravity() {
        return gravity;
    }

    double bounce() {
        return bounce;
    }

    static CasingType byId(int id) {
        for (CasingType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return RIFLE;
    }
}
