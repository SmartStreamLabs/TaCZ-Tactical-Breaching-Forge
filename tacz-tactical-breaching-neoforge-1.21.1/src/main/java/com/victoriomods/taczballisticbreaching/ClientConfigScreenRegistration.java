package com.victoriomods.taczballisticbreaching;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;

@OnlyIn(Dist.CLIENT)
final class ClientConfigScreenRegistration {
    private ClientConfigScreenRegistration() {
    }

    static void register(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignored, parent) -> new TacticalBreachingConfigScreen(parent)
        );
    }
}
