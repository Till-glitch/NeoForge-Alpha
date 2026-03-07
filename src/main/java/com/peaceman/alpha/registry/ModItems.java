package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alpha.MODID);

    // Block-Items
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", ModBlocks.EXAMPLE_BLOCK);
    public static final DeferredItem<BlockItem> SPACESHIP_CONTROL_ITEM = ITEMS.registerSimpleBlockItem("spaceship_control", ModBlocks.SPACESHIP_CONTROL);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}