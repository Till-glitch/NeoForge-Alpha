package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpaceshipControlScreen extends Screen {

    private final net.minecraft.core.BlockPos blockPos;
    private EditBox distanceInput; // Unser neues Textfeld

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
// 1. Der SCAN Knopf
        this.addRenderableWidget(Button.builder(Component.literal("1. Schiff scannen"), button -> {
            // Sendet den Befehl "SCAN" (Zahl ist hier egal, also 0)
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "SCAN", 0));
        }).bounds(this.width / 2 - 50, this.height / 2 - 40, 100, 20).build());

        // 2. Das Textfeld für die Distanz (etwas nach unten verschoben)
        this.distanceInput = new EditBox(this.font, this.width / 2 - 50, this.height / 2 - 15, 100, 20, Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        // 3. Der BEWEGEN Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Flug: Hoch"), button -> {
            try {
                int distance = Integer.parseInt(this.distanceInput.getValue());
                // Sendet den Befehl "MOVE_UP" mit der eingegebenen Distanz
                PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_UP", distance));
                this.minecraft.setScreen(null);
            } catch (NumberFormatException e) {
                // Ignorieren, wenn keine Zahl eingegeben wurde
            }
        }).bounds(this.width / 2 - 50, this.height / 2 + 10, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Flug: Runter"), button -> {
            try {
                int distance = Integer.parseInt(this.distanceInput.getValue());
                // Sendet den Befehl "MOVE_UP" mit der eingegebenen Distanz
                PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_DOWN", distance));
                this.minecraft.setScreen(null);
            } catch (NumberFormatException e) {
                // Ignorieren, wenn keine Zahl eingegeben wurde
            }
        }).bounds(this.width / 2 - 50, this.height / 2 + 30, 100, 20).build());

        // (Dein Highlight-Button kann z.B. bei y = this.height / 2 + 35 bleiben)
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Kleiner Hilfstext über dem Textfeld
        guiGraphics.drawCenteredString(this.font, "Wie viele Blöcke nach oben?", this.width / 2, this.height / 2 - 55, 0xA0A0A0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}