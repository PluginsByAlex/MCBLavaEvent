# 🌋 MCBLavaEvent

A comprehensive lava rising event plugin for Minecraft 1.21.4+ servers. Create intense survival challenges where players must stay above the rising lava while the world border closes in!

[![Build Status](https://github.com/your-username/MCBLavaEvent/actions/workflows/build.yml/badge.svg)](https://github.com/your-username/MCBLavaEvent/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## ✨ Features

### 🎮 Core Gameplay
- **Rising Lava**: Lava slowly rises from the bottom of the world, forcing players to move to higher ground
- **Shrinking World Border**: The world border gradually closes in, creating additional pressure
- **Last Player Standing**: The final survivor wins the event
- **Grace Period**: Configurable PvP-free period at the start of each event

### ⚙️ Customization
- **Configurable Settings**: Adjust lava rise speed, world border shrink rate, and timing
- **Random Events**: TNT rain, hostile mob spawns, and lightning storms
- **World Setup**: Easy area configuration with commands
- **Spectator Mode**: Eliminated players become spectators with flight enabled

### 💬 User Experience
- **MiniMessage Support**: Rich text formatting with hex colors and gradients
- **Title/Subtitle Messages**: Dramatic countdown and event notifications
- **PlaceholderAPI Integration**: Use placeholders in other plugins
- **Multilingual Ready**: Fully configurable messages

## 📦 Installation

### Automatic Installation (Recommended)
1. Download the latest JAR file from the [Releases](https://github.com/your-username/MCBLavaEvent/releases) page
2. Place it in your server's `plugins` folder
3. Restart your server
4. The plugin will automatically generate configuration files

### Manual Building
```bash
git clone https://github.com/your-username/MCBLavaEvent.git
cd MCBLavaEvent
mvn clean package
```

## 🚀 Quick Start

1. **Set up your event area:**
   ```
   /lavaevent setup 0 0 500
   ```
   This creates a 1000x1000 block area centered at coordinates (0, 0)

2. **Start an event:**
   ```
   /lavaevent start
   ```

3. **Stop an event:**
   ```
   /lavaevent stop
   ```

## 🎛️ Configuration

### Main Config (`config.yml`)
```yaml
game:
  world: "world"                    # Event world name
  start-delay: 30                   # Countdown before event starts (seconds)
  lava-rise-speed: 0.5             # Blocks per second
  max-lava-height: 200             # Maximum lava level
  border-shrink-speed: 0.1         # Border shrink rate
  grace-period: 60                 # PvP-free period (seconds)
  starting-lava-level: -64         # Starting Y coordinate
  
  border:
    starting-size: 1000            # Initial border size
    final-size: 50                 # Final border size
    center-x: 0                    # Border center X
    center-z: 0                    # Border center Z
  
  random-events:
    enabled: true
    chance: 0.3                    # 30% chance per interval
    interval: 120                  # Check every 2 minutes
```

### Messages (`messages.yml`)
All messages support MiniMessage formatting with hex colors:
```yaml
prefix: "<gradient:#FF6B35:#F7931E><bold>LAVA EVENT</bold></gradient> <dark_gray>»</dark_gray> "

event:
  starting: "<green>The lava event is starting in <yellow>{time}</yellow> seconds!"
  started: "<#FF4500><bold>🌋 THE LAVA IS RISING! 🌋</bold></#FF4500>"
  won: "<gold><bold>🏆 {player} is the last player standing! 🏆</bold></gold>"
```

## 🔧 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/lavaevent start` | `lavaevent.admin` | Start a lava event |
| `/lavaevent stop` | `lavaevent.admin` | Stop the current event |
| `/lavaevent reload` | `lavaevent.admin` | Reload configuration files |
| `/lavaevent setup <x> <z> <radius>` | `lavaevent.admin` | Set up event area |

## 🏷️ Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `lavaevent.admin` | `op` | Access to all commands |
| `lavaevent.bypass` | `op` | Bypass lava damage during events |

## 🔌 PlaceholderAPI Integration

Use these placeholders in other plugins:

| Placeholder | Description |
|-------------|-------------|
| `%lavaevent_active%` | Event active status (true/false) |
| `%lavaevent_grace_period%` | Grace period active (true/false) |
| `%lavaevent_lava_level%` | Current lava level |
| `%lavaevent_alive_count%` | Number of players alive |
| `%lavaevent_is_alive%` | If player is alive (true/false) |
| `%lavaevent_is_spectator%` | If player is spectator (true/false) |
| `%lavaevent_world%` | Event world name |
| `%lavaevent_status%` | Event status (inactive/grace_period/active) |

## 🎯 Random Events

The plugin includes three types of random events:

### 💣 TNT Rain
- TNT blocks fall from the sky at random locations
- Configurable duration and intensity
- Players must find shelter to survive

### 👹 Hostile Mob Spawns
- Spawns various hostile mobs around the play area
- Mobs target the nearest players
- Adds combat challenge beyond just avoiding lava

### ⚡ Lightning Storm
- Lightning strikes random locations
- Creates fire and can damage players
- Dramatic visual and audio effects

## 🔄 Automatic Builds

This repository uses GitHub Actions to automatically:
- Build the plugin on every push
- Create releases with detailed changelogs
- Upload JAR files for easy download

Every push to the main branch creates a new release automatically!

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Requirements

- **Minecraft**: 1.21.4+
- **Server Software**: Paper, Spigot, or compatible
- **Java**: 21+
- **Dependencies**: None (PlaceholderAPI is optional)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/your-username/MCBLavaEvent/issues) page to report bugs or request features.

## 📞 Support

- **Discord**: Join our community server (link coming soon)
- **Issues**: Use GitHub Issues for bug reports
- **Wiki**: Check the wiki for detailed guides

---

*Made with ❤️ for the Minecraft community* 