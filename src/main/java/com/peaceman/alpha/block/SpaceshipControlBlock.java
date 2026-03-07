package com.peaceman.alpha.block;

import com.peaceman.alpha.client.screen.SpaceshipControlScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
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