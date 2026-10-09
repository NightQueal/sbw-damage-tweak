# SBW damage compatibility 1.3.0

This release replaces the archived broad damage profiles with scoped compatibility corrections. SBW's own weapons, vehicle recipes and armor definitions are unchanged.

## What was wrong
TACZ's ProjectileExplosion stores configured blast damage in its power field. SBW removes vehicles from the explosion affected list and treats this as a vanilla explosion, recalculating from radius and exposure rather than configured damage. The correction reads TACZ's configured damage and uses SBW's existing quadratic impact/exposure formula, then calls the original VehicleEntity.hurt path. Damage-source type is preserved.

CBC 5.11.3 creates CannonDamageSource with only damage type and the armor-bypass flag. Both attacker and direct projectile are absent. SBW's early same-driver check compares attacker to last driver; when both are null, it rejects the hit. The correction supplies the actual projectile and its owner when known. If there is no known owner, the projectile itself is the attacker; this avoids null/null rejection and gives native directional/source checks a position. Team protection can only identify a team if CBC provides an owner.

CBC shell, impact, flak and mortar-stone explosions keep their native radius-based blast amount; they do not have TACZ's independent damage field. Attribution is fixed before SBW armor. Utility smoke/fluid/shrapnel-release explosion classes are not intercepted. Direct hits implemented by CBC's AbstractCannonProjectile/AbstractBigCannonProjectile receive attribution; separate addon projectile classes that do not inherit these paths may need additional integration.

CBC can repeatedly damage the same entity while a projectile remains inside its bounding box. The optional default-on safeguard uses CBC's existing per-projectile untouchable-entity mechanism to allow one direct impact per vehicle. It still permits hits on other vehicles and separate blast damage. It does not remove the projectile or create a damage cap.

## Configuration
Instance-wide config: config/civilized-sbw-common.toml. Forge generates it when the mod loads. Every world in the instance uses the same settings; the dedicated server's file controls multiplayer damage. Restart after config edits. Existing per-world configs are no longer read. To migrate customized settings, copy the old world's serverconfig/civilized-sbw-server.toml into config/civilized-sbw-common.toml before launching this version.

Compatibility toggles:
- taczExplosionFix: enable/disable the TACZ blast correction.
- cbcAttributionFix: enable/disable CBC direct/blast attribution and its input tuning.
- cbcOneDirectHitPerVehicle: enable/disable the CBC repeat-impact safeguard (requires attribution fix).
- debugDamage: log damage/exposure during testing; normally false.

Balance:
- taczExplosionMultiplier: global TACZ blast multiplier.
- cbcDirectMultiplier: global CBC direct-hit multiplier.
- cbcExplosionMultiplier: global CBC blast multiplier.
- taczGunMultipliers: exact gun ID = additional blast multiplier.
- cbcDirectProjectileMultipliers: exact projectile ID = additional direct-hit multiplier.
- cbcExplosionProjectileMultipliers: exact projectile ID = additional blast multiplier.
- vehicleMultipliers: exact vehicle ID = additional factor for compatibility-handled hits only.

All multipliers default to 1.0. Global, matching projectile/gun, and matching vehicle factors multiply together BEFORE SBW armor. Entries accept 0 through 1000; zero disables that damage path. Invalid entries are rejected by Forge's config validation. Later duplicate IDs override earlier entries. Unlisted IDs use 1.0. No heavy/medium/light replacement profiles or flat reductions/caps are imposed.

Example OPTIONAL first CBC balance trial (not installed):
cbcDirectProjectileMultipliers = ["createbigcannons:shot=4.0", "createbigcannons:ap_shot=6.0", "createbigcannons:ap_shell=6.0", "createbigcannons:he_shell=2.0"]
Keep global direct and blast multipliers at 1.0 initially. On an M1A2, before any native directional script, default solid/AP inputs 30/50 become 2/6 health damage. The optional 4x/6x profile becomes 20/56. These are proposed test values, not PvP-validated balance. HE remains focused on blast damage; autocannon is not globally boosted.

## Tests and limitations
Six isolated Forge 47.4.26 integration tests passed both without and with Explosion Overhaul 0.2.3.0. The instance uses Forge 47.4.10; required config API methods were checked in that installed Forge jar. Tests covered TACZ original/corrected blast, CBC actual native direct sources and HE explosions, a real AP-shot flight, pre-armor/global/per-projectile/per-vehicle configuration, preserved ordinary-bullet immunity/custom modifier pipeline, non-vehicle scope, recipes and disabled DPS enforcement.
Measured fixture health loss: TACZ blast 0.6357 original / 8.6735 corrected; CBC solid 0 original / 2 corrected; CBC HE blast 0 original / 11.8501 corrected; actual CBC AP flight 6 with repeat-hit protection, versus 24 in the earlier unprotected fixture. These are fixture measurements, not guaranteed hit damage in gameplay.

The native armor/custom-modifier pipeline is retained. CBC continues to use its native axis-aligned entity hit detection; this release does not replace that with SBW's detailed hull hitboxes. Full-pack client launch and multiplayer combat balance have not been tested. User saves are not edited.

Source references:
- SBW installed commit: https://github.com/Mercurows/SuperbWarfare/blob/993063bed/src/main/kotlin/com/atsuishio/superbwarfare/event/LivingEventHandler.kt
- CBC matching version: https://github.com/Cannoneers-of-Create/CreateBigCannons/blob/v5.11.3%2Bmc.1.20.1/src/main/java/rbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile.java

## Equipment toggles
[equipment] disableNetheriteHammer and disableDpsGenerator default to true for CIVILIZED. Set either to false to allow that equipment. Restart after edits. The accompanying KubeJS recipe/JEI scripts now honor these toggles; older unconditional pack scripts must also be replaced. Client JEI visibility follows the client config, while server enforcement follows the server config. Re-enabling does not restore previously removed items/entities.

## Optional advanced armor (1.2.1)
See ARMOR.md. With advancedArmor enabled, mapped projectile/vehicle combinations replace the native armor modifier output. Unknown sources and vehicles retain native armor. The compatibility baseline measurements above describe advancedArmor disabled.

Version 1.3.0 adds attachment-aware penetration and server-synchronized gun/ammunition tooltips; see ARMOR.md. Both client and server must update together.
