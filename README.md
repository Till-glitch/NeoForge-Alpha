# Mod Alpha (NeoForge Spaceship Mod)

An advanced spaceship mod for **Minecraft 1.21** (NeoForge) that allows you to build, fly, and protect functioning spaceships with powerful energy shields.

## Features

* **Spaceship Control Block:** The core of every ship. Allows you to create ships from blocks, update their structure, or dissolve them. Includes a visual marker function to display the ship's hull.
* **Helm (Control Panel):** Navigate your ship manually (Forward, Backward, Left, Right, Up, Down) or use the integrated waypoint system to save positions and fly to them automatically with pinpoint accuracy.
* **Ship Reactor:** The energy core of your ship. Stores up to 1,000,000 Forge Energy (FE). Energy is required for flight (depending on ship size and distance) and for the shields. *Developer Tip: Right-clicking with Redstone instantly fills the reactor with 50,000 FE!*
* **Shield Generator & Shader:** Protects your ship's blocks from explosions (e.g., Creepers, TNT). Shields consume reactor energy upon impact. Visually, the shield is represented by a stunning, procedurally generated hexagon shader with ripple and impact effects.
* **Backflip Tool (Klasingschen Degen):** A fun developer item that launches hit targets into the air and forces them to perform a backflip.

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21 (tested with version 21.0.167+).
2. Download the compiled .jar file of the mod.
3. Place the file into your Minecraft 'mods' folder.
4. Start the game!

## For Developers (Compiling)

This project uses Gradle. To build the mod yourself, clone the repository and run the following command in the root directory:

**Windows:**
```bash
gradlew.bat build
