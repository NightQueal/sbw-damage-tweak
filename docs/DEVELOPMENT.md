# Development

Requires Java 17, Python 3, and the compatible mod jars listed in `scripts/dependencies.json`. Dependency jars are not distributed in this repository.

## Prepare dependencies

Point the helper at a Minecraft instance containing those jars:

```sh
python scripts/prepare-dev.py "/path/to/minecraft-instance"
```

It verifies checksums and copies dependencies into the ignored `runtime-mods/` directory. The source instance is not modified. Add `--with-explosion-overhaul` to include the optional Explosion Overhaul compatibility test dependency.

## Build and test

```sh
./gradlew build
./gradlew runGameTestServer
```

On Windows use `gradlew.bat`. The release jar is generated in `build/libs/`. GameTests run in the isolated, ignored `test-run/` directory; they do not use player saves. Integration test classes and the test structure are excluded from the release jar.

The combat suite covers native and advanced armor, TACZ/CBC/SBW direct hits and explosions, target-specific damage controls, ammunition attachments, mounted weapons, component health, inspection, and profile synchronization. Successful compilation alone does not run GameTests.

## Internal identifiers

The existing `civilized_sbw` mod ID, package names and config filenames remain stable for compatibility. The distribution filename is `sbw-dmg-tweak-<version>.jar`.

Client/server profile synchronization has a versioned protocol; 1.8.0 must be installed on both sides.
