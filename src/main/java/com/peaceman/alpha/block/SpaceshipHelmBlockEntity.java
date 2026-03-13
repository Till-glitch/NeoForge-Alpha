package com.peaceman.alpha.block;

import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class SpaceshipHelmBlockEntity extends BlockEntity implements ISpaceshipNode {

    private UUID shipId;

    public SpaceshipHelmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_HELM_BE.get(), pos, state);
    }

    @Override
    public UUID getShipId() {
        return shipId;
    }

    @Override
    public void setShipId(UUID shipId) {
        this.shipId = shipId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.shipId != null) {
            tag.putUUID("ShipId", this.shipId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("ShipId")) {
            this.shipId = tag.getUUID("ShipId");
        }
    }
}
