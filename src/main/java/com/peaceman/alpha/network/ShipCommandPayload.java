package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import net.minecraft.core.BlockPos;
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
            var level = context.player().level();
            var pos = data.pos();

            if (level.getBlockState(pos).is(Alpha.SPACESHIP_CONTROL.get())) {
                // Hier leiten wir die Befehle an den Block weiter!
                if (data.command().equals("SCAN")) {
                    SpaceshipControlBlock.createShipInstance(level, pos);
                } else if (data.command().equals("MOVE_UP")) {
                    SpaceshipControlBlock.moveShipInstance(level, pos, data.value());
                } else if (data.command().equals("MOVE_DOWN")){
                    SpaceshipControlBlock.moveShipInstance(level, pos, -data.value());
                }
            }
        });
    }
}