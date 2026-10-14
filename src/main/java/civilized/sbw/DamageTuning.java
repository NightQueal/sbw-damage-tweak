package civilized.sbw;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.tacz.guns.entity.EntityKineticBullet;
import java.util.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Input damage multipliers only: native headshots, falloff, AP split and defenses remain intact. */
public final class DamageTuning {
 public static final String SBW_WEAPON_TAG="civilized_sbw_damage_weapon";
 public record Weapon(String family,String id) {}
 private record Impact(Weapon weapon,boolean blast) {}
 // CBC's native damage sources omit projectile attribution. Weak metadata preserves the
 // original source object, ownership, subtype and flags without keeping entities alive.
 private static final Map<DamageSource,Impact> SOURCES=Collections.synchronizedMap(new WeakHashMap<>());
 public static void capture(DamageSource source,Entity projectile,boolean blast) {
  Weapon weapon=weapon(projectile);if(weapon!=null)SOURCES.put(source,new Impact(weapon,blast));
 }
 public record Values(double multiplier,Map<String,Double> weapons) {}
 public static final class Rule {
  public final ForgeConfigSpec.DoubleValue multiplier;
  public final ForgeConfigSpec.ConfigValue<List<? extends String>> weapons;
  Rule(ForgeConfigSpec.Builder b) {
   multiplier=b.comment("Input damage factor: 1 preserves damage, 0 disables this damage category.").defineInRange("multiplier",1.0,0.0,1000.0);
   weapons=b.comment("Exact weapon ID = additional factor, e.g. tacz:m95=0.5. Last duplicate wins. SBW IDs come from armor scan damageTuningId; uncaptured SBW shots and CBC use projectile entity IDs.").defineListAllowEmpty("weaponMultipliers",List.of(),DamageTuning::valid);
  }
  Values values(){Map<String,Double> map=new LinkedHashMap<>();for(String s:weapons.get()){String[] p=s.split("=",-1);map.put(p[0].trim(),Double.parseDouble(p[1].trim()));}return new Values(multiplier.get(),map);}
 }
 public static final Map<String,Rule> RULES=new LinkedHashMap<>();
 public static void define(ForgeConfigSpec.Builder b) {
  b.comment("Additional damage controls for TACZ, SBW and CBC. Applied once before target defenses. All defaults preserve existing damage. Vehicle factors multiply the existing balance settings; player/mob factors are independent of them. Explosion radius, block destruction and knockback are unchanged.").push("damageTuning");
  for(String family:List.of("tacz","sbw","cbc")) {
   b.push(family);
   for(String target:List.of("vehicles","players","mobs")) {
    b.comment(target.equals("mobs")?"All non-player living entities.":"Damage received by "+target+".").push(target);
    for(String kind:List.of("direct","explosion")){b.push(kind);RULES.put(key(family,target,kind.equals("explosion")),new Rule(b));b.pop();}
    b.pop();
   }b.pop();
  }b.pop();
 }
 static boolean valid(Object entry) {
  if(!(entry instanceof String s))return false;String[] p=s.split("=",-1);
  if(p.length!=2||p[0].trim().isEmpty()||p[0].trim().length()>256)return false;
  try{double v=Double.parseDouble(p[1].trim());return Double.isFinite(v)&&v>=0&&v<=1000;}catch(NumberFormatException e){return false;}
 }
 static String key(String family,String target,boolean blast){return family+"."+target+"."+(blast?"explosion":"direct");}
 public static Rule rule(String family,String target,boolean blast){return RULES.get(key(family,target,blast));}
 public static Weapon weapon(Entity projectile) {
  if(projectile==null)return null;
  if(projectile instanceof EntityKineticBullet bullet)return new Weapon("tacz",String.valueOf(bullet.getGunId()));
  String gun=projectile.getPersistentData().getString(SBW_WEAPON_TAG);
  if(!gun.isEmpty())return new Weapon("sbw",gun);
  String entityId=DamageCompatibility.id(projectile);
  if(entityId.startsWith("superbwarfare:")) {
   return new Weapon("sbw",entityId);
  }
  return entityId.startsWith("createbigcannons:")?new Weapon("cbc",entityId):null;
 }
 public static boolean blast(DamageSource source){Impact impact=SOURCES.get(source);return impact!=null?impact.blast():source instanceof AttributedDamageSource a?a.blast:source.is(DamageTypeTags.IS_EXPLOSION);}
 public static double factor(Entity target,DamageSource source) {
  Impact impact=SOURCES.get(source);Weapon w=impact==null?weapon(source.getDirectEntity()):impact.weapon();if(w==null)return 1;
  String category=target instanceof VehicleEntity?"vehicles":target instanceof Player?"players":"mobs";
  return factor(w,category,blast(source));
 }
 public static double factor(Weapon weapon,String target,boolean blast) {
  Rule rule=rule(weapon.family(),target,blast);if(rule==null)return 1;
  return rule.multiplier.get()*CompatConfig.factor(rule.weapons.get(),weapon.id());
 }
 public static float scale(float input,double factor){return input<=0?input:(float)Math.min(Float.MAX_VALUE,input*factor);}
 public static void livingHurt(LivingHurtEvent event) {
  if(event.getEntity().level().isClientSide)return;
  double multiplier=factor(event.getEntity(),event.getSource());
  if(multiplier==1)return;
  event.setAmount(scale(event.getAmount(),multiplier));
  if(multiplier==0)event.setCanceled(true);
 }
 public static Map<String,Values> snapshot(){Map<String,Values> result=new LinkedHashMap<>();RULES.forEach((k,v)->result.put(k,v.values()));return result;}
 public static double previewFactor(Map<String,Values> values,Weapon weapon,String target,boolean blast) {
  if(weapon==null)return 1;Values v=values.get(key(weapon.family(),target,blast));return v==null?1:v.multiplier()*v.weapons().getOrDefault(weapon.id(),1.0);
 }
}
