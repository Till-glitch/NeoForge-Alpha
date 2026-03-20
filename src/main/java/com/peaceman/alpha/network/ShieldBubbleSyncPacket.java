package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record ShieldBubbleSyncPacket(UUID shipId, Set<BlockPos> relativeBubbleBlocks) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "shield_bubble_sync");

    @Override
    public ResourceLocation id() { return ID; }

    // Wie das Paket komprimiert/dekomprimiert wird
    public static final StreamCodec<FriendlyByteBuf, ShieldBubbleSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                // 1. UUID schreiben
                buf.writeUUID(packet.shipId());

                // 2. Anzahl der Blöcke
                buf.writeInt(packet.relativeBubbleBlocks().size());

                // 3. Jede BlockPos als kompakte long-Zahl schreiben (X,Y,Z in 64-Bit)
                for (BlockPos pos : packet.relativeBubbleBlocks()) {
                    buf.writeLong(pos.asLong());
                }
            },
            buf -> {
                // 1. UUID lesen
                UUID id = buf.readUUID();

                // 2. Anzahl lesen
                int size = buf.readInt();

                // 3. Blöcke lesen
                Set<BlockPos> blocks = new HashSet<>(size);
                for (int i = 0; i < size; i++) {
                    blocks.add(BlockPos.of(buf.readLong()));
                }
                return new ShieldBubbleSyncPacket(id, blocks);
            }
    );
}