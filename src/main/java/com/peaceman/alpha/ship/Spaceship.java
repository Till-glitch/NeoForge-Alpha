package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class Spaceship {
    private final UUID id;
    private BlockPos controllerPos;
    private Set<BlockPos> blocks;
    private final Map<String, BlockPos> homes;
    private final List<BlockPos> reactors = new ArrayList<>();
    private final List<BlockPos> shields = new ArrayList<>();

    // Konstruktor für ein brandneues Schiff
    public Spaceship(BlockPos controllerPos, Set<BlockPos> blocks) {
        this.id = UUID.randomUUID(); // Generiert eine einzigartige ID
        this.controllerPos = controllerPos;
        this.blocks = blocks;
        this.homes = new HashMap<>();
    }

    // Konstruktor für das Laden aus dem Savegame (Jetzt mit Reaktoren und Schilden!)
    public Spaceship(UUID id, BlockPos controllerPos, Set<BlockPos> blocks, Map<String, BlockPos> homes, List<BlockPos> loadedReactors, List<BlockPos> loadedShields) {
        this.id = id;
        this.controllerPos = controllerPos;
        this.blocks = blocks;
        this.homes = homes;

        // Listen befüllen
        this.reactors.addAll(loadedReactors);
        this.shields.addAll(loadedShields);
    }

    // --- GETTER & SETTER ---

    public UUID getId() { return id; }
    public List<BlockPos> getReactors() { return reactors; }
    public List<BlockPos> getShields() { return shields;}
    public BlockPos getControllerPos() { return controllerPos; }
    public void setControllerPos(BlockPos controllerPos) { this.controllerPos = controllerPos; }

    public Set<BlockPos> getBlocks() { return blocks; }

    public void addBlock(BlockPos pos, boolean isReactor, boolean isShield) {
        this.blocks.add(pos);
        if (isReactor) this.reactors.add(pos);
        if (isShield) this.shields.add(pos);
    }

    public void setBlocks(Set<BlockPos> blocks, Level level) {
        this.blocks = blocks;

        // GANZ WICHTIG: Die alten Listen leeren, bevor das Schiff an
        // der neuen Position neu gescannt wird!
        this.reactors.clear();
        this.shields.clear();

        for (BlockPos pos : blocks){
            // Wir nutzen jetzt das 'level', das wir als Parameter übergeben haben
            BlockEntity be = level.getBlockEntity(pos);

            // Reaktoren einsortieren
            if (be instanceof SpaceshipReactorBlockEntity) {
                this.reactors.add(pos);
            }
            // Schilde einsortieren
            else if (be instanceof SpaceshipShieldBlockEntity) {
                this.shields.add(pos);
            }
        }
    }
    // --- HOME-SYSTEM (Schon mal vorbereitet für später) ---

    public void addHome(String name, BlockPos pos) {
        this.homes.put(name, pos);
    }

    public Map<String, BlockPos> getHomes() { return homes; }
}