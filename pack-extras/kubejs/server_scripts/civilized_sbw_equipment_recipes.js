// Optional CIVILIZED pack equipment recipes. Independent of SBW Damage Tweak.
ServerEvents.recipes(event => {
 const equipment = global.civilizedSbwEquipment || {disableNetheriteHammer: true, disableDpsGenerator: true}
 if (equipment.disableNetheriteHammer) event.remove({output: 'superbwarfare:netherite_hammer'})
 if (equipment.disableDpsGenerator) event.remove({output: 'superbwarfare:dps_generator_deployer'})
})
