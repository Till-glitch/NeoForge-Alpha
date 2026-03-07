package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.SpaceshipControlBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScanShipPayload(BlockPos pos) implements CustomPacketPayload {

    // 1. Die eindeutige ID für dieses Paket
    public static final Type<ScanShipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "scan_ship"));

    // 2. Der Codec, der die Koordinaten in Nullen und Einsen übersetzt (fürs Internet) und zurück
    public static final StreamCodec<FriendlyByteBuf, ScanShipPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScanShipPayload::pos,
            ScanShipPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 3. Diese Methode wird auf dem SERVER ausgeführt, wenn das Paket ankommt!
    public static void handleData(final ScanShipPayload data, final IPayloadContext context) {
        // Wir übergeben die Arbeit an den Haupt-Server-Thread
        context.enqueueWork(() -> {
            var player = context.player();
            var level = player.level();
            var blockPos = data.pos();

            // Sicherheitscheck: Ist an dieser Koordinate wirklich unsere Steuereinheit?
            // Verhindert, dass Hacker das Paket fälschen und den Server crashen.
            if (level.getBlockState(blockPos).is(Alpha.SPACESHIP_CONTROL.get())) {
                // HIER STARTEN WIR DEN SCANNER!
                SpaceshipControlBlock.scanSpaceship(level, blockPos);
            }
        });
    }
}