// Recipes and server-side bans enforce this; hiding only improves JEI browsing.
JEIEvents.hideItems(event => {
 if (Java.loadClass('civilized.sbw.CompatConfig').DISABLE_HAMMER.get()) event.hide('superbwarfare:netherite_hammer')
 if (Java.loadClass('civilized.sbw.CompatConfig').DISABLE_DPS.get()) event.hide('superbwarfare:dps_generator_deployer')
})
