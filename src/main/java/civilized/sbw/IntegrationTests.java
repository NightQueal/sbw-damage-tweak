package civilized.sbw;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import java.lang.reflect.Method;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;

@GameTestHolder("civilized_sbw")
@PrefixGameTestTemplate(false)
public final class IntegrationTests {
 static {
  // Test-only: temporary config mutations must not be replayed asynchronously by the file watcher.
  try {com.electronwill.nightconfig.core.file.FileWatcher.defaultInstance().removeWatch(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("civilized-sbw-common.toml"));}
  catch(RuntimeException e){throw new RuntimeException(e);}
  CompatConfig.ADVANCED_ARMOR.set(false);
  CompatConfig.CBC_DIRECT_SCALE.set(1.0);CompatConfig.CBC_BLAST_SCALE.set(1.0);CompatConfig.TACZ_SCALE.set(1.0);
  CompatConfig.CBC_ATTRIBUTION.set(true);CompatConfig.TACZ_EXPLOSIONS.set(true);CompatConfig.CBC_SINGLE_HIT.set(true);
 }

 private static void require(boolean b,String m){if(!b)throw new RuntimeException(m);}
 private static VehicleEntity vehicle(GameTestHelper h,String id) {
  var e=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare",id)).create(h.getLevel());
  VehicleEntity v=(VehicleEntity)e;var pos=h.absolutePos(new BlockPos(2,12,2));v.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);v.setNoGravity(true);h.getLevel().addFreshEntity(v);v.setHealth(250);return v;
 }
 private static AbstractCannonProjectile cannon(GameTestHelper h,String id) {
  return (AbstractCannonProjectile)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("createbigcannons",id)).create(h.getLevel());
 }
 private static DamageSource direct(AbstractCannonProjectile p,Entity target) {
  try {Class<?> cls=p instanceof AbstractBigCannonProjectile?AbstractBigCannonProjectile.class:AbstractCannonProjectile.class;Method m=cls.getDeclaredMethod("getEntityDamage",Entity.class);m.setAccessible(true);return (DamageSource)m.invoke(p,target);}catch(Exception e){throw new RuntimeException(e);}
 }
 private static EntityKineticBullet rocket(GameTestHelper h,VehicleEntity v) {
  EntityKineticBullet p=new EntityKineticBullet(EntityKineticBullet.TYPE,h.getLevel());var shooter=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());shooter.setPos(v.position().add(0,1,-10));p.setOwner(shooter);
  try {var f=EntityKineticBullet.class.getDeclaredField("gunId");f.setAccessible(true);f.set(p,new ResourceLocation("tacz:rpg7"));f=EntityKineticBullet.class.getDeclaredField("ammoId");f.setAccessible(true);f.set(p,new ResourceLocation("tacz:rpg_rocket"));}catch(Exception e){throw new RuntimeException(e);}
  p.setPos(v.position().add(0,2,0));return p;
 }
 private static float taczBlast(GameTestHelper h,VehicleEntity v,EntityKineticBullet p) {
  float before=v.getHealth();com.tacz.guns.util.ExplodeUtil.createExplosion(p.getOwner(),p,120,3,false,false,p.position());return before-v.getHealth();
 }
 @GameTest(template="empty",timeoutTicks=60,batch="taczExplosionCompatibility")
 public static void taczExplosionCompatibility(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{
   var p=rocket(h,v);float exposure=CustomExplosion.getSeenPercentOptimized(h.getLevel(),p.position(),v);
   CompatConfig.TACZ_EXPLOSIONS.set(false);v.setHealth(250);float before=taczBlast(h,v,p);
   CompatConfig.TACZ_EXPLOSIONS.set(true);v.setHealth(250);float after=taczBlast(h,v,p);
   System.out.println("COMPAT_TEST TACZ native="+before+" fixed="+after+" exposure="+exposure);
   require(exposure>0&&after>before&&after>0,"TACZ blast not corrected: "+before+" / "+after);
   CompatConfig.TACZ_SCALE.set(0.0);v.setHealth(250);require(taczBlast(h,v,p)==0,"Zero blast multiplier did not disable damage");CompatConfig.TACZ_SCALE.set(1.0);
   v.discard();h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=60,batch="cbcDirectAttributionAndBalance")
 public static void cbcDirectAttributionAndBalance(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");var p=cannon(h,"shot");p.setPos(v.position().add(0,1,6));
  CompatConfig.CBC_ATTRIBUTION.set(false);var old=direct(p,v);v.hurt(old,30);require(v.getHealth()==250,"Native unattributed CBC shot unexpectedly damaged untouched tank");
  CompatConfig.CBC_ATTRIBUTION.set(true);var source=direct(p,v);require(source.getDirectEntity()==p&&source.getEntity()==p,"CBC ownerless attribution missing");
  float expected=v.getDamageModifier().compute(v,source,30);v.hurt(source,30);float basic=250-v.getHealth();require(Math.abs(basic-expected)<.001&&basic>0,"Native armor not preserved");
  CompatConfig.CBC_DIRECT_SCALE.set(2.0);v.setHealth(250);source=direct(p,v);v.hurt(source,30);float tuned=250-v.getHealth();CompatConfig.CBC_DIRECT_SCALE.set(1.0);
  require(tuned>basic,"CBC pre-armor multiplier had no effect");
  CompatConfig.CBC_DIRECT_OVERRIDES.set(java.util.List.of("createbigcannons:shot=2.0"));v.setHealth(250);v.hurt(direct(p,v),30);float perAmmo=250-v.getHealth();require(Math.abs(perAmmo-tuned)<.001,"Per-projectile tuning failed");CompatConfig.CBC_DIRECT_OVERRIDES.set(java.util.List.of("createbigcannons:shot=1.0"));
  var owner=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());p.setOwner(owner);source=direct(p,v);require(source.getEntity()==owner,"Known CBC owner not retained");require(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)==old.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"CBC armor flag lost");
  System.out.println("COMPAT_TEST CBC solid native=0 fixed="+basic+" multiplier2="+tuned);v.discard();h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=60,batch="cbcShellExplosion")
 public static void cbcShellExplosion(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{
   var p=cannon(h,"he_shell");p.setPos(v.position().add(0,3,0));
   var source=p.indirectArtilleryFire(false);CompatConfig.CBC_ATTRIBUTION.set(false);
   var original=new ShellExplosion(h.getLevel(),p,source,p.getX(),p.getY(),p.getZ(),8,false,Level.ExplosionInteraction.NONE);
   original.explode();float nativeLoss=250-v.getHealth();require(nativeLoss==0,"Native CBC shell attribution assumption failed");
   CompatConfig.CBC_ATTRIBUTION.set(true);v.setHealth(250);
   var fixed=new ShellExplosion(h.getLevel(),p,source,p.getX(),p.getY(),p.getZ(),8,false,Level.ExplosionInteraction.NONE);fixed.explode();float loss=250-v.getHealth();
   require(loss>0,"CBC shell blast remained blocked");
   CompatConfig.CBC_BLAST_SCALE.set(2.0);v.setHealth(250);new ShellExplosion(h.getLevel(),p,source,p.getX(),p.getY(),p.getZ(),8,false,Level.ExplosionInteraction.NONE).explode();float tuned=250-v.getHealth();CompatConfig.CBC_BLAST_SCALE.set(1.0);require(tuned>loss,"CBC blast multiplier had no effect");
   System.out.println("COMPAT_TEST CBC HE native="+nativeLoss+" fixed="+loss+" multiplier2="+tuned);v.discard();h.succeed();
  });
 }
 @GameTest(template="empty",timeoutTicks=80,batch="realCbcFlight")
 public static void realCbcFlight(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{var p=cannon(h,"ap_shot");p.setPos(v.position().add(0,7,0));p.setDeltaMovement(0,-3,0);h.getLevel().addFreshEntity(p);});
  h.runAtTickTime(24,()->{float loss=250-v.getHealth();System.out.println("COMPAT_TEST CBC AP actual flight healthLoss="+loss);require(loss>0&&loss<10,"Actual CBC AP shot flight failed or native armor bypassed: "+loss);v.discard();h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=40,batch="nativeArmorAndScope")
 public static void nativeArmorAndScope(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{var p=cannon(h,"ap_shot");p.setPos(v.position().add(0,1,6));var front=direct(p,v);var scripted=new com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier().reduce(20).multiply(.2f).custom((entity,source,damage)->((VehicleEntity)entity).getSourceAngle(source,.3f)*damage);float a=scripted.compute(v,front,50);p.setPos(v.position().add(0,1,-6));float b=scripted.compute(v,direct(p,v),50);System.out.println("COMPAT_TEST directional raw="+v.getSourceAngle(front,.3f)+" front="+a+" rear="+b);require(Math.abs(a-b)>1,"Directional armor lost: "+a+" / "+b);
  var holder=h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("tacz:bullet")));
  var bullet=new DamageSource(holder,p,p);require(v.getDamageModifier().compute(v,bullet,100)==0,"Native ordinary-bullet immunity changed");
  var nonVehicle=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());require(direct(p,nonVehicle).getDirectEntity()==null,"CBC non-vehicle source changed");
  CompatConfig.VEHICLE_OVERRIDES.set(java.util.List.of("superbwarfare:m_1a_2=0.0"));v.setHealth(250);v.hurt(direct(p,v),50);require(v.getHealth()==250,"Per-vehicle disable failed");CompatConfig.VEHICLE_OVERRIDES.set(java.util.List.of());
  System.out.println("COMPAT_TEST CBC AP directional native armor="+a+" / "+b);v.discard();h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=40,batch="resourceRules")
 public static void resourceRules(GameTestHelper h) {
  var tag=TagKey.create(Registries.ITEM,new ResourceLocation("forge:ingots/tungsten"));
  require(!new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare:tungsten_ingot"))).is(tag)&&new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("northstar:tungsten_ingot"))).is(tag),"Tungsten separation broken");
  var recipes=h.getLevel().getRecipeManager();for(String id:new String[]{"civilized_sbw:pressing/steel_plate","civilized_sbw:pressing/copper_plate","civilized_sbw:compacting/armor_plate","civilized_sbw:vehicle_key"})require(recipes.byKey(new ResourceLocation(id)).isPresent(),"Missing "+id);
  for(String id:new String[]{"civilized_sbw:drilling/scheelite","civilized_sbw:ore_vein_type/galena"})require(recipes.byKey(new ResourceLocation(id)).isEmpty(),"Disabled recipe active "+id);
  for(String id:new String[]{"superbwarfare:netherite_hammer","superbwarfare:dps_generator_deployer"}) {boolean disabled=id.endsWith("hammer")?CompatConfig.DISABLE_HAMMER.get():CompatConfig.DISABLE_DPS.get();require(recipes.byKey(new ResourceLocation(id)).isPresent()!=disabled,"Recipe toggle failed "+id);}
  var dps=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare:dps_generator")).create(h.getLevel());require(h.getLevel().addFreshEntity(dps)!=CompatConfig.DISABLE_DPS.get(),"DPS spawn toggle failed");dps.discard();h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=40,batch="equipmentToggles")
 public static void equipmentToggles(GameTestHelper h) {
  boolean hammerWas=CompatConfig.DISABLE_HAMMER.get(),dpsWas=CompatConfig.DISABLE_DPS.get();
  try {
   CompatConfig.DISABLE_DPS.set(false);
   var entity=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare:dps_generator")).create(h.getLevel());
   require(h.getLevel().addFreshEntity(entity),"Enabled DPS generator blocked");entity.discard();
   for(String id:new String[]{"superbwarfare:netherite_hammer","superbwarfare:dps_generator_deployer"}) {
    var toggle=id.endsWith("hammer")?CompatConfig.DISABLE_HAMMER:CompatConfig.DISABLE_DPS;
    toggle.set(false);
    var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,10,0,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id))));
    require(h.getLevel().addFreshEntity(item),"Enabled item blocked: "+id);item.discard();
    toggle.set(true);
    var blocked=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),0,10,0,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id))));
    require(!h.getLevel().addFreshEntity(blocked),"Disabled item allowed: "+id);
   }
  } finally {CompatConfig.DISABLE_HAMMER.set(hammerWas);CompatConfig.DISABLE_DPS.set(dpsWas);}
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=40,batch="armorMathAndGeometry")
 public static void armorMathAndGeometry(GameTestHelper h) {
  var ap=ArmorProfiles.ammo("entity:createbigcannons:ap_shot");
  var flat=ArmorModel.resolve(ap,50,100,1,0,3,.99,false,false);
  var angled=ArmorModel.resolve(ap,50,100,1,60,3,.99,false,false);
  require(flat.penetrated()&&flat.damage()>angled.damage(),"Sloped armor not stronger");
  require(!ArmorModel.resolve(ap,50,100,1,0,1,.99,false,false).penetrated(),"Kinetic velocity loss not applied");
  require(ArmorModel.resolve(ap,50,100,1,85,3,0,false,true).ricochet(),"Grazing AP did not ricochet");
  require(!ArmorModel.resolve(ap,50,5,1,85,3,0,false,true).ricochet(),"Thin plate overmatch failed");
  var heat=ArmorProfiles.ammo("gun:tacz:rpg7");
  var slow=ArmorModel.resolve(heat,50,100,1,0,.1,.99,false,false);
  var fast=ArmorModel.resolve(heat,50,100,1,0,10,.99,false,false);
  require(Math.abs(slow.damage()-fast.damage())<.001,"HEAT incorrectly depends on speed");
  require(ArmorModel.resolve(heat,50,100,2,0,10,.99,false,false).damage()<fast.damage(),"Chemical armor multiplier ignored");
  require(!ArmorModel.resolve(heat,50,100,1,89,3,0,true,true).ricochet(),"Area blast ricocheted");
  var box=new com.atsuishio.superbwarfare.tools.OBB(new org.joml.Vector3d(),new org.joml.Vector3d(1,1,1),new org.joml.Quaterniond().rotateY(Math.PI/4),com.atsuishio.superbwarfare.tools.OBB.Part.EMPTY);
  var hit=ArmorModel.intersect(box,new net.minecraft.world.phys.Vec3(0,0,10),new net.minecraft.world.phys.Vec3(0,0,-1),20);
  require(hit!=null&&Math.abs(hit.normal().length()-1)<.001&&hit.normal().z>.5,"Rotated OBB normal failed");
  require(ArmorModel.intersect(box,new net.minecraft.world.phys.Vec3(0,10,10),new net.minecraft.world.phys.Vec3(0,0,-1),20)==null,"OBB miss became a hit");
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=60,batch="vehicleArmorAndEngine")
 public static void vehicleArmorAndEngine(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{
   boolean old=CompatConfig.ADVANCED_ARMOR.get();
   try {
    CompatConfig.ADVANCED_ARMOR.set(true);
    var engine=v.getOBBs().stream().filter(b->b.part==com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE).findFirst().orElseThrow();
    var p=cannon(h,"ap_shot");p.setPos(engine.center.x,engine.center.y,engine.center.z-30);p.setDeltaMovement(0,0,3);
    float engineBefore=v.getMainEngineHealth();v.hurt(direct(p,v),50);
    require(v.getHealth()<250&&v.getMainEngineHealth()<engineBefore,"Penetrating rear shot did not damage native engine");
    float rearLoss=250-v.getHealth();v.setHealth(250);p.setPos(engine.center.x,engine.center.y,engine.center.z+30);p.setDeltaMovement(0,0,-3);v.hurt(direct(p,v),50);
    require(250-v.getHealth()<rearLoss,"Heavy front armor not stronger than rear");
    CompatConfig.ADVANCED_ARMOR.set(false);v.setHealth(250);v.hurt(direct(p,v),50);require(v.getHealth()<250,"Disabling advanced armor failed to restore native path");
    System.out.println("ARMOR_TEST M1A2 rear loss="+rearLoss+" engine health="+v.getMainEngineHealth());
   } finally {CompatConfig.ADVANCED_ARMOR.set(old);v.discard();}
   h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=60,batch="taczArmorAndShapedCharge")
 public static void taczArmorAndShapedCharge(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");var light=vehicle(h,"truck");h.runAtTickTime(8,()->{
   boolean old=CompatConfig.ADVANCED_ARMOR.get();
   try {
    CompatConfig.ADVANCED_ARMOR.set(true);
    var p=rocket(h,v);p.setDeltaMovement(0,0,-1);
    var gun=EntityKineticBullet.class.getDeclaredField("gunId");gun.setAccessible(true);gun.set(p,new ResourceLocation("test:rifle"));
    var ammo=EntityKineticBullet.class.getDeclaredField("ammoId");ammo.setAccessible(true);ammo.set(p,new ResourceLocation("tacz:50bmg"));
    var holder=h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("tacz:bullet")));
    p.setPos(light.getBoundingBox().getCenter().add(0,0,30));var src=new civilized.sbw.AttributedDamageSource(new DamageSource(holder,p,p),p,1);
    light.hurt(src,30);require(light.getHealth()<250,"FMJ could not damage unarmored truck");
    p.setPos(v.getBoundingBox().getCenter().add(0,0,30));v.hurt(src,30);require(v.getHealth()==250,"FMJ penetrated MBT front");
    ammo.set(p,new ResourceLocation("test:unknown"));require(ArmorModel.compute(v,src,30)==null,"Unknown ammo did not retain native armor");
    gun.set(p,new ResourceLocation("tacz:rpg7"));ammo.set(p,new ResourceLocation("tacz:rpg_rocket"));p.setDeltaMovement(0,0,1);
    var engine=v.getOBBs().stream().filter(b->b.part==com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE).findFirst().orElseThrow();
    var start=new net.minecraft.world.phys.Vec3(engine.center.x,engine.center.y,engine.center.z-30);
    var hit=ArmorModel.trace(v,start,new net.minecraft.world.phys.Vec3(0,0,1),60).get(0);
    p.setPos(hit.point());var explosion=new civilized.sbw.AttributedDamageSource(new DamageSource(holder,p,p),p,1,true,hit.point());
    float before=v.getMainEngineHealth();v.hurt(explosion,120);
    require(v.getHealth()<250&&v.getMainEngineHealth()<before,"Impact HEAT jet did not penetrate rear and hit engine");
    System.out.println("ARMOR_TEST TACZ FMJ truck loss="+(250-light.getHealth())+" RPG rear loss="+(250-v.getHealth()));
   } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
   finally {CompatConfig.ADVANCED_ARMOR.set(old);v.discard();light.discard();}
   h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=80,batch="advancedCbcFlight")
 public static void advancedCbcFlight(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");boolean old=CompatConfig.ADVANCED_ARMOR.get();
  h.runAtTickTime(30,()->{CompatConfig.ADVANCED_ARMOR.set(true);var p=cannon(h,"ap_shot");p.setPos(v.position().add(0,7,0));p.setDeltaMovement(0,-3,0);h.getLevel().addFreshEntity(p);});
  h.runAtTickTime(48,()->{
   try {float loss=250-v.getHealth();require(loss>10&&loss<100,"Advanced CBC projectile flight failed or repeated damage: "+loss);System.out.println("ARMOR_TEST CBC AP actual advanced flight healthLoss="+loss);}
   finally {CompatConfig.ADVANCED_ARMOR.set(old);v.discard();}
   h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=40,batch="armorScanReport")
 public static void armorScanReport(GameTestHelper h) {
  try {
   var commands=h.getLevel().getServer().getCommands().getDispatcher();
   var source=h.getLevel().getServer().createCommandSourceStack().withLevel(h.getLevel()).withPermission(2).withSuppressedOutput();
   require(commands.execute("sbwcompat armor scan",source)==1,"Registered armor scan command failed");
   require(java.nio.file.Files.exists(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("civilized-sbw-armor-scan.json")),"Scan report file missing");
   boolean blocked=false;try {commands.execute("sbwcompat armor scan",source.withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){blocked=true;}
   require(blocked,"Non-operator could execute armor scan");
  } catch(Exception e){throw new RuntimeException(e);}
  var report=ArmorScan.scan(h.getLevel());
  var guns=(java.util.List<?>)report.get("guns");require(!guns.isEmpty(),"TACZ gun registry not scanned");
  require(guns.stream().anyMatch(x->((java.util.Map<?,?>)x).get("id").equals("tacz:rpg7")&&((java.util.Map<?,?>)x).get("effectiveProfileKey").equals("gun:tacz:rpg7")),"Gun override mapping missing from scan");
  var vehicles=(java.util.List<?>)report.get("vehicles");require(vehicles.stream().anyMatch(x->((java.util.Map<?,?>)x).get("id").equals("superbwarfare:m_1a_2")),"Vehicle registry not scanned");
  var v=vehicle(h,"m_1a_2");var p=cannon(h,"ap_shot");h.runAtTickTime(8,()->{
   boolean old=CompatConfig.ADVANCED_ARMOR.get();try {
    CompatConfig.ADVANCED_ARMOR.set(true);var engine=v.getOBBs().stream().filter(b->b.part==com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE).findFirst().orElseThrow();p.setPos(engine.center.x,engine.center.y,engine.center.z-30);p.setDeltaMovement(0,0,3);
    float first=v.getDamageModifier().compute(v,direct(p,v),20),second=v.getDamageModifier().compute(v,direct(p,v),40);
    require(first>0&&Math.abs(second-first*2)<.01,"Incoming gun/shot damage not retained by armor");
   } finally {CompatConfig.ADVANCED_ARMOR.set(old);v.discard();}h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=40,batch="attachmentPenetration")
 public static void attachmentPenetration(GameTestHelper h) {
  require(StatText.range(150,Float.MAX_VALUE).equals("150m+"),"Unlimited range leaked float sentinel");
  require(StatText.range(75,150).equals("75-150m"),"Finite damage range incorrect");
  require(StatText.change(1.35).equals("+35%")&&StatText.change(.45).equals("-55%"),"Attachment percentage formatting incorrect");
  var base=ArmorProfiles.ammo("ammo:tacz:50bmg");require(base!=null,"Missing .50 profile");
  var fmj=ArmorProfiles.modified(base,"tacz:ammo_mod_fmj");var hp=ArmorProfiles.modified(base,"tacz:ammo_mod_hp");var he=ArmorProfiles.modified(base,"tacz:ammo_mod_he");
  require(Math.abs(fmj.penetration-33.75)<.001&&Math.abs(hp.penetration-11.25)<.001,"Attachment penetration incorrect");
  require(base.penetration==25&&base.normalization==0,"Attachment mutated shared base profile");
  require(ArmorModel.resolve(fmj,55.8,33,1,0,0,1,false,true).damage()>0,"FMJ cannot penetrate BMP front");
  require(ArmorModel.resolve(hp,80.6,16,1,0,0,1,false,true).damage()==0,"HP damage bypassed blocked plate");
  require(he.type.equals("HE")&&he.blastPenetration==6,"Explosive attachment not mapped separately");
  var heat=ArmorProfiles.ammo("gun:tacz:rpg7");require(ArmorProfiles.modified(heat,"tacz:ammo_mod_hp")==heat,"Attachment changed rocket shaped charge");
  require(ArmorProfiles.modified(base,"test:unknown")==base,"Unknown attachment changed base profile");
  var copy=new com.google.gson.Gson().fromJson(ArmorProfiles.json(),ArmorProfiles.Data.class);
  require(ArmorProfiles.modified(copy.ammunition.get("ammo:tacz:50bmg"),"tacz:ammo_mod_hp",copy).penetration==hp.penetration,"Synced tooltip profile differs from server");
  var original=new ProfileSync.Snapshot(ArmorProfiles.json(),true,false,true);
  var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
  try {original.write(buffer);require(ProfileSync.Snapshot.read(buffer).equals(original),"Server tooltip settings did not round-trip");}finally{buffer.release();}
  h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=40,batch="attachmentCapturedAtFire")
 public static void attachmentCapturedAtFire(GameTestHelper h) {
  var id=new ResourceLocation("tacz:m107");var index=com.tacz.guns.api.TimelessAPI.getCommonGunIndex(id).orElseThrow();
  var stack=com.tacz.guns.api.item.builder.GunItemBuilder.create().setId(id).setFireMode(com.tacz.guns.api.item.gun.FireMode.SEMI).putAttachment(com.tacz.guns.api.item.attachment.AttachmentType.EXTENDED_MAG,new ResourceLocation("tacz:ammo_mod_fmj")).build();
  var shooter=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());shooter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
  var attachmentIndex=com.tacz.guns.api.TimelessAPI.getCommonAttachmentIndex(new ResourceLocation("tacz:ammo_mod_fmj")).orElseThrow();
  var damageModifier=attachmentIndex.getData().getModifier().get("damage");
  require(damageModifier!=null&&damageModifier.getValue() instanceof com.tacz.guns.resource.pojo.data.attachment.Modifier,"Tooltip cannot read attachment damage modifier");
  var cache=new com.tacz.guns.resource.modifier.AttachmentCacheProperty();cache.eval(stack,index.getGunData());
  com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(shooter).updateCacheProperty(cache);
  var p=new EntityKineticBullet(h.getLevel(),shooter,stack,index.getGunData().getAmmoId(),id,new ResourceLocation("tacz:default"),false,index.getGunData(),index.getBulletData());
  require(p.getPersistentData().getString("civilized_sbw_ammo_attachment").equals("tacz:ammo_mod_fmj"),"Actual TACZ constructor did not capture attachment");
  com.tacz.guns.api.item.IGun.getIGunOrNull(stack).unloadAttachment(stack,com.tacz.guns.api.item.attachment.AttachmentType.EXTENDED_MAG);
  require(p.getPersistentData().getString("civilized_sbw_ammo_attachment").equals("tacz:ammo_mod_fmj"),"In-flight ammunition changed after gun modification");
  require(Math.abs(p.getDamage(p.position())-45)<.01,"TACZ FMJ damage changed/doubled: "+p.getDamage(p.position()));
  shooter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.succeed();
 }

 @GameTest(template="empty",timeoutTicks=40,batch="vehicleInspection")
 public static void vehicleInspection(GameTestHelper h) {
  var ammo=ArmorProfiles.ammo("ammo:tacz:50bmg");
  var slope=VehicleStats.preview(ammo,2,1,70,true);
  require(Math.abs(slope.penetrationChance()-100.0/3)<.001,"Ricochet probability not included in inspection");
  require(VehicleStats.preview(ammo,75,1,0,true).penetrationChance()==0,"Inspection shows penetration through blocked armor");
  require(VehicleStats.preview(ammo,1,1,85,true).penetrationChance()==100,"Overmatch inspection incorrect");
  var tank=ArmorProfiles.vehicle("superbwarfare:m_1a_2");require(VehicleStats.armor(tank,false).size()==1&&VehicleStats.armor(tank,true).size()==6,"Armor detail rows incorrect");
  var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation("superbwarfare:m_1a_2"));
  var container=com.atsuishio.superbwarfare.item.container.ContainerBlockItem.createInstance(type);
  require(VehicleStats.itemVehicle(container).equals("superbwarfare:m_1a_2"),"Actual SBW vehicle container not recognized");
  var item=new ItemStack(net.minecraft.world.item.Items.STONE);item.getOrCreateTag().putString("EntityType","superbwarfare:m_1a_2");require(VehicleStats.itemVehicle(item).equals("superbwarfare:m_1a_2"),"Container vehicle ID missing");
  var data=new net.minecraft.nbt.CompoundTag();data.putString("EntityType","superbwarfare:truck");item.setTag(new net.minecraft.nbt.CompoundTag());item.getOrCreateTag().put("BlockEntityTag",data);require(VehicleStats.itemVehicle(item).equals("superbwarfare:truck"),"Deployer vehicle ID missing");
  h.succeed();
 }

 @GameTest(template="empty",timeoutTicks=40,batch="armorImpactFeedback")
 public static void armorImpactFeedback(GameTestHelper h) {
  var ammo=ArmorProfiles.ammo("ammo:tacz:50bmg");
  var blocked=ArmorModel.resolve(ammo,30,75,1,0,0,1,false,true);
  var penetrated=ArmorModel.resolve(ammo,30,2,1,0,0,1,false,true);
  var ricochet=ArmorModel.resolve(ammo,30,2,1,75,0,0,false,true);
  require(ImpactFeedback.outcome(blocked)==ImpactFeedback.Outcome.BLOCKED&&ImpactFeedback.outcome(penetrated)==ImpactFeedback.Outcome.PENETRATION&&ImpactFeedback.outcome(ricochet)==ImpactFeedback.Outcome.RICOCHET,"Impact outcome classification incorrect");
  var target=vehicle(h,"truck");var shot=rocket(h,target);
  require(ImpactFeedback.reserve(target,shot,100,2),"First cue blocked");
  require(!ImpactFeedback.reserve(target,shot,103,2),"Same projectile played duplicate cue");
  require(!ImpactFeedback.reserve(target,rocket(h,target),101,2),"Automatic fire cue rate limit failed");
  require(ImpactFeedback.reserve(target,rocket(h,target),102,2),"Cue rate limit did not expire");
  require(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(CompatConfig.PEN_SOUND.get()))&&net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(CompatConfig.BLOCK_SOUND.get()))&&net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation(CompatConfig.RICOCHET_SOUND.get())),"Default cue sound missing");
  var p=rocket(h,target);CompatConfig.IMPACT_SOUNDS.set(false);ImpactFeedback.emit(target,p,target.position(),penetrated);
  require(!p.getPersistentData().contains("civilized_sbw_cue_"+target.getUUID()),"Disabled sound still emitted");CompatConfig.IMPACT_SOUNDS.set(true);
  target.getPersistentData().remove("civilized_sbw_last_cue");ImpactFeedback.emit(target,p,target.position(),penetrated);
  require(p.getPersistentData().contains("civilized_sbw_cue_"+target.getUUID()),"Actual sound emission was not reserved");target.discard();h.succeed();
 }

 @GameTest(template="empty",timeoutTicks=80,batch="longRangeInspectionAndTuning")
 public static void longRangeInspectionAndTuning(GameTestHelper h) {
  require(ArmorProfiles.ammo("gun:maxstuff:excaliber").blastPenetration==15,"Excaliber blast profile not migrated");
  require(ArmorProfiles.ammo("gun:tacz:uzi").penetration>ArmorProfiles.ammo("gun:tacz:glock_17").penetration,"Gun speed tuning not applied");
  require(ArmorProfiles.ammo("gun:tacz:rpg7").penetration==300,"RPG override changed unexpectedly");
  var target=vehicle(h,"m_1a_2");target.setPos(target.getX(),200,target.getZ());h.runAtTickTime(8,()->{
   var box=target.getOBBs().stream().filter(b->b.part!=com.atsuishio.superbwarfare.tools.OBB.Part.COLLISION).findFirst().orElseThrow();
   var center=new net.minecraft.world.phys.Vec3(box.center.x,box.center.y,box.center.z);
   var direction=new net.minecraft.world.phys.Vec3(0,0,1);var start=center.subtract(direction.scale(150));
   var observer=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());observer.setPos(start);
   var hit=VehicleInspectionTarget.trace(h.getLevel(),observer,start,direction,200);
   require(hit!=null&&hit.getEntity()==target,"Vehicle not inspected at 150 blocks");
   require(VehicleInspectionTarget.trace(h.getLevel(),observer,start,direction,100)==null,"Inspection exceeded configured range");
   var wall=net.minecraft.core.BlockPos.containing(center.subtract(direction.scale(20)));h.getLevel().setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
   require(VehicleInspectionTarget.trace(h.getLevel(),observer,start,direction,200)==null,"Vehicle inspected through solid block");
   h.getLevel().removeBlock(wall,false);target.discard();h.succeed();
  });
 }

}


