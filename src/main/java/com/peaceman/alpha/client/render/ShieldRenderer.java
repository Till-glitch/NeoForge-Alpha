package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.peaceman.alpha.Alpha; // Deinen Haupt-Import hinzugefügt
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import java.io.IOException;

public class ShieldRenderer {

    public static BlockPos shipAnchorPoint = null;
    public static Vec3 lastImpactPos = Vec3.ZERO;
    public static float shieldEnergyPercentage = 1.0f;
    public static long lastImpactTick = -1000;

    private static ShaderInstance hexShieldShader;

    @EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
            // Platzhalter durch Alpha.MODID ersetzt!
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "hex_shield"), DefaultVertexFormat.POSITION_TEX),
                    shaderInstance -> hexShieldShader = shaderInstance
            );
        }
    }

    @EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            if (shipAnchorPoint == null || hexShieldShader == null) return;

            Minecraft mc = Minecraft.getInstance();
            Camera camera = event.getCamera();
            PoseStack poseStack = event.getPoseStack();
            Vec3 cameraPos = camera.getPosition();

            poseStack.pushPose();

            // 1. Verschiebe Render-Ursprung
            double offsetX = shipAnchorPoint.getX() - cameraPos.x;
            double offsetY = shipAnchorPoint.getY() - cameraPos.y;
            double offsetZ = shipAnchorPoint.getZ() - cameraPos.z;
            poseStack.translate(offsetX, offsetY, offsetZ);

            Matrix4f matrix = poseStack.last().pose();

            // 2. Transparenz & Shader aktivieren
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(() -> hexShieldShader);

            // 3. Uniforms übergeben
            float gameTime = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaTicks()) * 0.05f;
            if (hexShieldShader.safeGetUniform("GameTime") != null) {
                hexShieldShader.safeGetUniform("GameTime").set(gameTime);
            }
            if (hexShieldShader.safeGetUniform("ImpactPos") != null) {
                float relImpactX = (float) (lastImpactPos.x - shipAnchorPoint.getX());
                float relImpactY = (float) (lastImpactPos.y - shipAnchorPoint.getY());
                float relImpactZ = (float) (lastImpactPos.z - shipAnchorPoint.getZ());
                hexShieldShader.safeGetUniform("ImpactPos").set(relImpactX, relImpactY, relImpactZ);
            }
            if (hexShieldShader.safeGetUniform("EnergyLevel") != null) {
                hexShieldShader.safeGetUniform("EnergyLevel").set(shieldEnergyPercentage);
            }
            // Zeit seit dem letzten Treffer berechnen
            float impactTimer = 0;
            if (lastImpactTick > 0) {
                impactTimer = (mc.level.getGameTime() - lastImpactTick) + event.getPartialTick().getGameTimeDeltaTicks();
            }
            if (hexShieldShader.safeGetUniform("ImpactTimer") != null) {
                hexShieldShader.safeGetUniform("ImpactTimer").set(impactTimer);
            }

            // 4. Leinwand-Geometrie aufbauen
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            // Radius von 20 Blöcken (Muss groß genug für das Schiff sein)
            float radius = 20.0f;
            drawBoundingGeometry(bufferbuilder, matrix, radius);

            // HIER IST DIE MAGIE: Den Buffer rendern und an die Grafikkarte schicken!
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

            // 5. Aufräumen
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        private static void drawBoundingGeometry(BufferBuilder builder, Matrix4f matrix, float r) {
            // Front
            builder.addVertex(matrix, -r, -r, -r).setUv(0, 0);
            builder.addVertex(matrix, -r, r, -r).setUv(0, 1);
            builder.addVertex(matrix, r, r, -r).setUv(1, 1);
            builder.addVertex(matrix, r, -r, -r).setUv(1, 0);
            // Hinten
            builder.addVertex(matrix, -r, -r, r).setUv(0, 0);
            builder.addVertex(matrix, r, -r, r).setUv(1, 0);
            builder.addVertex(matrix, r, r, r).setUv(1, 1);
            builder.addVertex(matrix, -r, r, r).setUv(0, 1);
            // Oben
            builder.addVertex(matrix, -r, r, -r).setUv(0, 0);
            builder.addVertex(matrix, -r, r, r).setUv(0, 1);
            builder.addVertex(matrix, r, r, r).setUv(1, 1);
            builder.addVertex(matrix, r, r, -r).setUv(1, 0);
            // Unten
            builder.addVertex(matrix, -r, -r, -r).setUv(0, 0);
            builder.addVertex(matrix, r, -r, -r).setUv(1, 0);
            builder.addVertex(matrix, r, -r, r).setUv(1, 1);
            builder.addVertex(matrix, -r, -r, r).setUv(0, 1);
            // Links
            builder.addVertex(matrix, -r, -r, r).setUv(0, 0);
            builder.addVertex(matrix, -r, r, r).setUv(0, 1);
            builder.addVertex(matrix, -r, r, -r).setUv(1, 1);
            builder.addVertex(matrix, -r, -r, -r).setUv(1, 0);
            // Rechts
            builder.addVertex(matrix, r, -r, -r).setUv(0, 0);
            builder.addVertex(matrix, r, r, -r).setUv(0, 1);
            builder.addVertex(matrix, r, r, r).setUv(1, 1);
            builder.addVertex(matrix, r, -r, r).setUv(1, 0);
        }
    }
}