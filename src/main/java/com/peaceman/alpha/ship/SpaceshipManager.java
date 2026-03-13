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

    // Unser neues, objektorientiertes Gedächtnis (Speichert Schiffe nach ihrer
    // eindeutigen UUID)
    public static final Map<UUID, Spaceship> ACTIVE_SHIPS = new HashMap<>();

    // Eine winzige Hilfsklasse für unseren perfekten Schnappschuss!
    private record BlockData(BlockState state, net.minecraft.nbt.CompoundTag nbt) {
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

        ensureMultipartBlocks(level, shipBlocks);

        return shipBlocks;
    }

    private static void ensureMultipartBlocks(Level level, Set<BlockPos> shipBlocks) {
        Set<BlockPos> toAdd = new HashSet<>();
        for (BlockPos pos : shipBlocks) {
            BlockState state = level.getBlockState(pos);

            if (state.hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                if (state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                    toAdd.add(pos.above());
                } else {
                    toAdd.add(pos.below());
                }
            }

            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)) {
                Direction facing = state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                if (state.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
                    toAdd.add(pos.relative(facing.getOpposite()));
                } else {
                    toAdd.add(pos.relative(facing));
                }
            }

            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE)) {
                net.minecraft.world.level.block.state.properties.ChestType type = state
                        .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE);
                if (type != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = pos.relative(dir);
                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.getBlock() == state.getBlock() &&
                                neighborState.hasProperty(
                                        net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE)
                                &&
                                neighborState.getValue(
                                        net.minecraft.world.level.block.state.properties.BlockStateProperties.CHEST_TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {

                            if (neighborState.getValue(
                                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) == state
                                            .getValue(
                                                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                                toAdd.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        shipBlocks.addAll(toAdd);
    }

    // Wir übergeben jetzt die UUID anstatt der Startposition!
    public static void moveShipInstance(Level level, UUID shipId, int dx, int dy, int dz, net.minecraft.world.entity.player.Player player) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);

        if (ship == null || ship.getBlocks().isEmpty()) {
            System.out.println("Fehler: Konnte kein Schiff mit dieser UUID finden!");
            return;
        }

        // Wir geben die gesamte Arbeit einfach an unseren neuen Mover ab!
        SpaceshipMover.moveShip(level, ship, dx, dy, dz, player);
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
    // Speichert die aktuelle Position als neues Home
    public static void saveHome(Level level, UUID shipId, String homeName) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        SpaceshipNavigationManager.saveHome(level, ship, homeName);
    }

    // Teleportiert das Schiff zu einem gespeicherten Home
    public static void teleportToHome(Level level, UUID shipId, String homeName, net.minecraft.world.entity.player.Player player) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        SpaceshipNavigationManager.teleportToHome(level, ship, homeName, player);
    }


    public static void createShip(Level level, BlockPos startPos) {
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {

            // Schutz: Wenn der Rucksack schon eine UUID hat, brechen wir ab!
            if (be.getShipId() != null && ACTIVE_SHIPS.containsKey(be.getShipId())) {
                System.out.println("Fehler: Block ist bereits mit einem Schiff verknüpft!");
                return;
            }

            Set<BlockPos> shipBlocks = SpaceshipScanner.scan(level, startPos);
            Spaceship newShip = new Spaceship(startPos, shipBlocks);
            ACTIVE_SHIPS.put(newShip.getId(), newShip);
            
            for (BlockPos pos : shipBlocks) {
                if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    node.setShipId(newShip.getId());
                }
            }

            System.out.println("Neues Schiff erstellt! UUID: " + newShip.getId());
            if (level instanceof ServerLevel serverLevel)
                ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // 2. STRUKTUR AKTUALISIEREN (Lässt UUID und Homes in Ruhe, updatet nur die
    // Blöcke)
    public static void updateShipBlocks(Level level, BlockPos startPos, UUID shipId) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        if (ship != null) {
            Set<BlockPos> newBlocks = SpaceshipScanner.scan(level, startPos);
            ship.setBlocks(newBlocks);
            for (BlockPos pos : newBlocks) {
                if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    node.setShipId(shipId);
                }
            }
            System.out.println("Struktur aktualisiert! Neue Block-Anzahl: " + newBlocks.size());
            if (level instanceof ServerLevel serverLevel)
                ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // 3. SCHIFF MANUELL AUFLÖSEN (Löscht das Schiff und leert den Block)
    public static void deleteShipFromBlock(Level level, BlockPos startPos, UUID shipId) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        if (ship != null) {
            for (BlockPos pos : ship.getBlocks()) {
                if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    node.setShipId(null);
                }
            }
        }
        removeShipInstance(level, shipId); // Ruft unsere bestehende Lösch-Methode auf
    }
}