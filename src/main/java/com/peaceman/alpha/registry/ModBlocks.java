package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // 1. Das Register für Blöcke erstellen
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alpha.MODID);

    // 2. Deine Blöcke eintragen
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

    public static final DeferredBlock<Block> SPACESHIP_CONTROL = BLOCKS.register("spaceship_control",
            () -> new SpaceshipControlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)));

    public static final DeferredBlock<Block> SPACESHIP_HELM = BLOCKS.register("spaceship_helm",
            () -> new com.peaceman.alpha.block.SpaceshipHelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f)));

    // 3. Diese Methode ruft unsere Hauptklasse später auf
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}