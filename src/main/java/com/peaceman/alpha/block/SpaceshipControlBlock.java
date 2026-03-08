package com.peaceman.alpha.block;

import com.peaceman.alpha.client.screen.SpaceshipControlScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SpaceshipControlBlock extends Block {

    public SpaceshipControlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            openScreen(pos);
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // WICHTIG: Wir prüfen, ob der Block WIRKLICH durch einen anderen (z.B. Luft) ersetzt wird.
        // Das verhindert, dass das Schiff gelöscht wird, wenn sich nur eine Block-Eigenschaft ändert.
        if (!state.is(newState.getBlock())) {

            // Nur der Server darf Daten löschen
            if (!level.isClientSide()) {
                com.peaceman.alpha.ship.SpaceshipManager.removeShipInstance(level, pos);
            }

            // Das normale Abbau-Verhalten von Minecraft weiterlaufen lassen (z.B. Item droppen)
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private void openScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new SpaceshipControlScreen(pos));
    }
}