package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    // 1. Das Register für BlockEntities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Alpha.MODID);

    // 2. Unsere BlockEntity eintragen und mit unserem Raumschiff-Block verknüpfen!
    public static final Supplier<BlockEntityType<SpaceshipControlBlockEntity>> SPACESHIP_CONTROL_BE =
            BLOCK_ENTITIES.register("spaceship_control_be", () ->
                    BlockEntityType.Builder.of(SpaceshipControlBlockEntity::new, ModBlocks.SPACESHIP_CONTROL.get()).build(null));

    public static final Supplier<BlockEntityType<com.peaceman.alpha.block.SpaceshipHelmBlockEntity>> SPACESHIP_HELM_BE =
            BLOCK_ENTITIES.register("spaceship_helm_be", () ->
                    BlockEntityType.Builder.of(com.peaceman.alpha.block.SpaceshipHelmBlockEntity::new, ModBlocks.SPACESHIP_HELM.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}