package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpaceshipControlScreen extends Screen {

    private final net.minecraft.core.BlockPos blockPos;

    public SpaceshipControlScreen(net.minecraft.core.BlockPos pos) {
        super(Component.literal("Raumschiff Steuerung"));
        this.blockPos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        // Exakte Bildschirmmitte berechnen
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Button-Maße (Etwas breiter, damit "Markierung An/Aus" gut reinpasst)
        int btnWidth = 140;
        int btnHeight = 20;
        int btnLeft = centerX - (btnWidth / 2);

        // 1. Schiff erstellen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff erstellen"), button -> {
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "CREATE", 0, ""));
        }).bounds(btnLeft, centerY - 45, btnWidth, btnHeight).build());

        // 2. Struktur aktualisieren
        this.addRenderableWidget(Button.builder(Component.literal("Struktur updaten"), button -> {
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "UPDATE_BLOCKS", 0, ""));
        }).bounds(btnLeft, centerY - 15, btnWidth, btnHeight).build());

        // 3. Schiff auflösen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff auflösen"), button -> {
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "DELETE_SHIP", 0, ""));
        }).bounds(btnLeft, centerY + 15, btnWidth, btnHeight).build());

        // 4. Markierung An/Aus
        this.addRenderableWidget(Button.builder(Component.literal("Markierung An/Aus"), button -> {
            com.peaceman.alpha.client.ShipHighlightRenderer.toggleHighlight(this.minecraft.level, this.blockPos);
        }).bounds(btnLeft, centerY + 45, btnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Spielwelt im Hintergrund abdunkeln
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 2. Zeichnet alle Buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 3. Den Titel exakt zentriert über den Buttons zeichnen (16777215 ist reines Weiß)
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 75, 16777215);
    }
}