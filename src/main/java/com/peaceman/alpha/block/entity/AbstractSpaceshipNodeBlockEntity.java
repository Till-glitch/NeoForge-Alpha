package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.block.ISpaceshipNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

// Die Klasse ist abstract, weil wir sie nie direkt in der Welt platzieren,
// sondern nur davon erben wollen.
public abstract class AbstractSpaceshipNodeBlockEntity extends BlockEntity implements ISpaceshipNode {

    private UUID shipId;

    // Der Konstruktor zwingt die Kind-Klassen, ihren spezifischen BlockEntityType zu übergeben
    public AbstractSpaceshipNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- GETTER & SETTER (Aus dem Interface) ---
    @Override
    public UUID getShipId() {
        return this.shipId;
    }

    @Override
    public void setShipId(UUID shipId) {
        this.shipId = shipId;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            // Synchronisiert die ID automatisch an den Client
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // --- NBT SPEICHERN & LADEN ---
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
        } else {
            this.shipId = null; // Wichtig für gelöschte Schiffe
        }
    }

    // --- NETZWERK SYNC ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}