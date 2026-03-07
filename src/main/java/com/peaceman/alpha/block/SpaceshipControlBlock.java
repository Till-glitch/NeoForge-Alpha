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

    private void openScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new SpaceshipControlScreen(pos));
    }
}