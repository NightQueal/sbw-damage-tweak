Forge 1.20.1 compatibility for Superb Warfare, TACZ and Create Big Cannons. 

## Features
- Attachment-aware FMJ/HP/incendiary/HE/slug vehicle penetration and server-synchronized gun/attachment tooltips. Hold Shift on a gun for detail.
- On-demand /sbwcompat armor scan report for unmapped vehicles, TACZ guns/ammo and projectile IDs.
- Optional per-vehicle armor, ammunition penetration, glancing-hit rejection and native engine-hit routing. See [ARMOR.md](docs/ARMOR.md).
- Correct TACZ explosions to use configured blast damage through SBW's distance/exposure formula.
- Supply projectile/shooter attribution for CBC vehicle hits and supported shell explosions.
- Configurable prevention of repeated CBC direct impacts on the same vehicle.
- Separate global, gun/projectile and vehicle input multipliers. Defaults are **1.0**, with no replacement armor tiers or damage caps.

### 1.7.0 aircraft balance
Aircraft profiles reduce non-explosive small-arms structural damage: 20% for sub-12 mm and 45% for 12–under-20 mm, only up to 80 mm nominal penetration. Blasts, HE/HEAT/APHE, cannon calibers and high-penetration endgame weapons retain full structure scaling. Direct engine hits damage native engine HP separately and pass 25% of engine damage to hull HP. Destroyed aircraft engines require repair before passive recovery. Native flight behavior remains in control of engine failure.
Applies to A10, AC130, J16, Ju87, AH6, Mi28, drone and Kirov profiles. All thresholds/factors are editable in armor JSON. includeDefaultAircraftProfiles adds missing aircraft balance fields on upgrade without overwriting existing values. Existing tank profile values and tank damage rules are preserved. Jade displays engine HP and predicted engine/hull damage.
