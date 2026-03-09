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

    // Eine winzige Hilfsklasse für unseren perfekten Schnappschuss!
    private record BlockData(BlockState state, net.minecraft.nbt.CompoundTag nbt) {}
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

        // --- 1. DER PERFEKTE SCHNAPPSCHUSS (Mit Inventaren) ---
        Map<BlockPos, BlockData> snapshot = new HashMap<>();

        for (BlockPos pos : shipBlocks) {
            BlockState state = level.getBlockState(pos);
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            net.minecraft.nbt.CompoundTag nbt = null;

            if (be != null) {
                // Speichert das komplette Inventar, Ofen-Laufzeiten etc. in eine Variable
                nbt = be.saveWithFullMetadata(level.registryAccess());

                // WICHTIG: Wir leeren die Kiste im Spiel, BEVOR wir sie abbauen!
                // Sonst regnet es beim Teleportieren Items aus dem Schiff.
                net.minecraft.world.Clearable.tryClear(be);
            }
            snapshot.put(pos, new BlockData(state, nbt));
        }

        // --- 2. DEN KONTROLLBLOCK VORBEREITEN ---
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {
            be.setShipId(null);
        }

        BlockPos newStartPos = startPos.offset(dx, dy, dz);
        ship.setControllerPos(newStartPos);

        // --- 3. ALTES SCHIFF IN LUFT AUFLÖSEN ---
        for (BlockPos pos : shipBlocks) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
        }

        // --- 4. NEUES SCHIFF INKL. INVENTAREN AUFBAUEN ---
        Set<BlockPos> newShipBlocks = new HashSet<>();
        for (Map.Entry<BlockPos, BlockData> entry : snapshot.entrySet()) {
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            BlockState state = entry.getValue().state();
            net.minecraft.nbt.CompoundTag nbt = entry.getValue().nbt();

            // Setzt den eigentlichen Block
            level.setBlock(newPos, state, 50);
            newShipBlocks.add(newPos);

            // Wenn der Block einen Rucksack (Inventar) hatte, geben wir ihn zurück!
            if (nbt != null) {
                // WICHTIG: Im NBT stehen noch die ALTEN X/Y/Z Koordinaten.
                // Wenn wir die nicht anpassen, stürzt das Spiel ab!
                nbt.putInt("x", newPos.getX());
                nbt.putInt("y", newPos.getY());
                nbt.putInt("z", newPos.getZ());

                // Erstellt die Kiste/Ofen aus den Daten neu und setzt sie in die Welt
                net.minecraft.world.level.block.entity.BlockEntity newBe =
                        net.minecraft.world.level.block.entity.BlockEntity.loadStatic(newPos, state, nbt, level.registryAccess());

                if (newBe != null) {
                    level.setBlockEntity(newBe);
                }
            }

            // Unser eigenes Kontrollblock-System (UUID wiedergeben)
            if (state.is(com.peaceman.alpha.registry.ModBlocks.SPACESHIP_CONTROL.get())) {
                if (level.getBlockEntity(newPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity newBe) {
                    newBe.setShipId(shipId);
                }
            }
        }

        ship.setBlocks(newShipBlocks);

        // Jetzt, wo alles sicher steht, updaten wir alle Blöcke,
        // damit sich Redstone, Zäune und Glasscheiben wieder richtig verbinden!
        for (BlockPos pos : newShipBlocks) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }

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
    public static void teleportToHome(Level level, UUID shipId, String homeName) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);

        if (ship != null && ship.getHomes().containsKey(homeName)) {
            BlockPos targetPos = ship.getHomes().get(homeName);
            BlockPos currentPos = ship.getControllerPos();

            // Rechnet die nötige Verschiebung aus!
            int dx = targetPos.getX() - currentPos.getX();
            int dy = targetPos.getY() - currentPos.getY();
            int dz = targetPos.getZ() - currentPos.getZ();

            // Wir nutzen unsere geniale Bewegungs-Methode für den Teleport!
            moveShipInstance(level, shipId, dx, dy, dz);
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
            be.setShipId(newShip.getId()); // Rucksack füllen

            System.out.println("Neues Schiff erstellt! UUID: " + newShip.getId());
            if (level instanceof ServerLevel serverLevel) ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // 2. STRUKTUR AKTUALISIEREN (Lässt UUID und Homes in Ruhe, updatet nur die Blöcke)
    public static void updateShipBlocks(Level level, BlockPos startPos, UUID shipId) {
        Spaceship ship = ACTIVE_SHIPS.get(shipId);
        if (ship != null) {
            Set<BlockPos> newBlocks = scanSpaceship(level, startPos);
            ship.setBlocks(newBlocks);
            System.out.println("Struktur aktualisiert! Neue Block-Anzahl: " + newBlocks.size());
            if (level instanceof ServerLevel serverLevel) ShipSavedData.get(serverLevel).setDirty();
        }
    }

    // 3. SCHIFF MANUELL AUFLÖSEN (Löscht das Schiff und leert den Block)
    public static void deleteShipFromBlock(Level level, BlockPos startPos, UUID shipId) {
        removeShipInstance(level, shipId); // Ruft unsere bestehende Lösch-Methode auf

        // Leert den Rucksack des Blocks, damit man ihn neu initialisieren kann
        if (level.getBlockEntity(startPos) instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity be) {
            be.setShipId(null);
        }
    }
}