package com.sirens.visuals.fx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sirens.visuals.cfg.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Real skybox: the user's image is wrapped on an equirectangular sphere around the camera, drawn from inside the
 * vanilla sky pass (see SkyRendererMixin). It is fixed to the world like the real sky, so it never "swims" when you
 * look around. Best with 2:1 panoramas; other ratios are stretched over the sphere.
 * Approach and render-type setup follow the community 1.21.11 sky-image mod (milesclimbing900-max/gjk).
 */
public final class SkyDome {
    private SkyDome() { }

    public static final Identifier TEX = Identifier.fromNamespaceAndPath("sirensvisuals", "user_sky");
    private static final int LON = 64, LAT = 32;
    private static final float RADIUS = 100.0f;
    /** x, y, z, u, v per vertex over a (LAT+1) x (LON+1) grid. */
    private static final float[] GRID = grid();
    private static RenderType type;

    private static float[] grid() {
        float[] g = new float[(LAT + 1) * (LON + 1) * 5];
        int k = 0;
        for (int i = 0; i <= LAT; i++) {
            double lat = Math.PI / 2.0 - Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double lon = 2.0 * Math.PI * j / LON;
                g[k++] = (float) (Math.cos(lat) * Math.sin(lon) * RADIUS);
                g[k++] = (float) (Math.sin(lat) * RADIUS);
                g[k++] = (float) (-Math.cos(lat) * Math.cos(lon) * RADIUS);
                g[k++] = (float) j / LON;
                g[k++] = (float) i / LAT;
            }
        }
        return g;
    }

    public static boolean active() { return Mods.SKY.on() && Mods.S_IMG_ON.on() && SkyFx.imgId != null; }

    private static RenderType type() {
        if (type == null) {
            RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("sirensvisuals", "pipeline/user_sky"))
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .build();
            type = RenderType.create("sirensvisuals_user_sky",
                    RenderSetup.builder(pipeline).withTexture("Sampler0", TEX).createRenderSetup());
        }
        return type;
    }

    /** 0.15..1: darker at night so the skybox does not glow in the dark. */
    private static float daylight() {
        var lv = Minecraft.getInstance().level;
        if (lv == null || !Mods.S_IMG_NIGHT.on()) return 1f;
        long t = ((lv.getDayTime() % 24000L) + 24000L) % 24000L;
        double d = Math.cos(2 * Math.PI * (t - 6000) / 24000.0) * 1.6 + 0.55;
        return (float) Math.max(0.15, Math.min(1.0, d));
    }

    public static void render(PoseStack pose) {
        if (!active()) return;
        Matrix4f m = new Matrix4f(pose.last().pose()).rotateY((float) Math.toRadians(Mods.S_IMG_YAW.f()));
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType t = type();
        VertexConsumer vc = buffers.getBuffer(t);
        float c = Mods.S_IMG_BRIGHT.f() * daylight(), a = Mods.S_IMG_OP.f();
        for (int i = 0; i < LAT; i++)
            for (int j = 0; j < LON; j++) {
                v(vc, m, i, j, c, a); v(vc, m, i + 1, j, c, a); v(vc, m, i + 1, j + 1, c, a); v(vc, m, i, j + 1, c, a);
            }
        buffers.endBatch(t);
    }

    private static void v(VertexConsumer vc, Matrix4f m, int i, int j, float c, float a) {
        int k = (i * (LON + 1) + j) * 5;
        vc.addVertex(m, GRID[k], GRID[k + 1], GRID[k + 2]).setUv(GRID[k + 3], GRID[k + 4]).setColor(c, c, c, a);
    }
}
