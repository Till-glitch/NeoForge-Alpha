package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.screen.SpaceshipReactorScreen;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.REACTOR_MENU.get(), SpaceshipReactorScreen::new);
    }
}
