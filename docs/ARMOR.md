# Experimental armor model (1.3.0)



Enable [armor] advancedArmor in config/civilized-sbw-common.toml. It is false by default in the public jar; the CIVILIZED test instance enables it. Turning it off restores native armor. ricochets and componentHits are independent switches. Restart after edits.



config/civilized-sbw-armor.json is generated automatically on first launch, with explicit vehicle and ammunition profiles. No world-specific files are used. Invalid numerical/type entries stop loading with a descriptive error instead of silently applying bad balance values. Restart to reload JSON.



## Armor values

All plate values are gameplay estimates in millimetres of RHA-equivalent protection, NOT authoritative physical armor thickness or classified composite armor figures. They establish a progression between soft vehicles, IFVs and tanks. They need playtesting. Front, side, rear, roof and bottom are independent; turretMultiplier modifies turret plates. chemicalMultiplier adds protection against HEAT. Wheels/tracks have trackArmor. engineDamageMultiplier scales a penetrating engine hit (including hull health loss, since SBW applies the same computed hit to hull and component).



Examples (front / side / rear / roof):

- M1A2: 420 / 75 / 40 / 25; chemical multiplier 1.8.

- T90A: 360 / 70 / 40 / 25; chemical multiplier 1.7.

- ZTZ99A: 400 / 80 / 45 / 30; chemical multiplier 1.8.

- Bradley: 55 / 30 / 20 / 12.

- BMP2: 33 / 16 / 13 / 8.

- LAV: 20 / 12 / 10 / 8.

- Truck: 3 / 2 / 2 / 1.



Profiles cover every vehicle in the installed SBW 0.8.9.1 data set, including fantasy vehicles with explicitly estimated values. An unknown/addon vehicle falls back to its native armor.



## Ammunition

Only explicitly mapped ammunition gets this model. TACZ keys are gun:tacz:rpg7 or ammo:tacz:50bmg; gun keys take precedence. Other projectile keys are entity:createbigcannons:ap_shot. Default mappings cover stock TACZ ammo and supported CBC projectiles. Custom gun-pack variants need an explicit entry. Existing SBW-owned weapon damage stays native unless you deliberately map its projectile ID. Do not map generic projectile IDs that represent different warheads unless that is intentional.



Supported types: FMJ, AP, APC, APCR, APHE, HE, HEAT. FMJ means full metal jacket: a jacket alone does not make ammunition armor-piercing. AP/APC profiles can have stronger penetration and normalization. APCR may be configured with smaller penetrator caliber, little normalization and earlier ricochet. APHE has a post-penetration damage multiplier, while HE has low direct penetration and configurable fragmentation/blast penetration. HEAT uses chemical protection and no velocity attenuation.



CBC native data does not provide real-world caliber or millimetre penetration. Its supplied caliber/penetration numbers are tuning values, not measured weapon specifications. TACZ also does not identify arbitrary custom AP/FMJs automatically.



## Formula and geometry

For mapped kinetic rounds with referenceSpeed > 0:

P = P_reference * min(3, speed / referenceSpeed)^velocityExponent.

Default exponent 1.43 is a normalized de Marre-style approximation for the same projectile. It is not a complete material/ballistic simulation; reference penetration incorporates projectile construction and size. Speed uses Minecraft blocks per tick; referenceSpeed = 0 disables velocity scaling. HEAT penetration does not depend on projectile speed.



Angle is measured from the plate normal: 0 degrees is perpendicular, 90 is grazing. For kinetic ammunition, normalization subtracts configured degrees. Effective plate = plate * chemicalMultiplier(if HEAT) / max(0.02, cos(angle - normalization)). Vehicle pitch/roll/yaw and rotated SBW hitboxes affect the normal. Incoming rays intersect SBW oriented component boxes, not arbitrary zones guessed from vehicle center.



Ricochet probability increases linearly from ricochetStart to ricochetEnd. The roll is stable per projectile/vehicle/tick, including debug recalculation. Configurable caliber/plate overmatch (default ratio 7) removes ricochet and slope penalties for kinetic rounds. Because plates use gameplay RHA equivalents, this overmatch is also an approximation, not a physically exact caliber-to-steel ratio.



If penetration <= effective plate, direct damage is zero. Otherwise residual fraction = 1 - effective plate / penetration; direct damage = incoming damage * (0.25 + 0.75 * residual fraction) * postPenMultiplier. This residual-to-health conversion is a gameplay choice, not a real-world injury formula.



A penetrating ray can intersect a native engine box behind the exterior plate. The hit routes into SBW engine health; engine failure remains SBW's native behavior. Wheel hits retain their wheel routing. Turret hits retain native turret routing unless the penetrating ray finds an engine. No new crew, fuel-fire, ammo-rack detonation, or secondary spall simulation is claimed.



Area blast uses blastPenetration and blastLeak; it cannot ricochet. HEAT at the impact point uses a short forward ray (2 blocks behind to 4 blocks ahead of the explosion) for the focused jet; off-axis targets receive ordinary blast rather than full HEAT penetration. Sphere blast exposure/distance remain the existing compatibility calculation. Missing projectile motion cannot establish a focused jet and uses area blast. This is a game approximation, not a full shaped-charge jet simulation.



Mapped hits replace the native DamageModifier armor output; they do not stack two armor reductions. Vehicle-level immunities, ownership and friendly-fire checks before that stage remain. Existing compatibility multipliers apply before armor. Explosion/source attribution remains intact. Unmapped damage sources keep native modifiers.



## Limitations

Ricochet currently rejects the glancing hit; it does not redirect the visible projectile or spawn a secondary bounced projectile. Native projectile continuation/explosion behavior remains. Geometry is SBW's box approximation; curved plates and welds are not modeled. One exterior plate is tested before an engine, not a full sequence of spaced armor layers. Chemical protection is a permanent multiplier, not individually consumed ERA tiles. HE/APHE fuse timing and CBC detonation logic are not replaced. Full-pack multiplayer balance and client rendering remain untested.



Only actual hits perform box intersection; no per-tick armor scans or proximity scans are added. Profiles load once per restart.



## References (design inspiration, no source code copied)

- https://wiki.warthunder.com/mechanics/242-armour (line-of-sight armor and material/chemical protection concepts)

- https://wiki.warthunder.com/661-shell-normalization-against-angled-armor-in-war-thunder (ammunition-dependent normalization)

- https://wiki.warthunder.com/jacob_de_marre (reference ballistic penetration)

- https://warthunder.com/en/devblog/current/6023 (caliber/plate overmatch and ricochet)



This is an independent Minecraft gameplay model inspired by those concepts; it does not reproduce War Thunder's full simulation or claim real-world predictive accuracy.



## Validation

Eleven isolated Forge integration tests passed both with and without Explosion Overhaul. They cover kinetic speed loss, angled armor, ricochet endpoints, thin-plate overmatch, HEAT speed independence and chemical protection, rotated OBB normals/misses, per-vehicle differences, native engine routing, a mapped TACZ HEAT impact, unknown-ammo fallback, native-model restoration, actual CBC AP projectile flight, existing damage compatibility and equipment toggles.



Fixture results (not universal damage guarantees): CBC AP rear engine shot on M1A2 caused about 65.88 hull damage; the heavy front blocked more effectively. TACZ FMJ caused about 27.27 damage to the soft truck and zero to the tank front. Rear HEAT impact caused about 147.41 tank damage with engine damage. Actual CBC AP flight caused about 52.68 damage with the advanced model. Full-pack multiplayer tuning remains pending.



## Scan for missing mappings

Operator command: /sbwcompat armor scan. Writes config/civilized-sbw-armor-scan.json on the server/instance. Includes registered SBW/addon vehicle subclasses, SBW/CBC projectile IDs, loaded TACZ gun packs and ammo IDs, effective gun-or-ammo mappings, base gun damage, projectile counts/speed, blast damage, stale profile keys and inspection errors. A gun sharing mapped ammo is covered without a gun override. Utility projectiles may intentionally remain unmapped. This command does not change live profiles or fabricate penetration values. Run again after loading new packs; the previous generated report is replaced. Permission level 2 required. Entity factories are inspected without spawning entities; a large pack can cause a brief pause during the command, but there is no continuous scan.



TACZ gun damage is already included: the actual shot's damage arriving at SBW is the input to armor (including adjustments TACZ applied before hurt). Profiles determine penetration, not a replacement gun damage number. Explosion correction uses TACZ's configured blast damage. Two guns using the same ammo profile can deal different post-penetration damage. Gun-specific penetration/warhead differences use gun: overrides. Report damage/speed values are base gun definitions, not a promise about attachment/Lua/event-modified shots.



1.2.1 validation: twelve integration tests passed, including execution of the registered command, report file creation, rejection of a non-operator command source, loaded gun/vehicle mapping detection and proportional preservation of incoming shot damage.



## Ammunition attachments and tooltips (1.3.0)



A TACZ bullet snapshots the installed extended-magazine ammunition attachment when the actual bullet constructor runs. Swapping guns or attachments after firing cannot change an in-flight round. Gun-specific profiles still take precedence over ammo profiles; the attachment then modifies that selected profile. Unknown attachments leave it unchanged. HEAT warheads keep their existing profile. TACZ damage/speed/explosion modifiers remain TACZ's responsibility and are not applied a second time.



The new `attachments` map in `config/civilized-sbw-armor.json` accepts exact TACZ attachment IDs, including third-party IDs. Existing configs without this section receive only the new section at startup, preserving vehicle and ammunition values. An explicit empty map disables attachment adjustments. Fields are type, penetrationMultiplier, blastPenetrationMultiplier, normalizationAdd and postPenMultiplier. These are gameplay estimates, not measured ballistics.



| Attachment | Penetration | Normalization | Blast penetration | Type |

|---|---:|---:|---:|---|

| FMJ | x1.35 | +3 degrees | x1 | FMJ |

| Hollow-point | x0.45 | unchanged | x1 | HP |

| Incendiary | x0.85 | unchanged | x1 | INCENDIARY |

| Explosive | x0.60 | unchanged | x3 | HE |

| Slug | x1.20 | unchanged | x1 | FMJ |



No extra burn damage, spall, ammo-rack detonation or visible projectile bounce is added. Incendiary ignition uses TACZ behavior. Explosion damage/radius come from the gun pack. Default TACZ profiles retain fixed penetration with range; optional referenceSpeed still uses actual impact velocity.



Gun tooltips show effective ammunition type and nominal direct/blast penetration. Hold Shift for normalization, ricochet thresholds, overmatch, blocked-blast leakage, engine behavior, range behavior, TACZ attachment-adjusted damage bands/launch speed/blast and hull-side examples for Truck, BMP-2 and M1A2 at 0/30/60 degrees. Examples are percentages of incoming direct damage for non-ricochet hits, not guaranteed final HP loss. Shooter scripts, cover, pellet splitting, actual engine intersections and compatibility scaling can alter results. The existing TACZ attachment tooltip is extended with vehicle profile multipliers and available native damage/speed/range/RPM modifiers. Other accessories keep TACZ's existing tooltip.



Armor profiles and active armor/ricochet/component switches are synchronized from the server once at login. Clients do not use their own armor JSON for connected-server tooltips. Disconnect clears the snapshot; restart/reconnect after configuration edits. Before joining a world, a tooltip asks the player to connect for settings. Both client and server require 1.3.0 for this network channel; update both together.



Validation: build and fourteen isolated Forge game tests passed with Explosion Overhaul, including the actual TACZ constructor capturing FMJ, preservation after unloading, unchanged TACZ damage, FMJ penetration versus hollow-point rejection, HE/HEAT distinctions, unknown attachment fallback, shared-profile immutability and profile packet round-trip. Tests now run in separate batches to prevent their temporary global config switches interfering. Tooltip appearance and full-pack multiplayer combat have not been visually/play tested.



## Compact stats UI (1.3.1)



Replaced explanatory hover text with pure numerical stats. Removed ammunition-type headings, raw attachment IDs, vehicle examples and repeated mechanical caveats. Native TACZ attachment tooltips receive signed vehicle penetration/blast/normalization modifiers with positive/negative coloring, plus penetration before/after for a compatible held gun. Unchanged modifiers are hidden. Native TACZ refit diagrams gain four rows: vehicle penetration, blast penetration, normalization and ricochet. Their background and toggle-button position expand together. Gun hover stats retain compact Shift damage bands and angle/caliber/overmatch values. Float.MAX_VALUE damage-range sentinels display as a final range such as 150m+, never as a huge block count. Changes affect display only; damage balance is unchanged.



Formatting checks cover the finite and unlimited damage ranges and signed attachment percentages. Client diagram and attachment mixins are client-only; server integration tests still cover damage behavior. Full client layout needs an in-game check.



## Vehicle inspection (1.3.2)

Vehicle containers/deployers and spawn eggs show profile front/side/rear armor. Shift adds roof/bottom/track or wheel armor and chemical/turret/engine multipliers. The values come from the connected server's armor profiles and are hidden when advanced armor is disabled.



Optional Jade 11 integration adds the same armor stats when looking at a deployed SBW vehicle. Jade's configured detail key (normally Shift) expands the overlay. Holding a mapped TACZ gun adds aim-ray impact angle, effective armor, nominal penetration, penetration chance, ricochet chance and the percentage of incoming damage retained after penetration. It traces the vehicle's actual OBBs and applies the same normalization, chemical protection, turret/wheel thickness and overmatch rules as combat. An unmapped gun or a ray that cannot resolve an armor surface produces no shot estimate.



Penetration chance is a nominal-velocity armor estimate conditional on hitting that surface, not accuracy or a destruction probability. Armor thresholds are deterministic; ricochet supplies the probability. Shot velocity loss, shooter scripts, projectile spread, subsequent engine intersections and separate explosions are not predicted. HEAT estimates describe the focused jet. Client aim can differ from the eventual server projectile impact. No per-tick server scans or new damage calculation are added.



Validation adds tests for real SBW container identification, nested deployer data, compact/detailed armor rows, blocked penetration, overmatch and ricochet-adjusted probability. Fifteen isolated Forge tests passed with Jade and Explosion Overhaul. Client overlay appearance needs an in-game check.



## Impact sound feedback (1.3.3)

Ricochet is the conventional term for a glancing projectile impact. The armor model simulates rejection of the hit, not a physical reflected trajectory; this update does not redirect projectiles.



Mapped advanced-armor direct impacts and focused HEAT jets produce distinct penetration, blocked and ricochet cues. Ordinary area blast is excluded to avoid misleading penetration cues for surrounding vehicles. Cues come from the computed result at the armor surface, not client estimates. They do not change damage, friendly-fire rules or native projectile/explosion effects. One cue is reserved per projectile/vehicle; the default two-tick per-vehicle interval limits automatic-fire sound spam. Native game/mod impact and explosion audio remains.



Settings are under [impactFeedback] in config/civilized-sbw-common.toml. enabled disables all added cues; shooterFeedback controls private long-range notification to the firing player. Nearby players hear the impact-position cue, with the privately notified shooter excluded from that broadcast. Sound IDs, three pitches, overall volume and minimumIntervalTicks are configurable. Missing configured sound IDs fall back to shield.block. Defaults use existing vanilla sound events, so no new audio files are redistributed.



Validation adds result classification, duplicate/rate-limit checks, sound registry resolution, disabled-feedback behavior and actual server sound emission. Sixteen isolated game tests passed with Jade and Explosion Overhaul. Audibility and full-pack sound mixing require an in-game check.



## Diagram spacing (1.3.4)

Added ten pixels of vertical space before the vehicle stat rows. This moves them clear of TACZ's final Weight row and expands the panel/button offset together. No damage or penetration values change.



## Gun tuning and long-range inspection (1.4.0)

Added 132 exact gun-ID defaults for pack guns using already mapped ammunition. [GUN-TUNING.md](GUN-TUNING.md) records each speed, reference and final penetration. Caliber baselines remain unchanged. Speed-based multipliers are bounded to 0.70-1.25; the values remain fixed penetration estimates, not newly enabled distance/velocity loss. Identical ammunition and speed can intentionally give identical penetration despite different damage. Excaliber and the two radius-6 M320 grenade launchers receive 15 mm blast penetration; rocket HEAT profiles remain unchanged. Attachments still apply after the gun override.



Existing configs receive only missing bundled gun keys at startup; existing gun, ammo, vehicle and attachment entries are preserved. Set includeDefaultGunProfiles=false in armor JSON to disable automatic additions and manually remove overrides to restore ammo fallback.



Jade extends vehicle-only aim inspection to 200 blocks while holding a TACZ gun. [inspection] vehicleRange in the client common config accepts 16-256. Uses loaded/tracked vehicles, actual OBBs and solid-block occlusion; no entity or chunk loading, weapon range change, interaction reach extension or damage changes. Detail-key behavior is unchanged. Unloaded/untracked vehicles cannot be inspected. Server tracking/view distance must make the vehicle available to the client.



Validation adds gun-profile migration, Excaliber blast tuning, Uzi versus Glock penetration, preservation of RPG override, a vehicle inspection at 150 blocks, configured distance rejection and stone-wall occlusion. Client Jade presentation still needs an in-game check.



Distant accessors use client-only data to avoid repeated out-of-range Jade server requests; normal nearby vehicle accessors are preserved. Seventeen integration tests passed.



## 1.5.0 — mounted SBW weapons



Mapped SBW mounted projectile weapons now use the same advanced armor calculation as TACZ/CBC. `armor.mountedWeaponArmor=true` additionally requires `armor.advancedArmor=true`. Turn it off to retain native SBW mounted damage. The setting is synchronized at login (network protocol 3; both client and server must update).



The selected `GunData` ammunition consumer is frozen on each projectile in SBW 0.8.9.1's `GunItem.shootBullet$lambda$19` factory before world insertion. Profiles are resolved as `sbwgun:<translated-weapon-name-key>|<ammo-id>` followed by `sbwammo:<ammo-id>`. Fired rounds do not change when the shooter switches weapons or ammunition. Handheld SBW guns are excluded using the native VEHICLE_GUN item identity. Original SBW raw gun damage is fed into armor once; AP ratio is not repurposed as penetration.



Mapped native CustomExplosion vehicle hits are processed once using the native configured damage, doubled radius, vehicle exposure, and distance formula, then advanced armor. Native explosions on other entities and unmapped vehicles/ammo are retained. Mounted damage does not inherit TACZ/CBC balance multipliers. HEAT uses its focused impact ray and chemical protection; off-axis targets use ordinary blast penetration.



While riding an SBW vehicle, 200-block Jade inspection uses the controlling seat's selected weapon and real shoot position/direction, even with an empty hand. Mounted details expand automatically by default (`inspection.mountedDetails=true`) so Shift/dismount is unnecessary; disable that client setting to require the usual detail key. Details include selected weapon/ammunition, angle, effective armor, nominal direct and blast penetration, ricochet and penetration chance, and remaining direct damage. Unarmed passengers see armor only unless they hold a TACZ gun, which uses its own profile and eye ray. The viewer's own vehicle is excluded. This is an instantaneous muzzle-ray estimate: gravity, future guidance, spread, and travel-time changes are not predicted. Loaded entities and terrain obstruction still apply.



There are 24 ammo-class defaults and 5 weapon/ammo overrides, all configurable gameplay estimates, not real ballistic measurements: large AP 600 mm; medium AP 140 mm; small AP 60 mm; BMP-2 30 mm AP 75 mm; GAU-8 30 mm AP 80 mm; 20 mm AP 40 mm; rifle MG 12 mm; heavy MG 25 mm; medium anti-ground HEAT 600 mm. Large HE direct/blast 35/35 mm. Energy rays, melon projectiles and swarm drones remain native and receive no false penetration statistics. Secondary spawned cluster/white-phosphorus fragments without captured metadata remain native.



`includeDefaultMountedProfiles=true` adds only missing bundled `sbwammo:` and `sbwgun:` entries on load, preserving custom values; false opts out. `/sbwcompat armor scan` now includes mounted weapons and all selectable ammo modes with their effective and suggested profile keys.



Validation: all 20 Forge GameTests passed, including real mounted AP firing, factory metadata capture, selected ammo/weapon/seat switching, mounted ammo scan, native CustomExplosion armor with no duplicate damage, and existing TACZ/CBC tests. The client overlay selection compiles; in-game visual positioning has not been manually checked.

