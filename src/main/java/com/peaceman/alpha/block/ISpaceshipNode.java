package com.peaceman.alpha.block;

import com.peaceman.alpha.ship.Spaceship;
import com.peaceman.alpha.ship.SpaceshipManager;

import java.util.UUID;

public interface ISpaceshipNode {
    UUID getShipId();

    void setShipId(UUID shipId);

    // --- NEU: Die Objekt-Methode ---
    // Jeder Block, der dieses Interface nutzt, kann dir jetzt sofort das fertige Schiff geben!
    default Spaceship getShip() {
        if (getShipId() == null) {
            return null;
        }
        // Holt sich das Schiff "on the fly" aus dem Manager
        return SpaceshipManager.getShip(getShipId());
    }
}
