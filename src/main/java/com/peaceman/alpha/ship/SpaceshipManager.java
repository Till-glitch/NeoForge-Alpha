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
        // Wir können das Schiff jetzt blitzschnell und direkt aus der Liste fischen
        Spaceship ship = ACTIVE_SHIPS.get(shipId);

        if (ship == null || ship.getBlocks().isEmpty()) {
            System.out.println("Fehler: Konnte kein Schiff mit dieser UUID finden!");
            return;
        }

        Set<BlockPos> shipBlocks = ship.getBlocks();
        
        // --- ENERGIE KOSTEN BERECHNEN ---
        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        int energyCost = shipBlocks.size() * distance * 50;
        
        List<com.peaceman.alpha.block.SpaceshipReactorBlockEntity> reactors = new java.util.ArrayList<>();
        int totalEnergyAvailable = 0;
        
        for (BlockPos pos : shipBlocks) {
            if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.SpaceshipReactorBlockEntity reactor) {
                reactors.add(reactor);
                totalEnergyAvailable += reactor.getEnergyStorage().getEnergyStored();
            }
        }
        
        if (totalEnergyAvailable < energyCost) {
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Nicht genug Energie! Benötigt: " + energyCost + " FE"), true);
            }
            return;
        }
        
        // --- ENERGIE ABZIEHEN ---
        int remainingCost = energyCost;
        for (com.peaceman.alpha.block.SpaceshipReactorBlockEntity reactor : reactors) {
            if (remainingCost <= 0) break;
            int extracted = reactor.getEnergyStorage().extractEnergy(remainingCost, false);
            remainingCost -= extracted;
        }

        BlockPos startPos = ship.getControllerPos(); // Wir holen uns die alte Position

        // ... (Dein bisheriger Code zum Berechnen der AABB und Snapshot-Machen bleibt
        // gleich) ...
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : shipBlocks) {
            if (pos.getX() < minX)
                minX = pos.getX();
            if (pos.getY() < minY)
                minY = pos.getY();
            if (pos.getZ() < minZ)
                minZ = pos.getZ();
            if (pos.getX() > maxX)
                maxX = pos.getX();
            if (pos.getY() > maxY)
                maxY = pos.getY();
            if (pos.getZ() > maxZ)
                maxZ = pos.getZ();
        }
        AABB shipBounds = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 3, maxZ + 2);
        List<Entity> entitiesToMove = level.getEntities(null, shipBounds).stream().filter(entity -> {
            BlockPos entityPos = entity.blockPosition();
            if (shipBlocks.contains(entityPos) || shipBlocks.contains(entityPos.below()))
                return true;
            for (Direction dir : Direction.values()) {
                if (shipBlocks.contains(entityPos.relative(dir)))
                    return true;
                if (shipBlocks.contains(entityPos.below().relative(dir)))
                    return true;
            }
            return false;
        }).toList();

        // --- 1. DER PERFEKTE SCHNAPPSCHUSS (Mit Inventaren) ---
        Map<BlockPos, BlockData> snapshot = new HashMap<>();

        for (BlockPos pos : shipBlocks) {
            BlockState state = level.getBlockState(pos);
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            net.minecraft.nbt.CompoundTag nbt = null;

            if (be != null) {
                // Speichert das komplette Inventar, Ofen-Laufzeiten etc. in eine Variable
                nbt = be.saveWithFullMetadata(level.registryAccess());
            }
            snapshot.put(pos, new BlockData(state, nbt));
        }

        // --- 1.5. INVENTARE BRUTAL LÖSCHEN (MOD-KOMPATIBILITÄT) ---
        for (BlockPos pos : shipBlocks) {
            if (level.getBlockEntity(pos) != null) {
                level.removeBlockEntity(pos); // Verhindert Drops beim Abbauen
            }
        }

        // --- 2. ZIEL-POSITION BEREINIGEN (KOLLISION) ---
        Set<BlockPos> newShipBlocks = new HashSet<>();
        for (BlockPos pos : shipBlocks) {
            newShipBlocks.add(pos.offset(dx, dy, dz));
        }

        for (BlockPos newPos : newShipBlocks) {
            // Wenn an der neuen Position kein Teil unseres aktuellen Schiffs ist und es
            // nicht Luft ist
            if (!shipBlocks.contains(newPos) && !level.getBlockState(newPos).isAir()) {
                level.destroyBlock(newPos, true);
            }
        }

        // --- 3. DEN KONTROLLBLOCK VORBEREITEN ---
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {
            be.setShipId(null);
        }

        BlockPos newStartPos = startPos.offset(dx, dy, dz);
        ship.setControllerPos(newStartPos);

        // --- 4. NEUES SCHIFF INKL. INVENTAREN AUFBAUEN (ZWEI PHASEN FÜR
        // REDSTONE/FACKELN) ---
        List<Map.Entry<BlockPos, BlockData>> solidBlocks = new ArrayList<>();
        List<Map.Entry<BlockPos, BlockData>> fragileBlocks = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
            if (entry.getValue().state().getCollisionShape(level, entry.getKey()).isEmpty()) {
                fragileBlocks.add(entry);
            } else {
                solidBlocks.add(entry);
            }
        }

        // Zuerst harte Blöcke (Böden/Wände) platzieren
        for (Map.Entry<BlockPos, BlockData> entry : solidBlocks) {
            placeBlockFromSnapshot(level, entry, dx, dy, dz, shipId);
        }
        // Danach zerbrechliche Blöcke (Fackeln/Redstone), die Halt brauchen
        for (Map.Entry<BlockPos, BlockData> entry : fragileBlocks) {
            placeBlockFromSnapshot(level, entry, dx, dy, dz, shipId);
        }

        ship.setBlocks(newShipBlocks);

        // --- 5. ENTITIES TELEPORTIEREN (Vor dem Abbau des alten Schiffs) ---
        for (Entity entity : entitiesToMove) {
            double newX = entity.getX() + dx;
            double newY = entity.getY() + dy;
            double newZ = entity.getZ() + dz;
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.teleportTo((ServerLevel) level, newX, newY, newZ, serverPlayer.getYRot(),
                        serverPlayer.getXRot());
            } else {
                entity.setPos(newX, newY, newZ);
                entity.hurtMarked = true;
            }
            entity.resetFallDistance();
        }

        // --- 6. ALTES SCHIFF IN LUFT AUFLÖSEN ---
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

        // Zuerst Fackeln und Redstone löschen
        for (BlockPos pos : fragileOld) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
        }
        // Dann Böden und Wände löschen
        for (BlockPos pos : solidOld) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
        }

        // Jetzt updaten wir alle neuen Blöcke, damit sich Redstone, Zäune und
        // Glasscheiben verbinden
        for (BlockPos pos : newShipBlocks) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }

        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }

    private static void placeBlockFromSnapshot(Level level, Map.Entry<BlockPos, BlockData> entry, int dx, int dy,
            int dz, UUID shipId) {
        BlockPos newPos = entry.getKey().offset(dx, dy, dz);
        BlockState state = entry.getValue().state();
        net.minecraft.nbt.CompoundTag nbt = entry.getValue().nbt();

        level.setBlock(newPos, state, 50);

        if (nbt != null) {
            nbt.putInt("x", newPos.getX());
            nbt.putInt("y", newPos.getY());
            nbt.putInt("z", newPos.getZ());

            net.minecraft.world.level.block.entity.BlockEntity newBe = net.minecraft.world.level.block.entity.BlockEntity
                    .loadStatic(newPos, state, nbt, level.registryAccess());

            if (newBe != null) {
                level.setBlockEntity(newBe);
            }
        }

        if (level.getBlockEntity(newPos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
            node.setShipId(shipId);
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

    // Speichert die aktuelle Position als neues Home
    public static void saveHome(Level level, UUID shipId, String homeName) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        if (ship != null) {
            // Speichert den Namen und die aktuelle Position des Kontrollblocks
            ship.addHome(homeName, ship.getControllerPos());
            System.out.println("Wegpunkt '" + homeName + "' erfolgreich gespeichert!");

            if (level instanceof ServerLevel serverLevel) {
                ShipSavedData.get(serverLevel).setDirty();
            }
        }
    }

    // Teleportiert das Schiff zu einem gespeicherten Home
    public static void teleportToHome(Level level, UUID shipId, String homeName, net.minecraft.world.entity.player.Player player) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);

        if (ship != null && ship.getHomes().containsKey(homeName)) {
            BlockPos targetPos = ship.getHomes().get(homeName);
            BlockPos currentPos = ship.getControllerPos();

            // Rechnet die nötige Verschiebung aus!
            int dx = targetPos.getX() - currentPos.getX();
            int dy = targetPos.getY() - currentPos.getY();
            int dz = targetPos.getZ() - currentPos.getZ();

            // Wir nutzen unsere geniale Bewegungs-Methode für den Teleport!
            moveShipInstance(level, shipId, dx, dy, dz, player);
            System.out.println("Schiff erfolgreich zu '" + homeName + "' teleportiert!");
        } else {
            System.out.println("Fehler: Wegpunkt '" + homeName + "' nicht gefunden!");
        }
    }

    // 1. SCHIFF INITIALISIEREN (Macht nur etwas, wenn der Block noch leer ist)
    public static void createShip(Level level, BlockPos startPos) {
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {

            // Schutz: Wenn der Rucksack schon eine UUID hat, brechen wir ab!
            if (be.getShipId() != null && ACTIVE_SHIPS.containsKey(be.getShipId())) {
                System.out.println("Fehler: Block ist bereits mit einem Schiff verknüpft!");
                return;
            }

            Set<BlockPos> shipBlocks = scanSpaceship(level, startPos);
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
            Set<BlockPos> newBlocks = scanSpaceship(level, startPos);
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