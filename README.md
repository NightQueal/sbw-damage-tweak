Forge 1.20.1 compatibility for Superb Warfare, TACZ and Create Big Cannons. 

## Features
- Attachment-aware FMJ/HP/incendiary/HE/slug vehicle penetration and server-synchronized gun/attachment tooltips. Hold Shift on a gun for detail.
- On-demand /sbwcompat armor scan report for unmapped vehicles, TACZ guns/ammo and projectile IDs.
- Optional per-vehicle armor, ammunition penetration, glancing-hit rejection and native engine-hit routing. See [ARMOR.md](docs/ARMOR.md).
- Correct TACZ explosions to use configured blast damage through SBW's distance/exposure formula.
- Supply projectile/shooter attribution for CBC vehicle hits and supported shell explosions.
- Configurable prevention of repeated CBC direct impacts on the same vehicle.
- Separate global, gun/projectile and vehicle input multipliers. Defaults are **1.0**, with no replacement armor tiers or damage caps.
