package com.peaceman.alpha.block;

import com.peaceman.alpha.client.screen.SpaceshipControlScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// HIER NEU: implements EntityBlock
public class SpaceshipControlBlock extends Block implements EntityBlock {

    public SpaceshipControlBlock(Properties properties) {
        super(properties);
    }

    // HIER NEU: Diese Methode wird automatisch aufgerufen, wenn der Block platziert wird
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipControlBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            openScreen(pos);
        }
        return InteractionResult.SUCCESS;
    }

    private void openScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new SpaceshipControlScreen(pos));
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {

            if (!level.isClientSide()) {
                // Wir holen den Rucksack des Blocks
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);

                // Wenn es unserer ist und er eine UUID hat...
                if (be instanceof SpaceshipControlBlockEntity shipBe && shipBe.getShipId() != null) {
                    // ...löschen wir genau dieses Schiff!
                    com.peaceman.alpha.ship.SpaceshipManager.removeShipInstance(level, shipBe.getShipId());
                }
            }

            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}