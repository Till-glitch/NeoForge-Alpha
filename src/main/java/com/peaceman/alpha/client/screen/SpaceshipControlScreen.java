package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.block.SpaceshipControlBlock;
import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpaceshipControlScreen extends Screen {

    private net.minecraft.core.BlockPos blockPos;
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

        this.addRenderableWidget(Button.builder(Component.literal("Markierung An/Aus"), button -> {

            // 1. Zustand umkehren (An wird zu Aus, Aus wird zu An)
            com.peaceman.alpha.client.ShipHighlightRenderer.isHighlightActive = !com.peaceman.alpha.client.ShipHighlightRenderer.isHighlightActive;

            if (com.peaceman.alpha.client.ShipHighlightRenderer.isHighlightActive) {
                // 2. Wenn AN: Wir nutzen deinen Scanner-Code, aber diesmal für den Client!
                com.peaceman.alpha.client.ShipHighlightRenderer.shipBlocks =
                        SpaceshipControlBlock.scanSpaceship(this.minecraft.level, this.blockPos);
            } else {
                // 3. Wenn AUS: Liste leeren, damit keine Partikel mehr spawnen
                com.peaceman.alpha.client.ShipHighlightRenderer.shipBlocks.clear();
            }

        }).bounds(this.width / 2 +70, this.height / 2 - 40, 120, 20).build());

        // 2. Das Textfeld für die Distanz (etwas nach unten verschoben)
        this.distanceInput = new EditBox(this.font, this.width / 2 - 50, this.height / 2 - 15, 100, 20, Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        java.util.function.Supplier<Integer> getDist = () -> {
            try { return Integer.parseInt(this.distanceInput.getValue()); }
            catch (NumberFormatException e) { return 1; } // Standard ist 1, falls Unsinn drinsteht
        };

        // HOCH Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Hoch"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_UP", dist));

            // HIER NEU: Das Menü rechnet die neue Position aus und merkt sie sich!
            this.blockPos = this.blockPos.above(dist);

        }).bounds(this.width / 2 - 50, this.height / 2 + 10, 100, 20).build());

        // RUNTER Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Runter"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_DOWN", dist));
            this.blockPos = this.blockPos.below(dist); // Neue Position merken!
        }).bounds(this.width / 2 - 50, this.height / 2 + 35, 100, 20).build());


        // VORWÄRTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Vorwärts (W)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_FORWARD", dist));

            // Für Vorwärts/Rückwärts/Links/Rechts müssen wir wissen, wo der Spieler hinguckt
            Direction forward = this.minecraft.player.getDirection();

            // .relative() verschiebt die Koordinate in eine bestimmte Himmelsrichtung
            this.blockPos = this.blockPos.relative(forward, dist);

        }).bounds(this.width / 2 - 155, this.height / 2 + 10, 100, 20).build());

        // RÜCKWÄRTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Rückwärts (S)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_BACKWARD", dist));

            Direction forward = this.minecraft.player.getDirection();
            // getOpposite() dreht die Richtung genau um (also nach hinten)
            this.blockPos = this.blockPos.relative(forward.getOpposite(), dist);

        }).bounds(this.width / 2 - 155, this.height / 2 + 35, 100, 20).build());

        // LINKS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Links (A)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_LEFT", dist));

            Direction forward = this.minecraft.player.getDirection();
            // getCounterClockWise() dreht die Richtung nach links
            this.blockPos = this.blockPos.relative(forward.getCounterClockWise(), dist);

        }).bounds(this.width / 2 + 55, this.height / 2 + 10, 100, 20).build());

        // RECHTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Rechts (D)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_RIGHT", dist));

            Direction forward = this.minecraft.player.getDirection();
            // getClockWise() dreht die Richtung nach rechts
            this.blockPos = this.blockPos.relative(forward.getClockWise(), dist);

        }).bounds(this.width / 2 + 55, this.height / 2 + 35, 100, 20).build());
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