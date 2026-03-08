package com.peaceman.alpha.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShipSavedData extends SavedData {

    // 1. SPEICHERN (Von der Liste in die Datei)
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag shipList = new ListTag();

        for (Map.Entry<BlockPos, Set<BlockPos>> entry : SpaceshipManager.ACTIVE_SHIPS.entrySet()) {
            CompoundTag shipTag = new CompoundTag();

            // Kontrollblock-Position abspeichern (als X, Y, Z Array)
            BlockPos ctrl = entry.getKey();
            shipTag.putIntArray("Controller", new int[]{ctrl.getX(), ctrl.getY(), ctrl.getZ()});

            // Alle dazugehörigen Schiffsblöcke abspeichern
            ListTag blockList = new ListTag();
            for (BlockPos block : entry.getValue()) {
                blockList.add(new IntArrayTag(new int[]{block.getX(), block.getY(), block.getZ()}));
            }
            shipTag.put("Blocks", blockList);

            shipList.add(shipTag);
        }

        tag.put("ActiveShips", shipList);
        return tag; // Gibt die fertige "Akte" an Minecraft zurück
    }

    // 2. LADEN (Von der Datei zurück in die Liste)
    public static ShipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipSavedData data = new ShipSavedData();
        SpaceshipManager.ACTIVE_SHIPS.clear(); // Alten Müll löschen, bevor wir laden

        ListTag shipList = tag.getList("ActiveShips", Tag.TAG_COMPOUND);
        for (int i = 0; i < shipList.size(); i++) {
            CompoundTag shipTag = shipList.getCompound(i);

            // Kontrollblock auslesen
            int[] ctrlArray = shipTag.getIntArray("Controller");
            BlockPos ctrlPos = new BlockPos(ctrlArray[0], ctrlArray[1], ctrlArray[2]);

            // Alle Schiffsblöcke auslesen
            Set<BlockPos> blocks = new HashSet<>();
            ListTag blockList = shipTag.getList("Blocks", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < blockList.size(); j++) {
                int[] blockArray = blockList.getIntArray(j);
                blocks.add(new BlockPos(blockArray[0], blockArray[1], blockArray[2]));
            }

            SpaceshipManager.ACTIVE_SHIPS.put(ctrlPos, blocks);
        }
        return data;
    }

    // 3. HILFSMETHODE: Holt unsere Datei aus dem Server
    public static ShipSavedData get(ServerLevel level) {
        // Wir speichern das zentral in der Overworld, damit die Daten nicht in Dimensionen verloren gehen
        ServerLevel overworld = level.getServer().overworld();

        SavedData.Factory<ShipSavedData> factory = new SavedData.Factory<>(
                ShipSavedData::new,
                ShipSavedData::load,
                null
        );

        // Sucht die Datei "spaceship_data.dat". Wenn sie nicht da ist, wird eine neue erstellt.
        return overworld.getDataStorage().computeIfAbsent(factory, "spaceship_data");
    }
}