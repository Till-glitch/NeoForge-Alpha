package com.peaceman.alpha.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import com.peaceman.alpha.Alpha;
import java.util.*;
@EventBusSubscriber(modid = Alpha.MODID)
public class SpaceshipManager {

    // Unser Gedächtnis für alle Schiffe
    public static final Map<BlockPos, Set<BlockPos>> ACTIVE_SHIPS = new HashMap<>();

    // 1. Der Scanner (Flood-Fill)
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

    // 2. Schiff als Instanz abspeichern
    public static void createShipInstance(Level level, BlockPos startPos) {
        Set<BlockPos> shipBlocks = scanSpaceship(level, startPos);
        ACTIVE_SHIPS.put(startPos, shipBlocks);
        System.out.println("Schiff instanziiert mit " + shipBlocks.size() + " Blöcken!");
        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // 3. Die Bewegung
    public static void moveShipInstance(Level level, BlockPos startPos, int dx, int dy, int dz) {
        Set<BlockPos> shipBlocks = ACTIVE_SHIPS.get(startPos);

        if (shipBlocks == null || shipBlocks.isEmpty()) {
            System.out.println("Fehler: Du musst das Schiff zuerst scannen!");
            return;
        }

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

        for (BlockPos pos : shipBlocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

        Set<BlockPos> newShipBlocks = new HashSet<>();
        BlockPos newStartPos = startPos.offset(dx, dy, dz);

        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            level.setBlock(newPos, entry.getValue(), 3);
            newShipBlocks.add(newPos);
        }

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

        ACTIVE_SHIPS.remove(startPos);
        ACTIVE_SHIPS.put(newStartPos, newShipBlocks);
        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // Wird aufgerufen, wenn der Kontrollblock abgebaut wird
    public static void removeShipInstance(Level level, BlockPos pos) {
        if (ACTIVE_SHIPS.containsKey(pos)) {
            ACTIVE_SHIPS.remove(pos); // Aus dem Gedächtnis löschen
            System.out.println("Schiffsinstanz an " + pos + " wurde gelöscht!");

            // Die Änderung in der Welt-Datei speichern
            if (level instanceof ServerLevel serverLevel) {
                ShipSavedData.get(serverLevel).setDirty();
            }
        }
    }
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Löst das Laden der Datei beim Serverstart aus
        ShipSavedData.get(event.getServer().overworld());
        System.out.println("=== RAUMSCHIFFE GELADEN: " + ACTIVE_SHIPS.size() + " ===");
    }
}