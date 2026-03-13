package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.SpaceshipReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

public class SpaceshipEnergyManager {

    // 1. Berechnet die benötigte Energie für den Flug
    public static int calculateMovementCost(int blockCount, int dx, int dy, int dz) {
        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        // Formel: 10 FE pro Block pro zurückgelegtem Meter
        return blockCount * distance * 10;
    }

    // 2. Sucht alle Reaktoren im Schiff und bündelt die verfügbare Energie
    public static int getTotalAvailableEnergy(Level level, Set<BlockPos> shipBlocks) {
        int totalEnergy = 0;
        for (BlockPos pos : shipBlocks) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                totalEnergy += reactor.getEnergyStorage().getEnergyStored();
            }
        }
        return totalEnergy;
    }

    // 3. Zieht die Energie der Reihe nach aus den Reaktoren ab
    public static void consumeEnergy(Level level, Set<BlockPos> shipBlocks, int amountToExtract) {
        int remainingCost = amountToExtract;

        for (BlockPos pos : shipBlocks) {
            if (remainingCost <= 0) break; // Fertig, wenn alle Kosten gedeckt sind

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                // Das 'false' am Ende bedeutet: Energie WIRKLICH abziehen, nicht nur simulieren
                int extracted = reactor.getEnergyStorage().extractEnergy(remainingCost, false);
                remainingCost -= extracted;
            }
        }
    }

    // 4. Die "All-in-One" Methode für den Flug (Gibt true zurück, wenn erfolgreich)
    public static boolean tryConsumeFlightEnergy(Level level, Set<BlockPos> shipBlocks, int dx, int dy, int dz, Player player) {
        int cost = calculateMovementCost(shipBlocks.size(), dx, dy, dz);
        int available = getTotalAvailableEnergy(level, shipBlocks);

        if (available < cost) {
            if (player != null) {
                player.displayClientMessage(
                        Component.literal("§cNicht genug Energie! §7Benötigt: " + cost + " FE | Vorhanden: " + available + " FE"), true);
            }
            return false; // Flug abbrechen
        }

        // Genug Energie da! Jetzt ziehen wir sie ab.
        consumeEnergy(level, shipBlocks, cost);
        return true; // Flug erlaubt!
    }
}