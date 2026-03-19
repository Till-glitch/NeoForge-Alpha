package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.ship.SpaceshipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

// Sagt dem System: Suche im Ordner 'peaceman_alpha' nach den Dateien
@GameTestHolder(Alpha.MODID)
public class SpaceshipGameTests {

    // Da die Klasse "SpaceshipGameTests" heißt, sucht das System jetzt nach
    // der Datei "spaceshipgametests.empty.snbt" - und genau die hast du ja erstellt!
    @GameTest()
    public static void testShipCreation(GameTestHelper helper) {

        BlockPos relativePos = new BlockPos(1, 2, 1);

        // 1. Block platzieren (Achte auf deinen korrekten ModBlocks-Import)
        helper.setBlock(relativePos, com.peaceman.alpha.registry.ModBlocks.SPACESHIP_CONTROL.get());

        // 2. Aktion ausführen
        BlockPos absolutePos = helper.absolutePos(relativePos);
        SpaceshipManager.createShip(helper.getLevel(), absolutePos);

        // 3. Überprüfung
        helper.succeedIf(() -> {
            if (helper.getLevel().getBlockEntity(absolutePos) instanceof ISpaceshipNode node) {
                if (node.getShipId() != null) {
                    return; // Test erfolgreich bestanden!
                }
            }
            helper.fail("Kontrollblock hat nach createShip keine UUID erhalten!");
        });
    }
}