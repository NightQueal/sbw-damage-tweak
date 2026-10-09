// CIVILIZED SBW update. Vehicle assembly and gun recipes are intentionally preserved.
ServerEvents.recipes(event => {
  // Plain metal plates come only from the mechanical press.
  ['superbwarfare:steel_plate', 'superbwarfare:copper_plate'].forEach(id => event.remove({output: id}));
  event.custom({
    type: 'create:pressing',
    ingredients: [{item: 'superbwarfare:steel_ingot'}],
    results: [{item: 'superbwarfare:steel_plate'}]
  }).id('civilized_sbw:pressing/steel_plate')
  // Keep Create's copper-ingot pressing recipe unambiguous.
  event.custom({
    type: 'create:pressing',
    ingredients: [{item: 'create:copper_sheet'}],
    results: [{item: 'superbwarfare:copper_plate'}]
  }).id('civilized_sbw:pressing/copper_plate')

  event.remove({id: 'superbwarfare:armor_plate'})
  event.custom({
    type: 'create:compacting',
    ingredients: [
      {item: 'superbwarfare:steel_plate'}, {item: 'superbwarfare:steel_plate'},
      {item: 'minecraft:string'}, {item: 'minecraft:string'}
    ],
    results: [{item: 'superbwarfare:armor_plate'}]
  }).id('civilized_sbw:compacting/armor_plate')

  event.custom({
    type: 'create:mixing', heatRequirement: 'heated', processingTime: 200,
    ingredients: [
      {item: 'minecraft:iron_ingot'}, {item: 'minecraft:iron_ingot'},
      {item: 'minecraft:iron_ingot'}, {item: 'minecraft:iron_ingot'},
      {item: 'minecraft:charcoal'}
    ],
    results: [{item: 'superbwarfare:steel_ingot', count: 4}]
  }).id('civilized_sbw:mixing/steel_ingot')

  // Preserve the original key reset recipe; replace only the new-key assembling recipe.
  event.remove({id: 'superbwarfare:vehicle_key_assembling'})
  event.shapeless('superbwarfare:vehicle_key', [
    'minecraft:iron_nugget', 'minecraft:iron_nugget', 'create:brass_sheet', 'minecraft:redstone'
  ]).id('civilized_sbw:vehicle_key')
})
