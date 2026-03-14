package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.ship.Spaceship;
import com.peaceman.alpha.ship.SpaceshipManager;
import com.peaceman.alpha.ship.SpaceshipMover;
import com.peaceman.alpha.ship.SpaceshipNavigationManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

// NEU: Wir haben Optional<UUID> am Anfang hinzugefügt!
public record ShipCommandPayload(Optional<UUID> shipId, BlockPos pos, String command, int value, String homeName) implements CustomPacketPayload {

    public static final Type<ShipCommandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_command"));

    public static final StreamCodec<ByteBuf, ShipCommandPayload> STREAM_CODEC = StreamCodec.composite(
            // LÖSUNG 2: Nutze UUIDUtil.STREAM_CODEC
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), ShipCommandPayload::shipId,
            BlockPos.STREAM_CODEC, ShipCommandPayload::pos,
            ByteBufCodecs.STRING_UTF8, ShipCommandPayload::command,
            ByteBufCodecs.INT, ShipCommandPayload::value,
            ByteBufCodecs.STRING_UTF8, ShipCommandPayload::homeName,
            ShipCommandPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final ShipCommandPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;
            var level = player.level();
            var pos = data.pos();

            // 1. SONDERFALL: SCHIFF ERSTELLEN
            if (data.command().equals("CREATE")) {
                if (level.getBlockEntity(pos) instanceof SpaceshipControlBlockEntity blockEntity) {

                    // Schiff erstellen und UUID zurückbekommen (du musst evtl. deine createShip Methode
                    // anpassen, damit sie die erstelle UUID oder das Spaceship-Objekt zurückgibt)
                    Spaceship newShip = SpaceshipManager.createShip(level, pos);

                    if (newShip != null) {
                        // UUID IN DER BLOCK ENTITY SPEICHERN (Das löst den Sync zum Client aus!)
                        blockEntity.setShipId(newShip.getId());
                    }
                }
                return; // WICHTIG: Danach abbrechen!
            }

            // 2. FÜR ALLE ANDEREN BEFEHLE: UUID NUTZEN
            if (data.shipId().isEmpty()) {
                System.out.println("UUID fehlt!");
                return; // Abbruch, wenn das Paket keine UUID mitgebracht hat
            }

            // Wir holen uns das Schiff direkt aus der Liste (ohne Blockabfrage in der Welt!)
            Spaceship ship = SpaceshipManager.getShip(data.shipId().get());

            if (ship == null) {
                return; // Schiff existiert auf dem Server nicht mehr
            }

            int dist = data.value();
            String homeName = data.homeName();

            // 3. AKTION AUSFÜHREN
            switch (data.command()) {
                case "SAVE_HOME":
                    SpaceshipNavigationManager.saveHome(level, ship, homeName);
                    break;
                case "TP_HOME":
                    SpaceshipNavigationManager.teleportToHome(level, ship, homeName, player);
                    break;
                case "UPDATE_BLOCKS":
                    SpaceshipManager.updateShipBlocks(level, ship);
                    System.out.println("Updating blocks");
                    break;
                case "DELETE_SHIP":
                    SpaceshipManager.deleteShip(level, ship);
                    break;

                // --- BEWEGUNG ---
                case "MOVE_UP":
                    SpaceshipMover.moveShip(level, ship, 0, dist, 0, player);
                    break;
                case "MOVE_DOWN":
                    SpaceshipMover.moveShip(level, ship, 0, -dist, 0, player);
                    break;

                // Relative Bewegungen gruppiert
                case "MOVE_FORWARD":
                case "MOVE_BACKWARD":
                case "MOVE_RIGHT":
                case "MOVE_LEFT":
                    Direction forward = player.getDirection();
                    Direction right = forward.getClockWise();
                    int dx = 0, dz = 0;

                    switch (data.command()) {
                        case "MOVE_FORWARD" -> { dx = forward.getStepX() * dist; dz = forward.getStepZ() * dist; }
                        case "MOVE_BACKWARD" -> { dx = -forward.getStepX() * dist; dz = -forward.getStepZ() * dist; }
                        case "MOVE_RIGHT" -> { dx = right.getStepX() * dist; dz = right.getStepZ() * dist; }
                        case "MOVE_LEFT" -> { dx = -right.getStepX() * dist; dz = -right.getStepZ() * dist; }
                    }
                    SpaceshipMover.moveShip(level, ship, dx, 0, dz, player);
                    break;
            }
        });
    }
}