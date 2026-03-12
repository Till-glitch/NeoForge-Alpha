package com.peaceman.alpha.block;

import com.peaceman.alpha.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.UUID;

public class SpaceshipReactorBlockEntity extends BlockEntity implements ISpaceshipNode {

    private UUID shipId;
    
    // Kapazität: 1.000.000 FE, maxReceive: 10.000 FE, maxExtract: 10.000 FE
    private final EnergyStorage energyStorage = new EnergyStorage(1000000, 10000, 10000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return extracted;
        }
    };

    public SpaceshipReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACESHIP_REACTOR_BE.get(), pos, state);
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

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.shipId != null) {
            tag.putUUID("ShipId", this.shipId);
        }
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("ShipId")) {
            this.shipId = tag.getUUID("ShipId");
        }
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    // --- CLIENT SYNC ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
        return tag;
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
