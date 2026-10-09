# CurseForge submission

Name: SBW Damage Tweak
Summary: Configurable vehicle damage compatibility between Superb Warfare, TACZ and Create Big Cannons.
Game: Minecraft
Project type: Mods
Loader: Forge
Minecraft version: 1.20.1
License: MIT
File: civilized-sbw-compat-1.2.1.jar
File type: Beta (full-pack multiplayer balance has not been validated)
Environment: Client and server

## Project description

SBW Damage Tweak improves vehicle damage compatibility between Superb Warfare, Timeless and Classics Zero (TACZ), and Create Big Cannons.

- Makes TACZ explosions use their configured blast damage through Superb Warfare's native vehicle armor and blast exposure calculation.
- Corrects shooter/projectile attribution for Create Big Cannons vehicle impacts and supported shell explosions.
- Prevents repeated direct hits from one cannon projectile on the same vehicle, with a configurable toggle.
- Provides global, per-gun/projectile, and per-vehicle damage multipliers. Defaults are 1.0.

This build was made for the CIVILIZED pack and also disables obtaining/using Superb Warfare's Netherite Hammer and DPS Generator. Both restrictions can be disabled independently in the equipment configuration section.

Requires Forge 1.20.1, Superb Warfare 0.8.9.1, TACZ 1.1.8, and Create Big Cannons 5.11.3, plus their normal dependencies. Add these three mods as required dependencies in the project relations.

Optional advanced armor: enable advancedArmor in config/civilized-sbw-common.toml. Per-vehicle and ammunition values live in config/civilized-sbw-armor.json. Includes approximate plate geometry, ammunition penetration, glancing-hit rejection, and native engine routing. Values are gameplay estimates; ricochet does not visibly bounce projectiles.

Configuration: config/civilized-sbw-common.toml. One config applies to all worlds in an instance. For multiplayer, edit the server's file. Restart after changing settings.

Eleven isolated integration tests passed with and without Explosion Overhaul. Full-pack multiplayer combat balance remains untested. Native vehicle armor and friendly-fire behavior are retained; ownerless cannon projectiles cannot identify a player's team.

## 1.2.1 changelog

- Added /sbwcompat armor scan for missing mappings, loaded TACZ gun damage/speed and unused profiles. Operator-only, reporting only.
- Added optional per-vehicle armor, ammunition penetration, angle-based hit rejection and engine hitbox routing. Enable advancedArmor to opt in; defaults preserve native armor.
- Added independent disableNetheriteHammer and disableDpsGenerator toggles. Defaults remain true for CIVILIZED; set false to allow the equipment.
- Updated the accompanying KubeJS recipe and JEI scripts to respect the toggles.
- New filename: config/civilized-sbw-common.toml.
- To preserve custom settings from 1.1.0, copy the world's serverconfig/civilized-sbw-server.toml to the new path before starting 1.2.1.
- Build and all six integration tests passed.

## Submission notes

The GitHub repository is private; do not advertise it as a publicly accessible source or issue tracker. Upload only the distributable jar from dist/, not the repository or its test fixtures. CurseForge may require a project avatar and an authenticated author account. The submitted project/file must pass CurseForge moderation before being available to users.
