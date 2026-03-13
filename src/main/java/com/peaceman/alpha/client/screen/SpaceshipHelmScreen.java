package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.peaceman.alpha.helper.TickScheduler;

public class SpaceshipHelmScreen extends Screen {

    private net.minecraft.core.BlockPos blockPos;
    private EditBox distanceInput;
    private EditBox homeNameInput;

    public SpaceshipHelmScreen(net.minecraft.core.BlockPos pos) {
        super(Component.literal("Raumschiff Navigation"));
        this.blockPos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        // Wie beim Command-Block: Wir orientieren uns an der exakten Bildschirmmitte
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        java.util.function.Supplier<Integer> getDist = () -> {
            try {
                return Integer.parseInt(this.distanceInput.getValue());
            } catch (NumberFormatException e) {
                return 1;
            }
        };

        // --- LINKE SEITE: FLUG-STEUERUNG ---
        int leftCol = centerX - 120;

        this.distanceInput = new EditBox(this.font, leftCol, centerY - 20, 80, 20, Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        this.addRenderableWidget(Button.builder(Component.literal("Hoch"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_UP", dist, ""));
            this.blockPos = this.blockPos.above(dist);
        }).bounds(leftCol, centerY + 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Runter"), button -> {
            TickScheduler.runAfterSeconds(5, () -> {
                int dist = getDist.get();
                PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_DOWN", dist, ""));
                this.blockPos = this.blockPos.below(dist);
            });
        }).bounds(leftCol, centerY + 30, 80, 20).build());


        // --- MITTE: RICHTUNGS-STEUERUNG (WASD) ---
        this.addRenderableWidget(Button.builder(Component.literal("W"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_FORWARD", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection(), dist);
        }).bounds(centerX - 17, centerY - 20, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("A"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_LEFT", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getCounterClockWise(), dist);
        }).bounds(centerX - 57, centerY + 5, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("S"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_BACKWARD", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getOpposite(), dist);
        }).bounds(centerX - 17, centerY + 5, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("D"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_RIGHT", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getClockWise(), dist);
        }).bounds(centerX + 23, centerY + 5, 35, 20).build());


        // --- RECHTE SEITE: NAVIGATION ---
        int rightCol = centerX + 80;

        this.homeNameInput = new EditBox(this.font, rightCol, centerY - 20, 80, 20, Component.literal("Wegpunkt"));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Speichern"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "SAVE_HOME", 0, homeName));
        }).bounds(rightCol, centerY + 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Anfliegen"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "TP_HOME", 0, homeName));
            this.minecraft.setScreen(null);
        }).bounds(rightCol, centerY + 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Abdunkeln des Hintergrunds (Genau wie der Command-Block)
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 2. Zeichnet alle Buttons und Textfelder, die in init() hinzugefügt wurden
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 3. Texte frei schwebend zeichnen (Mit Schatten, sieht auf dunklem Grund super aus!)
        // 16777215 ist der Farbcode für reines Weiß (wird auch im CommandBlock genutzt)
        // 10526880 ist ein schönes Grau
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 60, 16777215);

        guiGraphics.drawCenteredString(this.font, Component.literal("Manuell"), centerX - 80, centerY - 35, 10526880);
        guiGraphics.drawCenteredString(this.font, Component.literal("Wegpunkte"), centerX + 120, centerY - 35, 10526880);
    }
}