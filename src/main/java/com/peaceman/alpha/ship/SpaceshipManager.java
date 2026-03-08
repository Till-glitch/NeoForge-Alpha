package com.peaceman.alpha.ship;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.*;

@EventBusSubscriber(modid = Alpha.MODID)
public class SpaceshipManager {

    // Unser neues, objektorientiertes Gedächtnis (Speichert Schiffe nach ihrer eindeutigen UUID)
    public static final Map<UUID, Spaceship> ACTIVE_SHIPS = new HashMap<>();

    // Hilfsmethode: Findet ein Schiff anhand der Position seines Controllers
    public static Spaceship getShipAt(BlockPos controllerPos) {
        for (Spaceship ship : ACTIVE_SHIPS.values()) {
            if (ship.getControllerPos().equals(controllerPos)) {
                return ship;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShipSavedData.get(event.getServer().overworld());
        System.out.println("=== RAUMSCHIFFE GELADEN: " + ACTIVE_SHIPS.size() + " ===");
    }

    public static Set<BlockPos> scanSpaceship(Level level, BlockPos startPos) {
        Set<BlockPos> shipBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startPos);
        shipBlocks.add(startPos);

        int maxBlocks = 2000;

        while (!queue.isEmpty() && shipBlocks.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!level.getBlockState(neighbor).isAir() && !shipBlocks.contains(neighbor)) {
                    shipBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return shipBlocks;
    }

    public static void createShipInstance(Level level, BlockPos startPos) {
        // Altes Schiff an dieser Position löschen, falls wir neu scannen
        Spaceship oldShip = getShipAt(startPos);
        if (oldShip != null) ACTIVE_SHIPS.remove(oldShip.getId());

        Set<BlockPos> shipBlocks = scanSpaceship(level, startPos);

        // Neues Schiffsobjekt erstellen und speichern
        Spaceship newShip = new Spaceship(startPos, shipBlocks);
        ACTIVE_SHIPS.put(newShip.getId(), newShip);

        System.out.println("Schiff instanziiert! UUID: " + newShip.getId() + " | Blöcke: " + shipBlocks.size());

        // --- NEU: Den Rucksack des Blocks mit der UUID befüllen! ---
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity blockEntity) {
            blockEntity.setShipId(newShip.getId());
        }

        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // Wir übergeben jetzt die UUID anstatt der Startposition!
    public static void moveShipInstance(Level level, UUID shipId, int dx, int dy, int dz) {
        // Wir können das Schiff jetzt blitzschnell und direkt aus der Liste fischen
        Spaceship ship = ACTIVE_SHIPS.get(shipId);

        if (ship == null || ship.getBlocks().isEmpty()) {
            System.out.println("Fehler: Konnte kein Schiff mit dieser UUID finden!");
            return;
        }

        Set<BlockPos> shipBlocks = ship.getBlocks();
        BlockPos startPos = ship.getControllerPos(); // Wir holen uns die alte Position

        // ... (Dein bisheriger Code zum Berechnen der AABB und Snapshot-Machen bleibt gleich) ...
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : shipBlocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }
        AABB shipBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 2, maxZ + 1);
        List<Entity> entitiesToMove = level.getEntities(null, shipBounds).stream().filter(entity -> {
            BlockPos entityPos = entity.blockPosition();
            return shipBlocks.contains(entityPos) || shipBlocks.contains(entityPos.below());
        }).toList();

        Map<BlockPos, BlockState> snapshot = new HashMap<>();
        for (BlockPos pos : shipBlocks) snapshot.put(pos, level.getBlockState(pos));

        // --- DER NEUE TRICK FÜR DEN RUCKSACK ---
        // Bevor wir den alten Block löschen, nehmen wir ihm seine UUID weg!
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {
            be.setShipId(null);
        }

        // Neue Startposition berechnen und dem Schiffsobjekt geben
        BlockPos newStartPos = startPos.offset(dx, dy, dz);
        ship.setControllerPos(newStartPos);

        for (BlockPos pos : shipBlocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

        Set<BlockPos> newShipBlocks = new HashSet<>();
        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            level.setBlock(newPos, entry.getValue(), 3);
            newShipBlocks.add(newPos);

            // --- GANZ WICHTIG ---
            // Wenn wir den Kontrollblock an der neuen Position platzieren,
            // müssen wir ihm die UUID wieder in seinen neuen Rucksack stecken!
            if (entry.getValue().is(com.peaceman.alpha.registry.ModBlocks.SPACESHIP_CONTROL.get())) {
                if (level.getBlockEntity(newPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity newBe) {
                    newBe.setShipId(shipId);
                }
            }
        }

        ship.setBlocks(newShipBlocks);

        // ... (Dein Code für die Entities bleibt gleich) ...
        for (Entity entity : entitiesToMove) {
            double newX = entity.getX() + dx;
            double newY = entity.getY() + dy;
            double newZ = entity.getZ() + dz;
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.teleportTo((ServerLevel) level, newX, newY, newZ, serverPlayer.getYRot(), serverPlayer.getXRot());
            } else {
                entity.setPos(newX, newY, newZ);
                entity.hurtMarked = true;
            }
            entity.resetFallDistance();
        }

        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // Löscht jetzt einfach über die UUID
    public static void removeShipInstance(Level level, UUID shipId) {
        if (ACTIVE_SHIPS.remove(shipId) != null) {
            System.out.println("Schiffsinstanz mit UUID: " + shipId + " wurde zerstört!");
            if (level instanceof ServerLevel serverLevel) {
                ShipSavedData.get(serverLevel).setDirty();
            }
        }
    }

}