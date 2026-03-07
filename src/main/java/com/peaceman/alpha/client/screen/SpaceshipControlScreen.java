package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.block.SpaceshipControlBlock;
import com.peaceman.alpha.network.ScanShipPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpaceshipControlScreen extends Screen {
    // Neue Variable, um sich die Position zu merken
    private final net.minecraft.core.BlockPos blockPos;

    // Der Konstruktor erwartet jetzt die Position
    public SpaceshipControlScreen(net.minecraft.core.BlockPos pos) {
        super(Component.literal("Raumschiff Steuerung"));
        this.blockPos = pos;
    }
    @Override
    protected void init() {
        super.init();

        // DEIN ALTER KNOPF ("Start Engine")
        this.addRenderableWidget(Button.builder(Component.literal("Start Engine"), button -> {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.peaceman.alpha.network.ScanShipPayload(this.blockPos)
            );
            this.minecraft.setScreen(null);
        }).bounds(this.width / 2 - 50, this.height / 2 - 20, 100, 20).build());

        // NEUER KNOPF: Markierung An/Aus
        // Y-Position ist etwas tiefer (this.height / 2 + 5), damit sie nicht überlappen
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

        }).bounds(this.width / 2 - 60, this.height / 2 + 5, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Macht den normalen Minecraft-Hintergrund leicht dunkel
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Zeichnet einen Titeltext oben in die Mitte
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Zeichnet die Buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}