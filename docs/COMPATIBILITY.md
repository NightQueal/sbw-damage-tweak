# Compatibility

## Supported versions

Minecraft 1.20.1 and Forge 47, tested with Forge 47.4.26:

| Mod | Tested version |
| --- | --- |
| Superb Warfare | 0.8.9.1 hotfix, commit 993063bed |
| TACZ | 1.1.8 |
| Create Big Cannons | 5.11.3 |
| Create | 6.0.8 |
| Jade (optional) | 11.13.2 |
| Explosion Overhaul (optional) | 0.2.3.0 |

Install the supported mods' own dependencies. This mod does not bundle their classes or jars.

## Explosion and projectile compatibility

TACZ stores configured explosion damage separately from radius. The vehicle compatibility path uses that configured damage with SBW's distance/exposure calculation, then calls native vehicle damage handling.

CBC damage sources can omit shooter/projectile attribution. Vehicle hits receive the projectile and its owner, or the projectile as attacker when ownerless, allowing SBW's native ownership and direction checks to run. Supported shell, impact, flak, and mortar-stone explosions retain CBC's radius-based damage formula.

The repeat-impact safeguard allows one CBC direct hit per vehicle per projectile. Separate explosion damage and other vehicles remain hittable.

The new damage controls retain native damage-source flags and ownership. CBC source metadata distinguishes direct impacts from explosions without changing its source object for player/mob damage.

## Coverage and limitations

Advanced armor applies to mapped projectile/vehicle profiles, including supported SBW mounted weapons. Unknown armor combinations retain native behavior. Damage tuning does not require an armor mapping.

TACZ add-on gun packs use their exact gun IDs. SBW mounted weapons use keys reported by the armor scan; CBC uses projectile entity IDs. Add-on projectiles that bypass the intercepted firing/damage paths may require additional integration.

Melee, beams, scripted health changes and secondary effects without projectile attribution are outside the gun-damage controls. Explosion radius and terrain destruction are unchanged.

Jade inspection requires loaded entities and respects obstruction. Its range does not change interaction reach. Armor and penetration values are gameplay estimates; multiplayer balance still needs playtesting.

No recipes, equipment restrictions, ore generation changes or pack scripts are included in this mod.
