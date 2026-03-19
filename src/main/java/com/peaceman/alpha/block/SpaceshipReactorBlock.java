package com.peaceman.alpha.block;

import com.peaceman.alpha.menu.SpaceshipReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

public class SpaceshipReactorBlock extends BaseEntityBlock  {
    public static final com.mojang.serialization.MapCodec<SpaceshipReactorBlock> CODEC = simpleCodec(SpaceshipReactorBlock::new);
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public SpaceshipReactorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return this.defaultBlockState()
                .setValue(UP, level.getBlockState(pos.above()).getBlock() == this)
                .setValue(DOWN, level.getBlockState(pos.below()).getBlock() == this);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(UP, neighborState.getBlock() == this);
        }
        if (direction == Direction.DOWN) {
            return state.setValue(DOWN, neighborState.getBlock() == this);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpaceshipReactorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpaceshipReactorBlockEntity reactorEntity) {
                player.openMenu(reactorEntity, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
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

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpaceshipReactorBlockEntity) {
            return (MenuProvider) blockEntity;
        }
        return null;
    }

}
