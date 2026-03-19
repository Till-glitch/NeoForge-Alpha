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
    private List<BlockPos> reactors = new ArrayList<>();
    private List<BlockPos> shields = new ArrayList<>();
    private Set<BlockPos> shieldBubble = new HashSet<>(); // NEU!

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
    public void setReactors(List<BlockPos> reactors) { this.reactors = reactors; }
    public List<BlockPos> getShields() { return shields;}
    public void setShields(List<BlockPos> shields) { this.shields = shields; }
    public BlockPos getControllerPos() { return controllerPos; }
    public void setControllerPos(BlockPos controllerPos) { this.controllerPos = controllerPos; }
    public Set<BlockPos> getBlocks() { return blocks; }
    public void setBlocksRaw(Set<BlockPos> blocks) { this.blocks = blocks; }
    public void setShieldBubble(Set<BlockPos> shieldBubble) { this.shieldBubble = shieldBubble; }
    public Set<BlockPos> getShieldBubble() { return this.shieldBubble; }

    public void addBlock(BlockPos pos, boolean isReactor, boolean isShield) {
        this.blocks.add(pos);
        if (isReactor) this.reactors.add(pos);
        if (isShield) this.shields.add(pos);
    }

    // =====================================================================
    // --- 3. DAS SCHWERE STRUKTUR-UPDATE (Beim Bauen / Block abbauen)
    // =====================================================================
    public void setBlocks(Set<BlockPos> blocks, Level level) {
        this.blocks = blocks;

        // Die alten Listen leeren
        this.reactors.clear();
        this.shields.clear();

        for (BlockPos pos : blocks){
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity) {
                this.reactors.add(pos);
            } else if (be instanceof SpaceshipShieldBlockEntity) {
                this.shields.add(pos);
            }
        }

        // NEU: Nach dem Scannen berechnen wir einmalig die Schild-Hitbox!
        // (Radius: 5 Blöcke nach außen - kannst du beliebig anpassen)
        if (!this.shields.isEmpty()) {
            this.shieldBubble = ShieldMorphology.calculateShieldBubble(this.blocks, 5);
        } else {
            // Kein Schildgenerator auf dem Schiff? Dann leeren wir die Blase.
            if (this.shieldBubble != null) {
                this.shieldBubble.clear();
            } else {
                this.shieldBubble = new HashSet<>();
            }
        }
    }
    // --- HOME-SYSTEM (Schon mal vorbereitet für später) ---

    public void addHome(String name, BlockPos pos) {
        this.homes.put(name, pos);
    }

    public Map<String, BlockPos> getHomes() { return homes; }
}