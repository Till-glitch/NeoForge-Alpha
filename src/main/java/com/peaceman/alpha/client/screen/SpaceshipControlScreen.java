package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public class SpaceshipControlScreen extends Screen {

    // NEU: Die shipId Variable (darf null sein, wenn das Schiff noch nicht existiert)
    private UUID shipId;
    private final net.minecraft.core.BlockPos blockPos;

    // Konstruktor nimmt jetzt UUID und BlockPos an
    public SpaceshipControlScreen(UUID shipId, net.minecraft.core.BlockPos pos) {
        super(Component.literal("Raumschiff Steuerung"));
        this.shipId = shipId;
        this.blockPos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    private void updateShipIdFromBlock() {
        if (this.minecraft != null && this.minecraft.level != null) {
            if (this.minecraft.level.getBlockEntity(this.blockPos) instanceof SpaceshipControlBlockEntity be) {
                this.shipId = be.getShipId(); // Aktualisiert die Klassenvariable mit dem neuesten Stand vom Server
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int btnWidth = 140;
        int btnHeight = 20;
        int btnLeft = centerX - (btnWidth / 2);

        // Initiale Abfrage beim Öffnen des Screens (Nutzt jetzt direkt deine neue Methode!)
        updateShipIdFromBlock();

        // 1. Schiff erstellen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff erstellen"), button -> {
            updateShipIdFromBlock(); // Zur Sicherheit nochmal aktualisieren
            Optional<UUID> optionalShipId = Optional.ofNullable(this.shipId);
            PacketDistributor.sendToServer(new ShipCommandPayload(optionalShipId, this.blockPos, "CREATE", 0, ""));
        }).bounds(btnLeft, centerY - 45, btnWidth, btnHeight).build());

        // 2. Struktur aktualisieren
        this.addRenderableWidget(Button.builder(Component.literal("Struktur updaten"), button -> {
            updateShipIdFromBlock(); // WICHTIG: Frischen Stand vor dem Klick holen!
            Optional<UUID> optionalShipId = Optional.ofNullable(this.shipId);
            PacketDistributor.sendToServer(new ShipCommandPayload(optionalShipId, this.blockPos, "UPDATE_BLOCKS", 0, ""));
        }).bounds(btnLeft, centerY - 15, btnWidth, btnHeight).build());

        // 3. Schiff auflösen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff auflösen"), button -> {
            updateShipIdFromBlock(); // Frischen Stand vor dem Klick holen!
            Optional<UUID> optionalShipId = Optional.ofNullable(this.shipId);
            System.out.println("Sende DELETE_SHIP. Aktuelle UUID: " + this.shipId);
            PacketDistributor.sendToServer(new ShipCommandPayload(optionalShipId, this.blockPos, "DELETE_SHIP", 0, ""));
        }).bounds(btnLeft, centerY + 15, btnWidth, btnHeight).build());

        // 4. Markierung An/Aus (Braucht kein Update, da rein Client-seitig)
        this.addRenderableWidget(Button.builder(Component.literal("Markierung An/Aus"), button -> {
            com.peaceman.alpha.client.ShipHighlightRenderer.toggleHighlight(this.minecraft.level, this.blockPos);
        }).bounds(btnLeft, centerY + 45, btnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 16777215);
    }
}