# Optional CIVILIZED equipment restrictions

These KubeJS 6 / Forge 1.20.1 scripts are separate from the combat mod and work without it. Copy this `kubejs/` folder into the Minecraft instance/server. Keep the configuration on the client too for JEI hiding. Restart after changing startup scripts or toggles.

Edit `kubejs/config/civilized_sbw_equipment.json`: `disableNetheriteHammer` and `disableDpsGenerator` are independent booleans, default true. The server controls recipes, item use, inventory cleanup and generator spawn/load rejection; the client controls its JEI visibility. Setting a toggle false allows that equipment but cannot restore previously removed items/entities.

`startup_scripts/civilized_sbw_equipment.js` handles Forge events. Generator entities are rejected on spawn/chunk load, so no scan of every loaded entity is needed. Player inventories are checked once per second rather than every tick. Item use and newly dropped banned items are blocked immediately. `server_scripts/civilized_sbw_equipment_recipes.js` removes recipes. `client_scripts/civilized_sbw_hidden.js` hides the two items in JEI (requires KubeJS JEI integration on the client).

When upgrading from 1.5.0 or earlier, remove the obsolete `Java.loadClass('civilized.sbw.CompatConfig')` equipment block from your existing `civilized_sbw_recipes.js`, and replace the old `civilized_sbw_hidden.js`. Preserve other pack recipes and data. Copy the former `[equipment]` preferences into the JSON, then remove that obsolete section from `config/civilized-sbw-common.toml`. The 1.5.1 jar contains no equipment bans and no inventory/entity cleanup listeners.

Validation: all 20 Forge GameTests passed with both bans enabled and again with both disabled, covering spawn rejection, dropped items, use cancellation, inventory cleanup, recipe presence, and unchanged combat tests. Client JEI visibility has not been manually checked.
