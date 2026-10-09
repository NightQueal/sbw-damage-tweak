// Optional CIVILIZED pack rules. Independent of SBW Damage Tweak.
const civilizedEquipmentPath = 'kubejs/config/civilized_sbw_equipment.json'
const civilizedEquipmentDefaults = {disableNetheriteHammer: true, disableDpsGenerator: true}
const civilizedEquipmentFile = JsonIO.read(civilizedEquipmentPath)
if (civilizedEquipmentFile == null) JsonIO.write(civilizedEquipmentPath, civilizedEquipmentDefaults)
global.civilizedSbwEquipment = {
 disableNetheriteHammer: civilizedEquipmentFile == null || String(civilizedEquipmentFile.disableNetheriteHammer) !== 'false',
 disableDpsGenerator: civilizedEquipmentFile == null || String(civilizedEquipmentFile.disableDpsGenerator) !== 'false'
}
function civilizedEquipmentItemBanned(stack) {
 if (stack.isEmpty()) return false
 const id = String(stack.id)
 return (global.civilizedSbwEquipment.disableNetheriteHammer && id === 'superbwarfare:netherite_hammer') ||
  (global.civilizedSbwEquipment.disableDpsGenerator && id === 'superbwarfare:dps_generator_deployer')
}
ForgeEvents.onEvent('net.minecraftforge.event.entity.EntityJoinLevelEvent', event => {
 if (event.getLevel().isClientSide()) return
 const entity = event.getEntity()
 const id = String(entity.type)
 // Includes newly spawned generators and existing generators loaded from chunks.
 if (global.civilizedSbwEquipment.disableDpsGenerator && id === 'superbwarfare:dps_generator') event.setCanceled(true)
 if (id === 'minecraft:item' && civilizedEquipmentItemBanned(entity.item)) event.setCanceled(true)
})
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem', event => {
 if (!event.getLevel().isClientSide() && civilizedEquipmentItemBanned(event.getItemStack())) event.setCanceled(true)
})
ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$PlayerTickEvent', event => {
 const player = event.player
 if (String(event.phase) !== 'END' || player.level.isClientSide() || player.age % 20 !== 0) return
 if (!global.civilizedSbwEquipment.disableNetheriteHammer && !global.civilizedSbwEquipment.disableDpsGenerator) return
 const inventory = player.getInventory()
 for (let slot = 0; slot < inventory.getContainerSize(); slot++) {
  const stack = inventory.getItem(slot)
  if (civilizedEquipmentItemBanned(stack)) stack.setCount(0)
 }
})
