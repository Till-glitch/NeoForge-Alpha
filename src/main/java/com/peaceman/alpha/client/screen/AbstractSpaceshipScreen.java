package com.peaceman.alpha.client.screen; // Passe das Package an deine Struktur an

import com.peaceman.alpha.network.ShipCommandPayload;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractSpaceshipScreen extends Screen {

    protected final BlockPos blockPos;
    protected UUID shipId;

    protected AbstractSpaceshipScreen(Component title, BlockPos blockPos) {
        super(title);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        // Aktualisiert die ID automatisch, sobald sich IRGENDEIN Spaceship-Screen öffnet
        updateShipIdFromBlock();
    }

    // Die Methode, die sich immer den neuesten Stand holt
    protected void updateShipIdFromBlock() {
        if (this.minecraft != null && this.minecraft.level != null) {

            // NEU: Wir prüfen auf dein Interface ISpaceshipNode!
            // So funktioniert es beim Control-Block, beim Helm-Block und bei allen
            // zukünftigen Schiffs-Blöcken, die du noch baust!
            if (this.minecraft.level.getBlockEntity(this.blockPos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                this.shipId = node.getShipId();
            }

        }
    }

    /**
     * Ein genialer kleiner Helfer:
     * Diese Methode nimmt dir in deinen Buttons die komplette Arbeit ab!
     */
    protected void sendShipCommand(String command, int value, String homeName) {
        updateShipIdFromBlock(); // Zieht sich live die aktuellste UUID
        Optional<UUID> optionalShipId = Optional.ofNullable(this.shipId);
        PacketDistributor.sendToServer(new ShipCommandPayload(optionalShipId, this.blockPos, command, value, homeName));
    }
}