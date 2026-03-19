package com.peaceman.alpha.ship;

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

    // Radius muss mit dem Radius deines Shaders übereinstimmen!
    public static final int SHIELD_RADIUS = 20;
    public static final int ENERGY_COST_PER_BLOCK = 5; // 50 FE pro geschütztem Block

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        // Logik (Schaden & Energie) passiert IMMER nur auf dem Server
        if (level.isClientSide()) return;

        List<BlockPos> protectedBlocks = new ArrayList<>();

        // 1. Wir gehen alle aktiven Schiffe durch
        for (Spaceship ship : SpaceshipManager.ACTIVE_SHIPS.values()) {

            // Performance-Boost: Keine Schilde? Direkt überspringen!
            if (ship.getShields().isEmpty()) continue;

            // 2. Wir prüfen jeden Schildgenerator auf diesem Schiff
            for (BlockPos shieldPos : ship.getShields()) {

                // 3. Wir schauen uns alle Blöcke an, die die Explosion zerstören will
                for (BlockPos affectedBlock : event.getAffectedBlocks()) {

                    // Wenn der Block schon von einem anderen Schild gerettet wurde, überspringen
                    if (protectedBlocks.contains(affectedBlock)) continue;

                    // 4. Liegt der Block in der Sphäre unseres Schildes?
                    if (affectedBlock.distSqr(shieldPos) <= (SHIELD_RADIUS * SHIELD_RADIUS)) {

                        // 5. Zieht das Schiff erfolgreich Energie aus dem Reaktor?
                        if (SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, ENERGY_COST_PER_BLOCK)) {
                            // Block ist gerettet!
                            protectedBlocks.add(affectedBlock);
                        }
                    }
                }
            }
        }

        // 6. Am Ende streichen wir alle geretteten Blöcke aus der Zerstörungs-Liste der Explosion!
        if (!protectedBlocks.isEmpty()) {
            event.getAffectedBlocks().removeAll(protectedBlocks);

            // HIER KOMMT SPÄTER DER FUNKSPRUCH AN DEN SHADER HIN!
            // System.out.println("Schild hat " + protectedBlocks.size() + " Blöcke gerettet!");
        }
    }
}