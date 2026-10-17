package com.victoriomods.taczballisticbreaching;

enum GasMaskFilterType {
    BASIC("basic", "Basic Filter", 6000),
    MILITARY("military", "Military Filter", 12000),
    ADVANCED_NBC("advanced_nbc", "Advanced NBC Filter", 18000);

    private final String id;
    private final String displayName;
    private final int maxDurability;

    GasMaskFilterType(String id, String displayName, int maxDurability) {
        this.id = id;
        this.displayName = displayName;
        this.maxDurability = maxDurability;
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

    static GasMaskFilterType byId(String id) {
        for (GasMaskFilterType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return BASIC;
    }
}
