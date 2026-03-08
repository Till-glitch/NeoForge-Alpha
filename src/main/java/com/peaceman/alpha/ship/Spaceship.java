package com.peaceman.alpha.ship;

import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Spaceship {
    private final UUID id;
    private BlockPos controllerPos;
    private Set<BlockPos> blocks;
    private final Map<String, BlockPos> homes;

    // Konstruktor für ein brandneues Schiff
    public Spaceship(BlockPos controllerPos, Set<BlockPos> blocks) {
        this.id = UUID.randomUUID(); // Generiert eine einzigartige ID
        this.controllerPos = controllerPos;
        this.blocks = blocks;
        this.homes = new HashMap<>();
    }

    // Konstruktor für das Laden aus dem Savegame
    public Spaceship(UUID id, BlockPos controllerPos, Set<BlockPos> blocks, Map<String, BlockPos> homes) {
        this.id = id;
        this.controllerPos = controllerPos;
        this.blocks = blocks;
        this.homes = homes;
    }

    // --- GETTER & SETTER ---

    public UUID getId() { return id; }

    public BlockPos getControllerPos() { return controllerPos; }
    public void setControllerPos(BlockPos controllerPos) { this.controllerPos = controllerPos; }

    public Set<BlockPos> getBlocks() { return blocks; }
    public void setBlocks(Set<BlockPos> blocks) { this.blocks = blocks; }

    // --- HOME-SYSTEM (Schon mal vorbereitet für später) ---

    public void addHome(String name, BlockPos pos) {
        this.homes.put(name, pos);
    }

    public Map<String, BlockPos> getHomes() { return homes; }
}