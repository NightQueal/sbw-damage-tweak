// Optional pack restrictions; independent of the combat mod.
JEIEvents.hideItems(event => {
 const equipment = global.civilizedSbwEquipment || {disableNetheriteHammer: true, disableDpsGenerator: true}
 if (equipment.disableNetheriteHammer) event.hide('superbwarfare:netherite_hammer')
 if (equipment.disableDpsGenerator) event.hide('superbwarfare:dps_generator_deployer')
})
