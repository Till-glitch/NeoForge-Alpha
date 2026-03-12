package com.peaceman.alpha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class SpaceshipReactorBlock extends Block implements EntityBlock {

    public SpaceshipReactorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipReactorBlockEntity(pos, state);
    }

    // 1. Wird aufgerufen, wenn der Spieler ein Item (z.B. Redstone) in der Hand
    // hält
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {

        // --- DER ENTWICKLER-CHEAT ---
        if (stack.is(net.minecraft.world.item.Items.REDSTONE)) {
            if (!level.isClientSide()) {
                if (level.getBlockEntity(pos) instanceof SpaceshipReactorBlockEntity be) {
                    be.getEnergyStorage().receiveEnergy(50000, false);
                    be.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("DEV-CHEAT: 50.000 FE geladen!"));
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Wenn es kein Redstone ist, geben wir an die Methode für leere Hände weiter!
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 2. Wird aufgerufen, wenn der Spieler KEIN Item (oder ein irrelevantes Item)
    // in der Hand hält
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {

        // --- DAS NORMALE MENÜ ---
        if (level.isClientSide()) {
            openScreen(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new com.peaceman.alpha.client.screen.SpaceshipReactorScreen(pos));
    }
}
