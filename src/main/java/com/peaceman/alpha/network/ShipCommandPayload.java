package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

// NEU: Wir haben "String textData" hinzugefügt!
public record ShipCommandPayload(BlockPos pos, String command, int value, String textData) implements CustomPacketPayload {

    public static final Type<ShipCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_command"));

    public static final StreamCodec<FriendlyByteBuf, ShipCommandPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShipCommandPayload::pos,
            ByteBufCodecs.STRING_UTF8, ShipCommandPayload::command,
            ByteBufCodecs.INT, ShipCommandPayload::value,
            ByteBufCodecs.STRING_UTF8, ShipCommandPayload::textData, // Überträgt unseren Text
            ShipCommandPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final ShipCommandPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var level = player.level();
            var pos = data.pos();
            int dist = data.value();
            String text = data.textData();

            if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                UUID shipId = node.getShipId();

                // 1. Initialisieren (Darf auch passieren, wenn shipId noch null ist)
                if (data.command().equals("CREATE")) {
                    if (node instanceof com.peaceman.alpha.block.SpaceshipControlBlockEntity) {
                        com.peaceman.alpha.ship.SpaceshipManager.createShip(level, pos);
                    }
                }
                // Ab hier MUSS eine UUID existieren
                else if (shipId != null) {

                    // 2. Updaten & Löschen
                    if (data.command().equals("UPDATE_BLOCKS")) {
                        com.peaceman.alpha.ship.SpaceshipManager.updateShipBlocks(level, pos, shipId);
                        return;
                    }
                    else if (data.command().equals("DELETE_SHIP")) {
                        com.peaceman.alpha.ship.SpaceshipManager.deleteShipFromBlock(level, pos, shipId);
                        return;
                    }

                    // --- HOMES ---
                    if (data.command().equals("SAVE_HOME")) {
                        com.peaceman.alpha.ship.SpaceshipManager.saveHome(level, shipId, text);
                        return;
                    }
                    else if (data.command().equals("TP_HOME")) {
                        com.peaceman.alpha.ship.SpaceshipManager.teleportToHome(level, shipId, text);
                        return;
                    }

                    // --- BEWEGUNG ---
                    Direction forward = player.getDirection();
                    Direction right = forward.getClockWise();
                    int dx = 0, dy = 0, dz = 0;

                    switch (data.command()) {
                        case "MOVE_UP" -> dy = dist;
                        case "MOVE_DOWN" -> dy = -dist;
                        case "MOVE_FORWARD" -> { dx = forward.getStepX() * dist; dz = forward.getStepZ() * dist; }
                        case "MOVE_BACKWARD" -> { dx = -forward.getStepX() * dist; dz = -forward.getStepZ() * dist; }
                        case "MOVE_RIGHT" -> { dx = right.getStepX() * dist; dz = right.getStepZ() * dist; }
                        case "MOVE_LEFT" -> { dx = -right.getStepX() * dist; dz = -right.getStepZ() * dist; }
                    }

                    com.peaceman.alpha.ship.SpaceshipManager.moveShipInstance(level, shipId, dx, dy, dz);
                }
            }
        });
    }}