package com.peaceman.alpha.block;

import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class SpaceshipControlBlockEntity extends BlockEntity implements ISpaceshipNode {

    // Hier speichern wir die einzigartige ID unseres Schiffes!
    private UUID shipId;

    public SpaceshipControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_CONTROL_BE.get(), pos, state);
    }

    // --- GETTER & SETTER ---
    @Override
    public UUID getShipId() {
        return shipId;
    }

    @Override
    public void setShipId(UUID shipId) {
        this.shipId = shipId;
        setChanged(); // Sagt Minecraft: "Ich habe neue Daten, bitte beim Beenden speichern!"
    }

    // --- NBT SPEICHERN (In die Welt-Datei schreiben) ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.shipId != null) {
            tag.putUUID("ShipId", this.shipId);
        }
    }

    // --- NBT LADEN (Aus der Welt-Datei lesen) ---
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("ShipId")) {
            this.shipId = tag.getUUID("ShipId");
        }
    }
}