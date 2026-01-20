# HiWire - Tp2World v0.1.0

A teleport-to-world command mod for Hytale single- and multiplayer by HiWire Studio

![Project Logo Banner](docs/images/project-logo-banner.png)

## Features

- **Cross-World Teleportation** - Teleport yourself or other players to any world
- **Custom Position** - Specify exact coordinates with support for relative positions (~)
- **Custom Rotation** - Set head and body rotation separately for precise player orientation
- **World Autocomplete** - Tab completion for world names
- **Configurable Notifications** - Toggle whether teleported players receive notification messages
- **Multilingual** - Supports English (en-US), German (de-DE) and more (if added)
- **Customizable** - Override translations and assets

## Why this mod?

The default `/tp` command in Hytale has limitations when teleporting across worlds:

- **Cannot teleport other players to a different world** - The default command only allows teleporting yourself to another world, not other players
- **No rotation control for cross-world teleports** - The default command doesn't support setting head or body rotation when teleporting to another world

This mod fills those gaps by providing a dedicated command for cross-world teleportation with full control over position and rotation for any player.

## Requirements

- Hytale or Hytale Server
- Java 25

## Installation

### Using CurseForge App

The easiest way to install mods is via the [CurseForge App](https://www.curseforge.com/download/app), which handles installation and updates automatically.

### Manual Installation

1. Download the mod JAR file
2. Place it in the mods directory:
   - **Windows:** `%appdata%\Hytale\UserData\Mods`
   - **Mac:** `~/Library/Application Support/Hytale/UserData/Mods`
   - **Linux (Flatpak):** `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods`
   - **Dedicated Server:** `/mods` folder in your server directory
3. Restart the game or server

Since Hytale uses a server internally for both singleplayer and multiplayer, this mod works in both modes.

## Commands

| Command | Description | Executor | Permission |
|---------|-------------|----------|------------|
| `/tp2world <world>` | Teleport to a world's spawn point | Player only | `hiwire.tp2world.command.tp2world` |
| `/tp2world <world> --player <name>` | Teleport another player to a world | Any | `hiwire.tp2world.command.tp2world` |
| `/tp2world <world> --position <x y z>` | Teleport to specific coordinates | Player only | `hiwire.tp2world.command.tp2world` |
| `/tp2world <world> --rotation <pitch yaw roll>` | Teleport with custom head rotation | Player only | `hiwire.tp2world.command.tp2world` |
| `/tp2world <world> --bodyRotation <pitch yaw roll>` | Teleport with custom body rotation | Player only | `hiwire.tp2world.command.tp2world` |

### Command Arguments

| Argument | Type | Description |
|----------|------|-------------|
| `world` | Required | The name of the world to teleport to (supports tab completion) |
| `--player` | Optional | The player to teleport (default: yourself) |
| `--position` | Optional | Target position (x y z), supports relative coordinates with ~ |
| `--rotation` | Optional | Target head rotation (pitch yaw roll) in radians |
| `--bodyRotation` | Optional | Target body rotation (pitch yaw roll) in radians |

### Default Behavior

- If `--player` is not specified, teleports the command sender
- If `--position` is not specified, uses the world's spawn point
- If `--rotation` is not specified, uses the spawn point's rotation (or 0 0 0 if custom position is provided)
- If `--bodyRotation` is not specified, preserves previous pitch/roll and uses head yaw

### Examples

Teleport yourself to the "lobby" world:
```
/tp2world lobby
```

Teleport player "Steve" to specific coordinates in the "survival" world:
```
/tp2world survival --player Steve --position 100 64 -200
```

Teleport with relative position (10 blocks up from spawn):
```
/tp2world arena --position ~ ~10 ~
```

Teleport with custom rotation (facing south, π radians ≈ 3.14159):
```
/tp2world spawn --rotation 0 3.14159 0
```

Teleport another player with custom head and body rotation (facing east, π/2 radians ≈ 1.5708):
```
/tp2world arena --player Steve --rotation 0 1.5708 0 --bodyRotation 0 1.5708 0
```

## Permissions

| Permission | Description |
|------------|-------------|
| `hiwire.tp2world.command.tp2world` | Use the /tp2world command |

## Configuration

The configuration file is located at `mods/HiWire_Tp2World/config.json`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `NotifyTeleportedPlayer` | boolean | `true` | Whether to send a notification message to the teleported player |

**Note:** When teleporting another player, the command sender always receives a confirmation message. The `NotifyTeleportedPlayer` option only controls whether the teleported player also receives a notification.

## Customization

The mod supports user overrides for translations. Place your customizations in the mod's data folder under `/overrides`.

### Translation Files

Translation files are automatically created and updated at:
```
mods/HiWire_Tp2World/overrides/Server/Languages/{language}/
```

Edit these files to customize messages without modifying the original mod files.

## Building from Source

```bash
./gradlew build
```

The compiled mod JAR will be in `mod/build/libs/`.

## License

MIT License

Copyright (c) 2026 HiWire Studio

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Support

- **Author:** HiWire-Nick
