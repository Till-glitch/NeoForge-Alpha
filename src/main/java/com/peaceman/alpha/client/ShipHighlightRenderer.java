package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

// Durch diese Annotation registriert NeoForge dieses Event automatisch (nur auf dem Client!)
@EventBusSubscriber(modid = Alpha.MODID, value = Dist.CLIENT)
public class ShipHighlightRenderer {

    // Speichert, ob der Knopf gerade an oder aus ist
    public static boolean isHighlightActive = false;

    // Hier speichern wir die gefundenen Blöcke für die Anzeige
    public static Set<BlockPos> shipBlocks = new HashSet<>();

    // Dieses Event wird vom Client 20 mal pro Sekunde aufgerufen
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isHighlightActive && !shipBlocks.isEmpty()) {
            var level = Minecraft.getInstance().level;

            if (level != null) {
                // Damit es nicht extrem laggt, spawnen wir die Partikel nur ca. 2x pro Sekunde
                // (getGameTime() zählt in Ticks, 10 Ticks = halbe Sekunde)
                if (level.getGameTime() % 10 == 0) {

                    for (BlockPos pos : shipBlocks) {
                        // Spawnt einen leuchtenden Funken in der exakten Mitte jedes Blocks
                        level.addParticle(
                                ParticleTypes.END_ROD, // Sieht aus wie kleine weiße/leuchtende Funken
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                0, 0, 0 // Keine Bewegung (sie schweben auf der Stelle)
                        );
                    }
                }
            }
        }
    }
}