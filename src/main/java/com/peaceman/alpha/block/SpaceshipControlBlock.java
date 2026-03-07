package com.peaceman.alpha.block;

import com.peaceman.alpha.client.screen.SpaceshipControlScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;

import net.minecraft.core.Direction;

public class SpaceshipControlBlock extends Block {

    public SpaceshipControlBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            openScreen(pos); // Hier übergeben wir jetzt die Koordinaten!
        }
        return InteractionResult.SUCCESS;
    }

    private void openScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new SpaceshipControlScreen(pos));
    }

    // Unser "Gedächtnis" für alle aktiven Schiffe auf dem Server
    public static final Map<BlockPos, Set<BlockPos>> ACTIVE_SHIPS = new HashMap<>();

    // Wird aufgerufen, wenn man auf "Scan" drückt
    public static void createShipInstance(Level level, BlockPos startPos) {
        Set<BlockPos> shipBlocks = scanSpaceship(level, startPos); // Deinen alten Scanner aufrufen
        ACTIVE_SHIPS.put(startPos, shipBlocks); // Das Schiff im Gedächtnis speichern!
        System.out.println("Schiff instanziiert mit " + shipBlocks.size() + " Blöcken!");
    }

    // Wird aufgerufen, wenn man fliegen will
    // Wir tauschen "int distance" gegen "int dx, int dy, int dz" aus
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

        // HIER NEU: startPos.offset verschiebt die Koordinate in alle Richtungen
        BlockPos newStartPos = startPos.offset(dx, dy, dz);

        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            // HIER NEU: Offset auf jeden Block anwenden
            BlockPos newPos = entry.getKey().offset(dx, dy, dz);
            level.setBlock(newPos, entry.getValue(), 3);
            newShipBlocks.add(newPos);
        }

        // 6. DAS WICHTIGSTE: Die Passagiere mitnehmen!
        for (Entity entity : entitiesToMove) {
            double newX = entity.getX() + dx;
            double newY = entity.getY() + dy;
            double newZ = entity.getZ() + dz;

            if (entity instanceof ServerPlayer serverPlayer) {
                // Bei echten Spielern MÜSSEN wir die offizielle Teleport-Methode nutzen,
                // damit der Server dem Client zwingend sagt: "Du bist jetzt hier!"
                serverPlayer.teleportTo((ServerLevel) level, newX, newY, newZ, serverPlayer.getYRot(), serverPlayer.getXRot());
            } else {
                // Für Items, Tiere, Loren etc. reicht das normale setPos
                entity.setPos(newX, newY, newZ);
                entity.hurtMarked = true;
            }

            // Verhindert weiterhin Fallschaden für alle
            entity.resetFallDistance();
        }

        ACTIVE_SHIPS.remove(startPos);
        ACTIVE_SHIPS.put(newStartPos, newShipBlocks);
    }
    public static Set<BlockPos> scanSpaceship(Level level, BlockPos startPos) {
        Set<BlockPos> shipBlocks = new HashSet<>(); // Hier speichern wir alle gefundenen Blöcke
        Queue<BlockPos> queue = new LinkedList<>(); // Warteschlange für Blöcke, die wir noch prüfen müssen

        queue.add(startPos);
        shipBlocks.add(startPos);

        int maxBlocks = 2000; // WICHTIG: Ein Limit! Sonst stürzt der Server ab, wenn man aus Versehen den ganzen Planeten scannt.

        while (!queue.isEmpty() && shipBlocks.size() < maxBlocks) {
            BlockPos current = queue.poll();

            // Alle 6 Richtungen prüfen
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                // Wenn der Nachbar KEINE Luft ist und wir ihn noch nicht in der Liste haben:
                if (!level.getBlockState(neighbor).isAir() && !shipBlocks.contains(neighbor)) {
                    shipBlocks.add(neighbor); // Zum Schiff hinzufügen
                    queue.add(neighbor);      // In die Warteschlange packen, um dessen Nachbarn später auch zu prüfen
                }
            }
        }

        System.out.println("Scan abgeschlossen! Das Schiff besteht aus " + shipBlocks.size() + " Blöcken.");
        return shipBlocks;
    }
}