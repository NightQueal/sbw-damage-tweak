# Vehicle armor

Enable `armor.advancedArmor` in `config/civilized-sbw-common.toml` to use mapped armor profiles. It is disabled by default. Edit the generated `config/civilized-sbw-armor.json` and restart to reload profiles.

## Profiles

Vehicle profiles define front, side, rear, roof and bottom armor, plus turret, running gear, engine and HEAT protection factors. Values use millimetres of equivalent armor as a gameplay scale, not authoritative physical armor measurements.

Ammunition profiles define direct and blast penetration, caliber, normalization, ricochet angles, overmatch, post-penetration damage and blast leakage. Gun-specific profiles override shared ammunition profiles; supported ammunition attachments apply afterward.

A blocked direct shot deals zero advanced-armor damage. Penetrating shots use remaining penetration to determine damage. Normalization and impact angle affect effective armor; ricochet is probabilistic and rejects damage without physically bouncing the projectile.

## Components

Penetrating rays that intersect engine hitboxes can damage native engine health. Running gear has separate health; a wheel/track hit damages the hull only if the ray also penetrates the armor behind it.

Destroyed tank tracks disable powered movement and steering. Gravity, momentum and towing remain possible. Zero-health running gear must be repaired before passive regeneration resumes.

Aircraft profiles reduce selected small-arms structural damage and distinguish engine damage from hull damage. Destroyed engines require repair before passive regeneration. Native SBW flight behavior controls engine failure.

## Inspection and scanning

Optional Jade inspection shows health, armor, penetration, ricochet probability and estimated damage. Shift expands details; mounted inspection can expand automatically through the config.

Run `/sbwcompat armor scan` as an operator to write `config/civilized-sbw-armor-scan.json`. The command reports mappings, unmapped IDs and mounted weapon damage-tuning keys without changing profiles.

Damage multipliers scale input damage but do not increase penetration. See [damage tuning](DAMAGE-TUNING.md).
