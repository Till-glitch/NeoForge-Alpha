package com.peaceman.alpha.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShipSavedData extends SavedData {

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag shipList = new ListTag();

        for (Spaceship ship : SpaceshipManager.ACTIVE_SHIPS.values()) {
            CompoundTag shipTag = new CompoundTag();

            // 1. UUID abspeichern
            shipTag.putUUID("ID", ship.getId());

            // 2. Kontrollblock abspeichern
            BlockPos ctrl = ship.getControllerPos();
            shipTag.putIntArray("Controller", new int[]{ctrl.getX(), ctrl.getY(), ctrl.getZ()});

            // 3. Schiffsblöcke abspeichern
            ListTag blockList = new ListTag();
            for (BlockPos block : ship.getBlocks()) {
                blockList.add(new IntArrayTag(new int[]{block.getX(), block.getY(), block.getZ()}));
            }
            shipTag.put("Blocks", blockList);

            // 4. Homes abspeichern
            CompoundTag homesTag = new CompoundTag();
            for (Map.Entry<String, BlockPos> home : ship.getHomes().entrySet()) {
                BlockPos hp = home.getValue();
                homesTag.putIntArray(home.getKey(), new int[]{hp.getX(), hp.getY(), hp.getZ()});
            }
            shipTag.put("Homes", homesTag);

            shipList.add(shipTag);
        }

        tag.put("ActiveShips", shipList);
        return tag;
    }

    public static ShipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipSavedData data = new ShipSavedData();
        SpaceshipManager.ACTIVE_SHIPS.clear();

        ListTag shipList = tag.getList("ActiveShips", Tag.TAG_COMPOUND);
        for (int i = 0; i < shipList.size(); i++) {
            CompoundTag shipTag = shipList.getCompound(i);

            UUID id = shipTag.getUUID("ID");

            int[] ctrlArray = shipTag.getIntArray("Controller");
            BlockPos ctrlPos = new BlockPos(ctrlArray[0], ctrlArray[1], ctrlArray[2]);

            Set<BlockPos> blocks = new HashSet<>();
            ListTag blockList = shipTag.getList("Blocks", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < blockList.size(); j++) {
                int[] blockArray = blockList.getIntArray(j);
                blocks.add(new BlockPos(blockArray[0], blockArray[1], blockArray[2]));
            }

            Map<String, BlockPos> homes = new HashMap<>();
            CompoundTag homesTag = shipTag.getCompound("Homes");
            for (String key : homesTag.getAllKeys()) {
                int[] hpArray = homesTag.getIntArray(key);
                homes.put(key, new BlockPos(hpArray[0], hpArray[1], hpArray[2]));
            }

            Spaceship loadedShip = new Spaceship(id, ctrlPos, blocks, homes);
            SpaceshipManager.ACTIVE_SHIPS.put(id, loadedShip);
        }
        return data;
    }

    public static ShipSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        SavedData.Factory<ShipSavedData> factory = new SavedData.Factory<>(
                ShipSavedData::new,
                ShipSavedData::load,
                null
        );
        return overworld.getDataStorage().computeIfAbsent(factory, "spaceship_data");
    }
}