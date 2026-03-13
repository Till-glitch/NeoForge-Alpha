package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import com.peaceman.alpha.helper.TickScheduler;

public class SpaceshipHelmScreen extends Screen {

    private static final ResourceLocation BG_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 166;

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

        int leftPos = (this.width - WIDTH) / 2;
        int topPos = (this.height - HEIGHT) / 2;

        java.util.function.Supplier<Integer> getDist = () -> {
            try {
                return Integer.parseInt(this.distanceInput.getValue());
            } catch (NumberFormatException e) {
                return 1;
            }
        };

        // --- DISTANZ UND MANUELLES FLIEGEN ---
        this.distanceInput = new EditBox(this.font, leftPos + 20, topPos + 40, 80, 20,
                Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        // HOCH Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Hoch"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_UP", dist, ""));
            this.blockPos = this.blockPos.above(dist);
        }).bounds(leftPos + 20, topPos + 65, 80, 20).build());

        // RUNTER Knopf
        this.addRenderableWidget(Button.builder(Component.literal("Runter"), button -> {
            TickScheduler.runAfterSeconds(5, () -> {
                int dist = getDist.get();
                PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_DOWN", dist, ""));
                this.blockPos = this.blockPos.below(dist);
            });
        }).bounds(leftPos + 20, topPos + 90, 80, 20).build());

        // Richtungs-Steuerung (Mitte)
        int centerCol = leftPos + 110;
        this.addRenderableWidget(Button.builder(Component.literal("W"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_FORWARD", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection(), dist);
        }).bounds(centerCol, topPos + 40, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("A"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_LEFT", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getCounterClockWise(), dist);
        }).bounds(centerCol - 40, topPos + 65, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("S"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_BACKWARD", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getOpposite(), dist);
        }).bounds(centerCol, topPos + 65, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("D"), button -> {
            int dist = getDist.get();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "MOVE_RIGHT", dist, ""));
            this.blockPos = this.blockPos.relative(this.minecraft.player.getDirection().getClockWise(), dist);
        }).bounds(centerCol + 40, topPos + 65, 35, 20).build());

        // --- HOME SYSTEM (Rechts) ---
        int rightCol = leftPos + 160;
        this.homeNameInput = new EditBox(this.font, rightCol, topPos + 40, 80, 20,
                Component.literal("Wegpunkt"));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Speichern"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "SAVE_HOME", 0, homeName));
        }).bounds(rightCol, topPos + 65, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Anfliegen"), button -> {
            String homeName = this.homeNameInput.getValue();
            PacketDistributor.sendToServer(new ShipCommandPayload(this.blockPos, "TP_HOME", 0, homeName));
            this.minecraft.setScreen(null);
        }).bounds(rightCol, topPos + 90, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int leftPos = (this.width - WIDTH) / 2;
        int topPos = (this.height - HEIGHT) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        // Hintergrund-Textur zeichnen
        guiGraphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, topPos + 10, 0x404040);
        
        guiGraphics.drawString(this.font, "Flug", leftPos + 20, topPos + 28, 0x404040, false);
        guiGraphics.drawString(this.font, "Navigation", leftPos + 160, topPos + 28, 0x404040, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().popPose();
    }
}
