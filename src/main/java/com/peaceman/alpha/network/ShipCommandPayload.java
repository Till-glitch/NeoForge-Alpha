package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlockEntity;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.SpaceshipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record ShipCommandPayload(BlockPos pos, String command, int value) implements CustomPacketPayload {

    public static final Type<ShipCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_command"));

    public static final StreamCodec<FriendlyByteBuf, ShipCommandPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShipCommandPayload::pos,
            ByteBufCodecs.STRING_UTF8, ShipCommandPayload::command, // Überträgt unseren Befehl (z.B. "SCAN" oder "MOVE_UP")
            ByteBufCodecs.INT, ShipCommandPayload::value,
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

            // Wir holen uns den Rucksack!
            if (level.getBlockEntity(pos) instanceof SpaceshipControlBlockEntity be) {

                // UUID auslesen (ist null, wenn noch nicht gescannt)
                UUID shipId = be.getShipId();

                if (data.command().equals("SCAN")) {
                    com.peaceman.alpha.ship.SpaceshipManager.createShipInstance(level, pos);
                }
                else if (shipId != null) { // Wenn wir fliegen wollen, MUSS die UUID existieren!

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

                    // Wir geben dem Manager jetzt die UUID anstatt der Position!
                    com.peaceman.alpha.ship.SpaceshipManager.moveShipInstance(level, shipId, dx, dy, dz);
                }
            }
        });
    }}