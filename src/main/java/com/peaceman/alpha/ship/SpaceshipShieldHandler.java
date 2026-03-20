package com.peaceman.alpha.event;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.ship.Spaceship;
import com.peaceman.alpha.ship.SpaceshipManager;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpaceshipShieldHandler {

    // Der Radius wird jetzt von der ShieldMorphology bestimmt,
    // wir brauchen hier also nur noch die Energiekosten!
    public static final int ENERGY_COST_PER_BLOCK = 50;

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        // Logik passiert IMMER nur auf dem Server
        if (level.isClientSide()) return;

        List<BlockPos> protectedBlocks = new ArrayList<>();

        // 1. Wir gehen alle aktiven Schiffe durch
        for (Spaceship ship : SpaceshipManager.ACTIVE_SHIPS.values()) {

            // Performance-Boost: Keine Schildblase berechnet? Direkt überspringen!
            if (ship.getShieldBubble() == null || ship.getShieldBubble().isEmpty()) continue;

            // 2. Wir prüfen alle Blöcke, die die Explosion zerstören will
            for (BlockPos affectedBlock : event.getAffectedBlocks()) {

                // Wenn der Block schon gerettet wurde (z.B. von einem überlappenden Schiff), überspringen
                if (protectedBlocks.contains(affectedBlock)) continue;

                // 3. DIE MAGIE: Ein einziger O(1) Check gegen unsere morphologische Blase!
                if (ship.getShieldBubble().contains(affectedBlock)) {

                    // 4. Zieht das Schiff erfolgreich Energie aus dem Reaktor?
                    if (SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, ENERGY_COST_PER_BLOCK)) {
                        // Block ist gerettet!
                        protectedBlocks.add(affectedBlock);
                    }
                }
            }
        }

        // 5. Am Ende streichen wir alle geretteten Blöcke aus der Zerstörungs-Liste der Explosion!
        if (!protectedBlocks.isEmpty()) {
            event.getAffectedBlocks().removeAll(protectedBlocks);

            // HIER KOMMT SPÄTER DER FUNKSPRUCH AN DEN SHADER HIN!
            // z.B. NetworkManager.sendToClients(new ShieldImpactPacket(event.getExplosion().center()));
        }
    }
}