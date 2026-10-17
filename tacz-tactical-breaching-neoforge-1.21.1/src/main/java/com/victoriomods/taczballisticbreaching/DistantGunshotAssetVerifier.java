package com.victoriomods.taczballisticbreaching;

import java.net.URL;
import java.util.List;

public final class DistantGunshotAssetVerifier {
    private static final List<String> REQUIRED_ASSETS = List.of(
            "assets/tacz_tactical_breaching/sounds/distant/rifle_pop_1.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/rifle_echo_1.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/pistol_pop_1.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/gun_distance_300.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/gun_distance_1000.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/gun_distance_3000.ogg",
            "assets/tacz_tactical_breaching/sounds/distant/gun_distance_7000.ogg"
    );

    private static boolean checked;
    private static boolean missing;

    private DistantGunshotAssetVerifier() {
    }

    public static boolean verifyOnce() {
        if (checked) {
            return !missing;
        }
        checked = true;
        ClassLoader loader = DistantGunshotAssetVerifier.class.getClassLoader();
        for (String asset : REQUIRED_ASSETS) {
            URL resource = loader.getResource(asset);
            if (resource == null) {
                missing = true;
                TaczTacticalBreaching.LOGGER.warn("Missing distant gunshot sound asset: {}", asset);
            }
        }
        return !missing;
    }

    public static boolean hasMissingAssets() {
        verifyOnce();
        return missing;
    }
}
