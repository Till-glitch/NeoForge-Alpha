package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.block.SpaceshipReactorBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.EnergyStorage;

public class SpaceshipReactorScreen extends Screen {

    // WICHTIG: Hier trägst du später DEINE eigene Textur ein!
    // Für den Moment nutzen wir das Standard-Inventar, das noch klassisch
    // funktioniert.
    private static final ResourceLocation BG_TEXTURE = ResourceLocation
            .parse("minecraft:textures/gui/container/inventory.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    private final BlockPos blockPos;

    public SpaceshipReactorScreen(BlockPos pos) {
        super(Component.literal("Raumschiff Reaktor"));
        this.blockPos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Hintergrund abdunkeln (Wie in AbstractContainerScreen)
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Position zentrieren
        int leftPos = (this.width - WIDTH) / 2;
        int topPos = (this.height - HEIGHT) / 2;

        // 3. Textur zeichnen (EXAKT wie EnderIO es macht: blit mit Start 0,0)
        // Das erwartet eine 256x256 große PNG-Datei.
        guiGraphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);

        // 4. Titel zeichnen
        guiGraphics.drawString(this.font, this.title, leftPos + 8, topPos + 6, 0x404040, false);

        // 5. Energie rendern
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity be = this.minecraft.level.getBlockEntity(blockPos);

            if (be instanceof SpaceshipReactorBlockEntity reactorEntity) {
                EnergyStorage energyStorage = reactorEntity.getEnergyStorage();
                int currentEnergy = energyStorage.getEnergyStored();
                int maxEnergy = energyStorage.getMaxEnergyStored();

                String energyText = "Energie: " + String.format("%,d", currentEnergy) + " FE";
                guiGraphics.drawCenteredString(this.font, energyText, this.width / 2, topPos + 25, 0x404040);

                // --- LADEBALKEN ---
                int barWidth = 120;
                int barHeight = 12;
                int startX = (this.width - barWidth) / 2;
                int startY = topPos + 40;

                guiGraphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xFF333333);

                if (maxEnergy > 0) {
                    float fillPercentage = (float) currentEnergy / maxEnergy;
                    int filledWidth = (int) (fillPercentage * barWidth);
                    guiGraphics.fill(startX, startY, startX + filledWidth, startY + barHeight, 0xFF00FF00);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}