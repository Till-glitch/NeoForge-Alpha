package com.peaceman.alpha.block;

import com.peaceman.alpha.client.screen.SpaceshipControlScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    public static void moveShip(Level level, BlockPos startPos, int distance) {
        // 1. Alle Blöcke finden
        Set<BlockPos> shipBlocks = scanSpaceship(level, startPos);
        Map<BlockPos, BlockState> snapshot = new HashMap<>();

        // 2. Zustand aller Blöcke speichern (Schnappschuss)
        for (BlockPos pos : shipBlocks) {
            snapshot.put(pos, level.getBlockState(pos));
        }

        // 3. Alle alten Blöcke löschen (bevor wir neue setzen, um Überschneidungen zu vermeiden!)
        for (BlockPos pos : shipBlocks) {
            // Flag 18 verhindert, dass z.B. Sand sofort fällt oder Wasser fließt, während das Schiff morpht
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }

        // 4. Das Schiff an der neuen Position aufbauen!
        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            BlockPos oldPos = entry.getKey();
            BlockState state = entry.getValue();

            // Rechnet die Distanz auf der Y-Achse (nach oben) drauf
            BlockPos newPos = oldPos.above(distance);

            // Block platzieren (Flag 3 ist der Standard-Wert für normale Block-Updates)
            level.setBlock(newPos, state, 3);
        }
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
    public static void moveShipInstance(Level level, BlockPos startPos, int distance) {
        // 1. Die Instanz aus dem Gedächtnis laden
        Set<BlockPos> shipBlocks = ACTIVE_SHIPS.get(startPos);

        if (shipBlocks == null || shipBlocks.isEmpty()) {
            System.out.println("Fehler: Du musst das Schiff zuerst scannen!");
            return;
        }

        Map<BlockPos, BlockState> snapshot = new HashMap<>();
        for (BlockPos pos : shipBlocks) snapshot.put(pos, level.getBlockState(pos));

        for (BlockPos pos : shipBlocks) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

        // Die neuen Positionen merken, damit wir die Instanz im Gedächtnis aktualisieren können!
        Set<BlockPos> newShipBlocks = new HashSet<>();
        BlockPos newStartPos = startPos.above(distance); // Der Kontrollblock bewegt sich ja auch mit

        for (Map.Entry<BlockPos, BlockState> entry : snapshot.entrySet()) {
            BlockPos newPos = entry.getKey().above(distance);
            level.setBlock(newPos, entry.getValue(), 3);
            newShipBlocks.add(newPos);
        }

        // 2. Das Gedächtnis aktualisieren (alte Position löschen, neue speichern)
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