package com.victoriomods.taczballisticbreaching;

enum ArmorPlateType {
    SOFT("soft", "Soft Armor", 500, "Pistol", "Light", 0.0D, 0.55D, 0.05D, 0.0D, 0.15D),
    CERAMIC("ceramic", "Ceramic Plate", 1500, "Pistol/Rifle", "Medium", 0.0D, 0.70D, 0.50D, 0.25D, 0.35D),
    HEAVY("heavy", "Heavy Plate", 2500, "Rifle", "Heavy", 0.10D, 0.80D, 0.68D, 0.45D, 0.55D),
    MILITARY("military", "Military Grade Plate", 4000, "Military Grade", "Very Heavy", 0.20D, 0.90D, 0.78D, 0.60D, 0.68D);

    private final String id;
    private final String displayName;
    private final int maxDurability;
    private final String protectionLabel;
    private final String weightLabel;
    private final double movementPenalty;
    private final double pistolReduction;
    private final double rifleReduction;
    private final double highPowerReduction;
    private final double shotgunReduction;

    ArmorPlateType(String id, String displayName, int maxDurability, String protectionLabel, String weightLabel, double movementPenalty, double pistolReduction, double rifleReduction, double highPowerReduction, double shotgunReduction) {
        this.id = id;
        this.displayName = displayName;
        this.maxDurability = maxDurability;
        this.protectionLabel = protectionLabel;
        this.weightLabel = weightLabel;
        this.movementPenalty = movementPenalty;
        this.pistolReduction = pistolReduction;
        this.rifleReduction = rifleReduction;
        this.highPowerReduction = highPowerReduction;
        this.shotgunReduction = shotgunReduction;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    int maxDurability() {
        return maxDurability;
    }

    String protectionLabel() {
        return protectionLabel;
    }

    String weightLabel() {
        return weightLabel;
    }

    double movementPenalty() {
        return movementPenalty;
    }

    double reductionFor(Threat threat, boolean armorPiercing) {
        double reduction = switch (threat) {
            case PISTOL -> pistolReduction;
            case RIFLE -> rifleReduction;
            case HIGH_POWER -> highPowerReduction;
            case SHOTGUN -> shotgunReduction;
        };
        return armorPiercing ? reduction * 0.65D : reduction;
    }

    enum Threat {
        PISTOL,
        RIFLE,
        HIGH_POWER,
        SHOTGUN
    }
}
