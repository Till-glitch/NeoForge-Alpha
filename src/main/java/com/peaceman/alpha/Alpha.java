package com.peaceman.alpha;

import com.mojang.logging.LogUtils;
import com.peaceman.alpha.network.ShipCommandPayload;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModCreativeTabs;
import com.peaceman.alpha.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Alpha.MODID)
public class Alpha {
    public static final String MODID = "peaceman_alpha";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alpha(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Registriert unsere Netzwerk-Pakete (für die Raumschiff-Steuerung)
        modEventBus.addListener(this::registerNetwork);

        // 2. Ruft unsere aufgeräumten Register-Klassen auf
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }

    // Kümmert sich darum, dass Client und Server miteinander reden können
    private void registerNetwork(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1.0");
        registrar.playToServer(
                ShipCommandPayload.TYPE,
                ShipCommandPayload.STREAM_CODEC,
                ShipCommandPayload::handleData
        );
    }
}