package com.peaceman.alpha.block;

import com.peaceman.alpha.block.entity.SpaceshipHelmBlockEntity;
import com.peaceman.alpha.client.screen.SpaceshipHelmScreen;
import net.minecraft.client.Minecraft;
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

public class SpaceshipHelmBlock extends Block implements EntityBlock {

    public SpaceshipHelmBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipHelmBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        // 1. Wir prüfen die Logik: Sind wir der Client?
        if (level.isClientSide()) {
            // 2. Wir rufen unsere Puffer-Klasse auf.
            // Weil wir den vollen Pfad (com.peaceman...) angeben, sparen wir uns den Import oben.
            com.peaceman.alpha.client.ClientHooks.openHelmScreen(pos);
        }

        return InteractionResult.SUCCESS;
    }
}