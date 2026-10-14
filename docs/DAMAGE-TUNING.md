# Damage tuning (1.8.0)

Edit `config/civilized-sbw-common.toml` on the server (or your single-player instance). Restart after editing, then reconnect clients so Jade receives the updated settings. Install the same mod version on server and clients.

The new `damageTuning` sections independently control **direct hits** and **explosions** against **vehicles**, **players**, and **mobs**, for **TACZ**, **SBW**, and **Create Big Cannons**. Mobs includes all non-player living entities, including animals. Each section has a global `multiplier` and an additional per-weapon `weaponMultipliers` list. New defaults are `1.0` and an empty list, preserving existing damage.

## Examples

```toml
# Halve M95 input damage against players; other TACZ weapons stay unchanged.
[damageTuning.tacz.players.direct]
multiplier = 1.0
weaponMultipliers = ["tacz:m95=0.5"]

# Increase RPG blast damage against vehicles, without changing player/mob damage.
[damageTuning.tacz.vehicles.explosion]
multiplier = 1.0
weaponMultipliers = ["tacz:rpg7=1.5"]

# Reduce all SBW direct projectile damage against mobs.
[damageTuning.sbw.mobs.direct]
multiplier = 0.75
weaponMultipliers = []

# Reduce CBC armor-piercing shot damage against players.
[damageTuning.cbc.players.direct]
multiplier = 1.0
weaponMultipliers = ["createbigcannons:ap_shot=0.5"]
```

Edit the existing generated section rather than adding a second copy of the same TOML section.

The factor is `multiplier × matching weapon override`. A missing override is `1.0`; the last duplicate ID wins. Each configured value accepts 0–1000. Zero disables health damage for that category. It does not disable the projectile, explosion terrain damage, knockback, or other weapon effects.

## Weapon IDs

- TACZ: the gun-pack ID, such as `tacz:m95`. Add-on packs work without armor profiles.
- SBW handheld guns: the item ID, such as `superbwarfare:rpg`, captured when the gun fires.
- SBW mounted guns: the existing `sbwgun:...|...` key, distinguishing mounted weapons and their selected ammunition. Run `/sbwcompat armor scan` and copy the mounted row's **`damageTuningId`** from `config/civilized-sbw-armor-scan.json`.
- SBW projectiles without captured gun data: the projectile entity ID. A projectile already in flight before upgrading uses this fallback.
- CBC: the projectile entity ID, such as `createbigcannons:he_shell`.

These controls handle attributed projectile damage. Melee, beams, scripted health changes, fire damage after impact, and secondary explosions that discard the original projectile attribution are not treated as gun hits.

## Interaction with armor and older settings

Damage is scaled before the target's defenses. Existing falloff, headshots, attachments, TACZ's normal/armor-piercing split, player PvP rules, and native damage-source flags remain intact. The same direct factor applies to both portions of a TACZ bullet.

Vehicle tuning applies before native or advanced vehicle armor, including vehicle-armor-bypassing hits. Raising damage does not raise penetration: a blocked hit may still deal zero. Native armor can be nonlinear, so doubling input damage does not always double health loss.

The older `[balance]` TACZ blast and CBC multipliers remain active and multiply the new vehicle controls. They do not affect the new player/mob categories. Keep one layer at `1.0` if you want to avoid stacking adjustments.

Server settings are synchronized to clients on login for Jade's estimated vehicle direct-hit damage. Gun diagrams and gun-item tooltips continue to show the gun's base statistics, since player, mob, and vehicle damage can now differ.
