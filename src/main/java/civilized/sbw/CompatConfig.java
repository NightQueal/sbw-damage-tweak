package civilized.sbw;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
public final class CompatConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.DoubleValue INSPECTION_RANGE;
 public static final ForgeConfigSpec.BooleanValue IMPACT_SOUNDS,SHOOTER_CUES;
 public static final ForgeConfigSpec.IntValue IMPACT_INTERVAL;
 public static final ForgeConfigSpec.ConfigValue<String> PEN_SOUND,BLOCK_SOUND,RICOCHET_SOUND;
 public static final ForgeConfigSpec.DoubleValue IMPACT_VOLUME,PEN_PITCH,BLOCK_PITCH,RICOCHET_PITCH;
 public static final ForgeConfigSpec.BooleanValue MOUNTED_ARMOR,MOUNTED_DETAILS;
 public static final ForgeConfigSpec.BooleanValue TACZ_EXPLOSIONS,CBC_ATTRIBUTION,CBC_SINGLE_HIT,DEBUG,ADVANCED_ARMOR,RICOCHETS,COMPONENT_HITS;
 public static final ForgeConfigSpec.DoubleValue TACZ_SCALE,CBC_DIRECT_SCALE,CBC_BLAST_SCALE;
 public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_OVERRIDES,CBC_DIRECT_OVERRIDES,CBC_BLAST_OVERRIDES,VEHICLE_OVERRIDES;
 static {
  var b=new ForgeConfigSpec.Builder();
  b.comment("Compatibility switches. Native SBW armor, direction, ownership and friendly-fire rules remain active.").push("compatibility");
  TACZ_EXPLOSIONS=b.comment("Use TACZ's configured explosion damage with SBW's native blast exposure/distance formula.").define("taczExplosionFix",true);
  CBC_ATTRIBUTION=b.comment("Attach the CBC projectile/shooter to vehicle damage sources, including shell blasts. No-owner shots use the projectile as attacker to avoid SBW's null-driver rejection.").define("cbcAttributionFix",true);
  CBC_SINGLE_HIT=b.comment("Prevent one CBC cannon projectile damaging the same vehicle repeatedly while it remains inside the hitbox. Other vehicles and the shell explosion remain hittable. Set false for original CBC repeat-hit behavior.").define("cbcOneDirectHitPerVehicle",true);
  DEBUG=b.comment("Log vehicle hit input, exposure and health loss. Enable only while testing.").define("debugDamage",false);b.pop();
  b.comment("Multiply incoming damage BEFORE native SBW armor. 1.0 preserves the original damage amount; 0 disables damage.").push("balance");
  TACZ_SCALE=b.defineInRange("taczExplosionMultiplier",1.0,0.0,1000.0);
  CBC_DIRECT_SCALE=b.defineInRange("cbcDirectMultiplier",1.0,0.0,1000.0);
  CBC_BLAST_SCALE=b.defineInRange("cbcExplosionMultiplier",1.0,0.0,1000.0);
  TACZ_OVERRIDES=b.comment("Exact TACZ gun ID = additional multiplier, e.g. tacz:rpg7=1.0. Multiplies the global factor.").defineListAllowEmpty("taczGunMultipliers",List.of("tacz:rpg7=1.0"),CompatConfig::validEntry);
  CBC_DIRECT_OVERRIDES=b.comment("Exact projectile IDs; applies only to direct impacts. Multiply the global factor.").defineListAllowEmpty("cbcDirectProjectileMultipliers",List.of("createbigcannons:shot=1.0","createbigcannons:ap_shot=1.0","createbigcannons:ap_shell=1.0","createbigcannons:he_shell=1.0"),CompatConfig::validEntry);
  CBC_BLAST_OVERRIDES=b.comment("Exact projectile IDs; applies only to explosions. Multiply the global factor. CBC keeps its radius-based blast formula.").defineListAllowEmpty("cbcExplosionProjectileMultipliers",List.of("createbigcannons:ap_shell=1.0","createbigcannons:he_shell=1.0"),CompatConfig::validEntry);
  VEHICLE_OVERRIDES=b.comment("Exact SBW vehicle ID = additional incoming-damage multiplier. Applies only to compatibility-handled CBC direct/blast and TACZ blast hits; These multipliers do not scale SBW mounted weapons.").defineListAllowEmpty("vehicleMultipliers",List.of(),CompatConfig::validEntry);
  b.pop();
  b.push("armor");
  ADVANCED_ARMOR=b.comment("Use configurable vehicle/ammunition armor profiles instead of native armor for mapped projectile hits. Unmapped hits retain native SBW armor. Restart after edits.").define("advancedArmor",false);
  MOUNTED_ARMOR=b.comment("Apply advanced armor to mapped SBW vehicle-mounted projectile weapons, including their blasts. Requires advancedArmor. Unmapped/energy weapons retain native behavior.").define("mountedWeaponArmor",true);
  RICOCHETS=b.comment("Enable probabilistic glancing-hit rejection. Does not redirect the physical projectile.").define("ricochets",true);
  COMPONENT_HITS=b.comment("Route penetrating rays intersecting SBW engine hitboxes to native engine health.").define("componentHits",true);
  b.pop();
  b.push("impactFeedback");
  IMPACT_SOUNDS=b.comment("Outcome sounds for advanced mapped armor impacts. No extra damage or physical projectile bounce.").define("enabled",true);
  SHOOTER_CUES=b.comment("Also send the outcome cue directly to the shooter, including at long range.").define("shooterFeedback",true);
  IMPACT_INTERVAL=b.comment("Minimum ticks between audible impacts on one vehicle; deduplicated per projectile/vehicle.").defineInRange("minimumIntervalTicks",2,0,100);
  PEN_SOUND=b.define("penetrationSound","minecraft:block.anvil.land",CompatConfig::validSound);
  BLOCK_SOUND=b.define("blockedSound","minecraft:item.shield.block",CompatConfig::validSound);
  RICOCHET_SOUND=b.define("ricochetSound","minecraft:item.trident.hit",CompatConfig::validSound);
  IMPACT_VOLUME=b.defineInRange("volume",0.65,0.0,4.0);
  PEN_PITCH=b.defineInRange("penetrationPitch",1.3,0.5,2.0);
  BLOCK_PITCH=b.defineInRange("blockedPitch",0.8,0.5,2.0);
  RICOCHET_PITCH=b.defineInRange("ricochetPitch",1.8,0.5,2.0);
  b.pop();b.push("inspection");
  MOUNTED_DETAILS=b.comment("Client-side: automatically expand armor/penetration details while riding SBW, so holding the dismount key is unnecessary.").define("mountedDetails",true);
  INSPECTION_RANGE=b.comment("Client-side Jade vehicle inspection range while holding a TACZ gun or riding an SBW vehicle. Mounted weapons use their muzzle ray. Loaded entities only; solid blocks obstruct it. Does not change interaction reach.").defineInRange("vehicleRange",200.0,16.0,256.0);
  b.pop();SPEC=b.build();
 }
 private static boolean validSound(Object value){return value instanceof String s&&net.minecraft.resources.ResourceLocation.tryParse(s)!=null;}
 private static boolean validEntry(Object entry) {
  if(!(entry instanceof String s))return false;
  String[] p=s.split("=",-1);
  if(p.length!=2 || !p[0].trim().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))return false;
  try {double d=Double.parseDouble(p[1].trim());return Double.isFinite(d)&&d>=0&&d<=1000;}catch(NumberFormatException e){return false;}
 }
 public static double factor(List<? extends String> entries,String id) {
  double result=1;
  for(String entry:entries){String[] p=entry.split("=",-1);if(p.length==2&&p[0].trim().equals(id))result=Double.parseDouble(p[1].trim());}
  return result;
 }
}
