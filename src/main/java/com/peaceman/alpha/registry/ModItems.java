package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.item.BackflipToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alpha.MODID);

    // Block-Items
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block",
            ModBlocks.EXAMPLE_BLOCK);
    public static final DeferredItem<BlockItem> SPACESHIP_CONTROL_ITEM = ITEMS
            .registerSimpleBlockItem("spaceship_control", ModBlocks.SPACESHIP_CONTROL);
    public static final DeferredItem<BlockItem> SPACESHIP_HELM_ITEM = ITEMS.registerSimpleBlockItem("spaceship_helm",
            ModBlocks.SPACESHIP_HELM);
    public static final DeferredItem<BlockItem> SPACESHIP_REACTOR_ITEM = ITEMS.registerSimpleBlockItem("spaceship_reactor",
            ModBlocks.SPACESHIP_REACTOR);
    public static final DeferredItem<BlockItem> SPACESHIP_SHIELD_ITEM = ITEMS.registerSimpleBlockItem("spaceship_shield",
            ModBlocks.SPACESHIP_SHIELD);

    // Items
    public static final DeferredItem<Item> BACKFLIP_TOOL = ITEMS.register("backflip_tool",
            () -> new BackflipToolItem(new Item.Properties()
                    .durability(250)
                    .attributes(BackflipToolItem.createAttributes())));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
