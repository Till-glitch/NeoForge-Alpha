package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
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

        java.util.function.Supplier<Integer> getDist = () -> {
            try {
                return Integer.parseInt(this.distanceInput.getValue());
            } catch (NumberFormatException e) {
                return 1;
            }
        };

        // --- DISTANZ UND MANUELLES FLIEGEN ---
        this.distanceInput = new EditBox(this.font, this.width / 2 - 50, this.height / 2 - 40, 100, 20,
                Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        // HOCH Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Hoch"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_UP", dist, ""));
            this.blockPos = this.blockPos.above(dist);
        }).bounds(this.width / 2 - 50, this.height / 2 - 15, 100, 20).build());

        // RUNTER Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Runter"), button -> {
            TickScheduler.runAfterSeconds(5, () -> {
                int dist = getDist.get();
                PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_DOWN", dist, ""));
                this.blockPos = this.blockPos.below(dist);
            });
        }).bounds(this.width / 2 - 50, this.height / 2 + 10, 100, 20).build());

        // VORWÄRTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Vorwärts (W)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_FORWARD", dist, ""));

            Direction forward = this.minecraft.player.getDirection();
            this.blockPos = this.blockPos.relative(forward, dist);
        }).bounds(this.width / 2 - 160, this.height / 2 - 15, 100, 20).build());

        // RÜCKWÄRTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Rückwärts (S)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_BACKWARD", dist, ""));

            Direction forward = this.minecraft.player.getDirection();
            this.blockPos = this.blockPos.relative(forward.getOpposite(), dist);
        }).bounds(this.width / 2 - 160, this.height / 2 + 10, 100, 20).build());

        // LINKS Knopf (Etwas weiter unten platziert)
        this.addRenderableWidget(Button.builder(Component.literal("Links (A)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_LEFT", dist, ""));

            Direction forward = this.minecraft.player.getDirection();
            this.blockPos = this.blockPos.relative(forward.getCounterClockWise(), dist);
        }).bounds(this.width / 2 - 160, this.height / 2 + 35, 100, 20).build());

        // RECHTS Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Rechts (D)"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_RIGHT", dist, ""));

            Direction forward = this.minecraft.player.getDirection();
            this.blockPos = this.blockPos.relative(forward.getClockWise(), dist);
        }).bounds(this.width / 2 - 50, this.height / 2 + 35, 100, 20).build());

        // --- DAS NEUE HOME SYSTEM (Auf der rechten Seite des Bildschirms) ---
        int rightColumnX = this.width / 2 + 60;

        this.homeNameInput = new EditBox(this.font, rightColumnX, this.height / 2 - 40, 100, 20,
                Component.literal("Wegpunkt Name"));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Wegpunkt speichern"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "SAVE_HOME", 0, homeName));
        }).bounds(rightColumnX, this.height / 2 - 15, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Zu Wegpunkt fliegen"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "TP_HOME", 0, homeName));
            this.minecraft.setScreen(null);
        }).bounds(rightColumnX, this.height / 2 + 10, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.drawString(this.font, "Flugdistanz", this.width / 2 - 50, this.height / 2 - 55, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Navigation", this.width / 2 + 60, this.height / 2 - 55, 0xA0A0A0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
