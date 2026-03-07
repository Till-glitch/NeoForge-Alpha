package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = Alpha.MODID, value = Dist.CLIENT)
public class ShipHighlightRenderer {

    public static boolean isHighlightActive = false;
    public static Set<BlockPos> shipBlocks = new HashSet<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isHighlightActive && !shipBlocks.isEmpty()) {
            var level = Minecraft.getInstance().level;

            if (level != null) {
                // Ein bisschen schneller (alle 5 Ticks), damit die Hülle gut sichtbar ist
                if (level.getGameTime() % 10 == 0) {

                    for (BlockPos pos : shipBlocks) {
                        // Prüfe alle 6 Seiten des Blocks (Oben, Unten, Nord, Süd, Ost, West)
                        for (Direction dir : Direction.values()) {

                            // Wenn der Block auf DIESER Seite NICHT zum Schiff gehört (z.B. Luft ist)...
                            if (!shipBlocks.contains(pos.relative(dir))) {

                                // ...dann sind wir an der Außenhülle!
                                // Wir schieben den Partikel um 0.55 in die jeweilige Richtung nach außen
                                double x = pos.getX() + 0.5 + (dir.getStepX() * 0.55);
                                double y = pos.getY() + 0.5 + (dir.getStepY() * 0.55);
                                double z = pos.getZ() + 0.5 + (dir.getStepZ() * 0.55);

                                // Ein bisschen Zufall (-0.5 bis +0.5), damit sich die Partikel
                                // schön über die gesamte Block-Oberfläche verteilen
                                if (dir.getAxis() != Direction.Axis.X) x += (level.random.nextDouble() - 0.5);
                                if (dir.getAxis() != Direction.Axis.Y) y += (level.random.nextDouble() - 0.5);
                                if (dir.getAxis() != Direction.Axis.Z) z += (level.random.nextDouble() - 0.5);

                                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }
    }
}