# SBW Damage Tweak

A Forge 1.20.1 mod adding configurable combat compatibility between **Superb Warfare**, **Timeless and Classics Zero (TACZ)**, and **Create Big Cannons**.

## Features

- Vehicle armor profiles, gun-specific penetration, ammunition attachment effects, and angle-based ricochets.
- Separate engine, hull, wheel, and track damage, with disabled tracks preventing powered tank movement.
- TACZ explosion compatibility and CBC projectile attribution.
- Independent global and per-weapon damage multipliers for direct hits and explosions against vehicles, players, and mobs.
- Impact sounds, TACZ penetration statistics, and optional Jade vehicle inspection.

Unmapped armor hits retain native behavior. Armor profiles are gameplay estimates rather than physical simulations.

## Installation

Install the same version on the server and clients. Requires Minecraft 1.20.1, Forge 47, Superb Warfare 0.8.9.1, TACZ 1.1.8, and Create Big Cannons 5.11.3, along with their dependencies. Jade 11.13.2 is optional for the inspection overlay.

[Download 1.8.0](https://github.com/NightQueal/sbw-damage-tweak/raw/main/dist/sbw-dmg-tweak-1.8.0.jar). Remove the previous version before installing.

## Configuration

Forge generates `config/civilized-sbw-common.toml` and the mod generates `config/civilized-sbw-armor.json`. Multiplayer combat uses the server settings. Restart after editing and reconnect clients to refresh inspection data.

New damage multipliers default to **1.0**. Advanced armor is opt-in through `armor.advancedArmor`.

- [Damage tuning and examples](docs/DAMAGE-TUNING.md)
- [Armor and component damage](docs/ARMOR.md)
- [Compatibility details](docs/COMPATIBILITY.md)
- [Building and testing](docs/DEVELOPMENT.md)
- [Changelog](CHANGELOG.md)

## Issues and license

Report issues with mod versions, reproduction steps, and relevant logs through [GitHub Issues](https://github.com/NightQueal/sbw-damage-tweak/issues).

[MIT licensed](LICENSE). Unofficial compatibility mod; not affiliated with the supported mod teams.
