package com.peaceman.alpha.block;

import com.peaceman.alpha.menu.SpaceshipReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpaceshipReactorBlockEntity extends BlockEntity implements MenuProvider, ISpaceshipNode {

    // --- ISpaceshipNode Daten ---
    private UUID shipId;

    // Kapazität: 1.000.000 FE, maxReceive: 10.000 FE, maxExtract: 10.000 FE
    // Wir überschreiben die Methoden, um setChanged() aufzurufen!
    private final EnergyStorage energyStorage = new EnergyStorage(1000000, 10000, 10000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged(); // WICHTIG: Teilt Minecraft mit, dass die Energie auf die Festplatte muss!

                // Optional: Auskommentieren, wenn der Block von außen (z.B. durch Waila/The One Probe) updaten soll
                // if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged(); // WICHTIG: Sichert den neuen Stand nach Entnahme!

                // Optional: Auskommentieren, wenn der Block von außen updaten soll
                // if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
            return extracted;
        }
    };

    // Daten-Synchronisation für das Menü (Server -> Client für die GUI)
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            switch (index) {
                case 0: return energyStorage.getEnergyStored();
                case 1: return energyStorage.getMaxEnergyStored();
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            // Client-seitig setzen wir hier nichts, da Energie nur Server -> Client fließt
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public SpaceshipReactorBlockEntity(BlockPos pos, BlockState state) {
        super(com.peaceman.alpha.registry.ModBlockEntities.SPACESHIP_REACTOR_BE.get(), pos, state);
    }

    // --- ISpaceshipNode Methoden ---
    @Override
    public UUID getShipId() {
        return shipId;
    }

    @Override
    public void setShipId(UUID shipId) {
        this.shipId = shipId;
        setChanged(); // Auch hier muss gespeichert werden, wenn das Schiff sich ändert
    }

    // --- MenuProvider Methoden ---
    @Override
    public Component getDisplayName() {
        return Component.literal("Reaktor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpaceshipReactorMenu(containerId, playerInventory, this, this.data);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // --- NBT Daten Speichern & Laden ---
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // ShipId speichern
        if (this.shipId != null) {
            tag.putUUID("ShipId", this.shipId);
        }

        // Energie SICHER speichern (Offizieller NeoForge-Weg)
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // ShipId laden
        if (tag.hasUUID("ShipId")) {
            this.shipId = tag.getUUID("ShipId");
        }

        // Energie SICHER laden (Offizieller NeoForge-Weg)
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    // --- CLIENT SYNC (Für Mods, die NBT von außen lesen wollen) ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
        return tag;
    }

    @Override
    @Nullable
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}