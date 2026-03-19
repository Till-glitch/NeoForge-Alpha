package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SpaceshipShieldBlock extends Block implements EntityBlock {

    public SpaceshipShieldBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipShieldBlockEntity(pos, state);
    }
}