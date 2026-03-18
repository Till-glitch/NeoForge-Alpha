package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class SpaceshipMover {

    // Unser Daten-Container für den perfekten Klon
    public record BlockData(BlockState state, CompoundTag nbt) {}

    public static void moveShip(Level level, Spaceship ship, int dx, int dy, int dz, Player player) {
        Set<BlockPos> shipBlocks = ship.getBlocks();

        // --- 1. ENERGIE PRÜFEN (Ausgelagert in SpaceshipEnergyManager) ---
        if (!SpaceshipEnergyManager.tryConsumeFlightEnergy(level, ship, dx, dy, dz, player)) {
            return; // Abbruch, wenn wir nicht fliegen dürfen!
        }

        BlockPos startPos = ship.getControllerPos();

        // --- 2. BOUNDING BOX FÜR ENTITIES BERECHNEN ---
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
        AABB shipBounds = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 3, maxZ + 2);

        List<Entity> entitiesToMove = level.getEntities(null, shipBounds).stream().filter(entity -> {
            BlockPos entityPos = entity.blockPosition();
            if (shipBlocks.contains(entityPos) || shipBlocks.contains(entityPos.below())) return true;
            for (Direction dir : Direction.values()) {
                if (shipBlocks.contains(entityPos.relative(dir))) return true;
                if (shipBlocks.contains(entityPos.below().relative(dir))) return true;
            }
            return false;
        }).toList();

        // --- 3. SCHNAPPSCHUSS MACHEN ---
        Map<BlockPos, BlockData> snapshot = new HashMap<>();
        for (BlockPos pos : shipBlocks) {
            BlockState state = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            CompoundTag nbt = (be != null) ? be.saveWithFullMetadata(level.registryAccess()) : null;
            snapshot.put(pos, new BlockData(state, nbt));
        }

        // --- 4. INVENTARE SICHERN (Verhindert Item-Drops beim Löschen) ---
        for (BlockPos pos : shipBlocks) {
            if (level.getBlockEntity(pos) != null) {
                level.removeBlockEntity(pos);
            }
        }

        // --- 5. ZIELPOSITION BEREINIGEN ---
        Set<BlockPos> newShipBlocks = new HashSet<>();
        for (BlockPos pos : shipBlocks) {
            newShipBlocks.add(pos.offset(dx, dy, dz));
        }
        for (BlockPos newPos : newShipBlocks) {
            if (!shipBlocks.contains(newPos) && !level.getBlockState(newPos).isAir()) {
                level.destroyBlock(newPos, true);
            }
        }

        // --- 6. CONTROLLER UMZIEHEN ---
        if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {
            be.setShipId(null);
        }
        BlockPos newStartPos = startPos.offset(dx, dy, dz);
        ship.setControllerPos(newStartPos);

        // --- 7. NEUE BLÖCKE SETZEN (Zwei Phasen: Fest -> Zerbrechlich) ---
        List<Map.Entry<BlockPos, BlockData>> solidBlocks = new ArrayList<>();
        List<Map.Entry<BlockPos, BlockData>> fragileBlocks = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
            if (entry.getValue().state().getCollisionShape(level, entry.getKey()).isEmpty()) {
                fragileBlocks.add(entry);
            } else {
                solidBlocks.add(entry);
            }
        }

        for (Map.Entry<BlockPos, BlockData> entry : solidBlocks) {
            placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId());
        }
        for (Map.Entry<BlockPos, BlockData> entry : fragileBlocks) {
            placeBlockFromSnapshot(level, entry, dx, dy, dz, ship.getId());
        }

        ship.setBlocks(newShipBlocks, level);

        // --- 8. ENTITIES TELEPORTIEREN ---
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

        // --- 9. ALTE BLÖCKE LÖSCHEN (Zwei Phasen: Zerbrechlich -> Fest) ---
        List<BlockPos> solidOld = new ArrayList<>();
        List<BlockPos> fragileOld = new ArrayList<>();
        for (BlockPos pos : shipBlocks) {
            if (!newShipBlocks.contains(pos)) {
                if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                    fragileOld.add(pos);
                } else {
                    solidOld.add(pos);
                }
            }
        }

        for (BlockPos pos : fragileOld) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
        for (BlockPos pos : solidOld) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);

        // --- 10. UPDATES & SPEICHERN ---
        for (BlockPos pos : newShipBlocks) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }

        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    private static void placeBlockFromSnapshot(Level level, Map.Entry<BlockPos, BlockData> entry, int dx, int dy, int dz, UUID shipId) {
        BlockPos newPos = entry.getKey().offset(dx, dy, dz);
        BlockState state = entry.getValue().state();
        CompoundTag nbt = entry.getValue().nbt();

        level.setBlock(newPos, state, 50);

        if (nbt != null) {
            nbt.putInt("x", newPos.getX());
            nbt.putInt("y", newPos.getY());
            nbt.putInt("z", newPos.getZ());
            BlockEntity newBe = BlockEntity.loadStatic(newPos, state, nbt, level.registryAccess());
            if (newBe != null) {
                level.setBlockEntity(newBe);
            }
        }

        if (level.getBlockEntity(newPos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
            node.setShipId(shipId);
        }
    }
}