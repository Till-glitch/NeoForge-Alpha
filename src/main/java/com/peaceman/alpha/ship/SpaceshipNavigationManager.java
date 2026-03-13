package com.peaceman.alpha.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

public class SpaceshipNavigationManager {

    // 1. Speichert die aktuelle Position als neuen Wegpunkt
    public static void saveHome(Level level, Spaceship ship, String homeName) {
        if (ship != null) {
            ship.addHome(homeName, ship.getControllerPos());
            System.out.println("Wegpunkt '" + homeName + "' erfolgreich gespeichert!");

            // Wichtig: Dem Server sagen, dass er die neue Liste auf der Festplatte speichern muss
            if (level instanceof ServerLevel serverLevel) {
                ShipSavedData.get(serverLevel).setDirty();
            }
        }
    }

    // 2. Berechnet die Route und gibt sie an den SpaceshipMover weiter
    public static void teleportToHome(Level level, Spaceship ship, String homeName, Player player) {
        if (ship != null && ship.getHomes().containsKey(homeName)) {
            BlockPos targetPos = ship.getHomes().get(homeName);
            BlockPos currentPos = ship.getControllerPos();

            // Rechnet die nötige Verschiebung (Delta) in alle 3 Richtungen aus
            int dx = targetPos.getX() - currentPos.getX();
            int dy = targetPos.getY() - currentPos.getY();
            int dz = targetPos.getZ() - currentPos.getZ();

            // Wir rufen direkt unseren neuen Mover auf!
            SpaceshipMover.moveShip(level, ship, dx, dy, dz, player);

            System.out.println("Schiff erfolgreich zu '" + homeName + "' teleportiert!");
        } else {
            System.out.println("Fehler: Wegpunkt '" + homeName + "' nicht gefunden!");
            if (player != null) {
                player.displayClientMessage(Component.literal("§cFehler: Wegpunkt '" + homeName + "' existiert nicht!"), true);
            }
        }
    }
}