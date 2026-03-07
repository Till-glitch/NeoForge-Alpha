package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

            if (level.getBlockState(pos).is(Alpha.SPACESHIP_CONTROL.get())) {

                if (data.command().equals("SCAN")) {
                    SpaceshipControlBlock.createShipInstance(level, pos);
                }
                else if (data.command().equals("MOVE_UP")) {
                    SpaceshipControlBlock.moveShipInstance(level, pos, 0, dist, 0);
                }
                else if (data.command().equals("MOVE_DOWN")) {
                    SpaceshipControlBlock.moveShipInstance(level, pos, 0, -dist, 0);
                }
                else {
                    // Die Richtung, in die der Spieler schaut (Norden, Süden, Osten, Westen)
                    Direction forward = player.getDirection();
                    // Die Richtung, die rechts vom Spieler liegt (Uhrzeigersinn)
                    Direction right = forward.getClockWise();

                    int dx = 0, dy = 0, dz = 0;

                    // Wir multiplizieren die Blickrichtung (gibt 1, 0 oder -1 zurück) mit der Distanz
                    switch (data.command()) {
                        case "MOVE_FORWARD" -> { dx = forward.getStepX() * dist; dz = forward.getStepZ() * dist; }
                        case "MOVE_BACKWARD" -> { dx = -forward.getStepX() * dist; dz = -forward.getStepZ() * dist; }
                        case "MOVE_RIGHT" -> { dx = right.getStepX() * dist; dz = right.getStepZ() * dist; }
                        case "MOVE_LEFT" -> { dx = -right.getStepX() * dist; dz = -right.getStepZ() * dist; }
                    }

                    // Die universelle Methode mit unseren berechneten Werten aufrufen!
                    SpaceshipControlBlock.moveShipInstance(level, pos, dx, dy, dz);
                }
            }
        });
    }
}