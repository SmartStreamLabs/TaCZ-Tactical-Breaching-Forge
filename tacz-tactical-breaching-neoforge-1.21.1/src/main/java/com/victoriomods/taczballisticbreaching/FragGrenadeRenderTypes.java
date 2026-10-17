package com.victoriomods.taczballisticbreaching;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class FragGrenadeRenderTypes extends RenderType {
    private FragGrenadeRenderTypes() {
        super("tacz_tactical_breaching_unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, false, false, () -> {
        }, () -> {
        });
    }

    static RenderType v40Triangles(ResourceLocation texture) {
        return triangles("tacz_tactical_breaching_v40_frag_grenade", texture);
    }

    static RenderType flashbangTriangles(ResourceLocation texture) {
        return triangles("tacz_tactical_breaching_low_poly_flashbang", texture);
    }

    private static RenderType triangles(String name, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 32768, true, false, state);
    }
}
