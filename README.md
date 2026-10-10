# CIVILIZED SBW Compatibility

Forge 1.20.1 compatibility for Superb Warfare, TACZ and Create Big Cannons. Version **1.5.2** adds an optional, configurable armor model. Native armor remains the default; enable advancedArmor to test the new model.

## Features
- Attachment-aware FMJ/HP/incendiary/HE/slug vehicle penetration and server-synchronized gun/attachment tooltips. Hold Shift on a gun for detail.
- On-demand /sbwcompat armor scan report for unmapped vehicles, TACZ guns/ammo and projectile IDs.
- Optional per-vehicle armor, ammunition penetration, glancing-hit rejection and native engine-hit routing. See [ARMOR.md](docs/ARMOR.md).
- Correct TACZ explosions to use configured blast damage through SBW's distance/exposure formula.
- Supply projectile/shooter attribution for CBC vehicle hits and supported shell explosions.
- Configurable prevention of repeated CBC direct impacts on the same vehicle.
- Separate global, gun/projectile and vehicle input multipliers. Defaults are **1.0**, with no replacement armor tiers or damage caps.
- Independent toggles for CIVILIZED restrictions on the SBW Netherite Hammer and DPS Generator.
