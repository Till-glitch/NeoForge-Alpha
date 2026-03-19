package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceshipEnergyManager {

    // 1. Berechnet die benötigte Energie für den Flug
    public static int calculateMovementCost(Spaceship ship, int dx, int dy, int dz) {
        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        // Formel: 10 FE pro Block pro zurückgelegtem Meter
        return ship.getBlocks().size() * distance * 10;
    }

    // 2. Sucht alle Reaktoren im Schiff und bündelt die verfügbare Energie
    public static int getTotalAvailableEnergy(Level level, Spaceship ship) {
        int totalEnergy = 0;
        for (BlockPos pos : ship.getReactors()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                totalEnergy += reactor.getEnergyStorage().getEnergyStored();
            }
        }
        return totalEnergy;
    }

    // 3. Zieht die Energie der Reihe nach aus den Reaktoren ab
    public static void consumeEnergy(Level level, Spaceship ship, int amountToExtract) {
        int remainingCost = amountToExtract;

        for (BlockPos pos : ship.getReactors()) {
            if (remainingCost <= 0) break; // Fertig, wenn alle Kosten gedeckt sind

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                int extracted = reactor.getEnergyStorage().extractEnergy(remainingCost, false);
                remainingCost -= extracted;
            }
        }
    }

    // 4. Die allgemeine Basis-Methode für ALLES (Schilde, Waffen, Flug)
    public static boolean tryConsumeEnergyAmount(Level level, Spaceship ship, int amount) {
        if (getTotalAvailableEnergy(level, ship) < amount) {
            return false; // Nicht genug Energie da
        }

        // Genug Energie vorhanden -> Abziehen!
        consumeEnergy(level, ship, amount);
        return true;
    }

    // 5. Die spezielle Methode für den Flug (nutzt jetzt die Basis-Methode!)
    public static boolean tryConsumeFlightEnergy(Level level, Spaceship ship, int dx, int dy, int dz, Player player) {
        int cost = calculateMovementCost(ship, dx, dy, dz);

        // Wir lassen einfach unsere allgemeine Methode die harte Arbeit machen!
        boolean success = tryConsumeEnergyAmount(level, ship, cost);

        // Wenn die Basis-Methode 'false' liefert UND ein Spieler da ist, geben wir eine Warnung aus
        if (!success && player != null) {
            int available = getTotalAvailableEnergy(level, ship);
            player.displayClientMessage(
                    Component.literal("§cNicht genug Energie! §7Benötigt: " + cost + " FE | Vorhanden: " + available + " FE"), true);
        }

        return success;
    }
}