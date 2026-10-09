# CIVILIZED SBW Compatibility

Forge 1.20.1 compatibility for Superb Warfare, TACZ and Create Big Cannons. Version **1.3.1** adds an optional, configurable armor model. Native armor remains the default; enable advancedArmor to test the new model.

## Features
- Attachment-aware FMJ/HP/incendiary/HE/slug vehicle penetration and server-synchronized gun/attachment tooltips. Hold Shift on a gun for detail.
- On-demand /sbwcompat armor scan report for unmapped vehicles, TACZ guns/ammo and projectile IDs.
- Optional per-vehicle armor, ammunition penetration, glancing-hit rejection and native engine-hit routing. See [ARMOR.md](docs/ARMOR.md).
- Correct TACZ explosions to use configured blast damage through SBW's distance/exposure formula.
- Supply projectile/shooter attribution for CBC vehicle hits and supported shell explosions.
- Configurable prevention of repeated CBC direct impacts on the same vehicle.
- Separate global, gun/projectile and vehicle input multipliers. Defaults are **1.0**, with no replacement armor tiers or damage caps.
- Independent toggles for CIVILIZED restrictions on the SBW Netherite Hammer and DPS Generator.

## Install
Use [the packaged 1.3.0 jar](dist/civilized-sbw-compat-1.3.1.jar) in the instance/server mods directory. The SHA-256 is recorded in [dist/SHA256SUMS](dist/SHA256SUMS). The mod generates config/civilized-sbw-common.toml in the instance/server folder. Edit that file or copy [the configuration template](config/civilized-sbw-common.toml) there. Settings apply to every world in that instance; on multiplayer servers, the server's config controls damage. Restart after changes. To preserve old settings, copy the previous world's serverconfig/civilized-sbw-server.toml to config/civilized-sbw-common.toml before launching 1.2.1.

Requires Minecraft 1.20.1, Forge 47, Superb Warfare 0.8.9.1, TACZ 1.1.8 and Create Big Cannons 5.11.3, plus those mods' normal dependencies. The original instance uses Forge 47.4.10; the isolated development tests use 47.4.26.

## Configuration and damage behavior
See [COMPATIBILITY.md](docs/COMPATIBILITY.md) for exact options, source analysis, measured damage and proposed optional balance trials. Smoke/fluid utility explosions are not intercepted. CBC retains its box-based hit detection; detailed SBW hull collision is not replaced. Native ownership/friendly-fire checks remain; an ownerless cannon projectile cannot supply a player's team.

## Build and tests
Use JDK 17 or 21 and Python 3.9+. The repository does not redistribute third-party mod jars, worlds, credentials or raw machine logs.

Prepare the exact tested dependency versions from your own modded Minecraft instance:

```text
python scripts/prepare-dev.py "PATH_TO_MINECRAFT_INSTANCE"
```

The script only reads the instance. It copies hash-verified dependencies into ignored runtime-mods/ and installs this repository's fixture data into ignored test-run/. The optional explosion-mod regression uses:

```text
python scripts/prepare-dev.py "PATH_TO_MINECRAFT_INSTANCE" --with-explosion-overhaul
```

On Windows:

```text
gradlew.bat build
gradlew.bat runGameTestServer
```

On Linux/macOS:

```text
./gradlew build
./gradlew runGameTestServer
```

The first build downloads Gradle/Forge development dependencies. Build output is build/libs/civilized-sbw-compat-1.3.1.jar. The test class and empty fixture structure are excluded from the production jar. The armor baseline passed eleven tests with and without Explosion Overhaul. Version 1.2.1 passed twelve tests including the registered scan command and permission checks. Version 1.3.0 passed fourteen tests including attachment capture and profile packet round-trip. Tooltip appearance and full-pack multiplayer combat balance remain untested.

## License
Original compatibility code is MIT licensed. Minecraft, Forge and the other mods retain their respective licenses. Third-party dependency jars are deliberately excluded.

Equipment: in [equipment], set disableNetheriteHammer or disableDpsGenerator to false to allow that item. Defaults remain true. The included KubeJS recipe/JEI scripts honor the toggles; replace older unconditional versions if using them. Restart after edits.
