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
 private static void resetDamageTuning(){DamageTuning.RULES.values().forEach(r->{r.multiplier.set(1.0);r.weapons.set(java.util.List.of());});}
 private static net.minecraft.world.entity.animal.Pig damagePig(GameTestHelper h) {
  var pig=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());var pos=h.absolutePos(new BlockPos(3,12,3));pig.setPos(pos.getX(),pos.getY(),pos.getZ());pig.setNoAi(true);pig.setNoGravity(true);pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);pig.setHealth(100);h.getLevel().addFreshEntity(pig);return pig;
 }
 private static float hurtLiving(net.minecraft.world.entity.LivingEntity target,DamageSource source,float damage) {
  target.invulnerableTime=0;target.setHealth(target.getMaxHealth());float before=target.getHealth();target.hurt(source,damage);return before-target.getHealth();
 }
 @GameTest(template="empty",timeoutTicks=60,batch="damageTuningLiving")
 public static void damageTuningLiving(GameTestHelper h) {
  resetDamageTuning();var pig=damagePig(h);var tank=vehicle(h,"m_1a_2");var bullet=rocket(h,tank);bullet.setPos(tank.getBoundingBox().getCenter().add(0,0,30));bullet.setDeltaMovement(0,0,-1);var level=h.getLevel();
  var normal=com.tacz.guns.init.ModDamageTypes.Sources.bullet(level.registryAccess(),bullet,bullet.getOwner(),false);
  var ap=com.tacz.guns.init.ModDamageTypes.Sources.bullet(level.registryAccess(),bullet,bullet.getOwner(),true);
  var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DamageTest"));player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(pig.position().add(8,0,0));
  boolean oldPvp=level.getServer().isPvpAllowed();level.getServer().setPvpAllowed(true);
  try {
   try{var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(player,0);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
   require(hurtLiving(pig,normal,8)==8,"Default TACZ living damage changed");
   var mob=DamageTuning.rule("tacz","mobs",false);mob.multiplier.set(.5);mob.weapons.set(java.util.List.of("tacz:rpg7=0.5"));
   require(hurtLiving(pig,normal,8)==2,"Global/per-gun mob multiplier missing or applied twice");
   require(hurtLiving(pig,ap,8)==2,"Armor-piercing portion not scaled equally");
   float playerBase=hurtLiving(player,normal,8);require(playerBase>0,"Player baseline missing");DamageTuning.rule("tacz","players",false).multiplier.set(.25);
   float playerTuned=hurtLiving(player,normal,8);require(Math.abs(playerTuned-playerBase*.25)<.01,"Player factor did not apply independently: "+playerBase+" / "+playerTuned);
   require(DamageTuning.factor(tank,normal)==1,"Living settings leaked into vehicles");
   require(hurtLiving(pig,level.damageSources().generic(),8)==8,"Unrelated generic damage changed");
   mob.multiplier.set(0.0);require(hurtLiving(pig,normal,8)==0,"Zero TACZ multiplier still damaged mob");
   var sbw=com.atsuishio.superbwarfare.init.ModEntities.PROJECTILE.get().create(level);
   var gun=com.atsuishio.superbwarfare.data.gun.GunData.from(new ItemStack(com.atsuishio.superbwarfare.init.ModItems.RPG.get()));MountedWeapons.capture(sbw,gun);
   require(DamageTuning.weapon(sbw).id().equals(gun.id),"Handheld SBW weapon ID was not frozen at firing");
   var sbwSource=com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireAbsoluteDamage(level.registryAccess(),sbw,bullet.getOwner());
   DamageTuning.rule("sbw","mobs",false).multiplier.set(0.0);pig.setHealth(100);pig.invulnerableTime=0;
   com.atsuishio.superbwarfare.tools.DamageHandler.doDamage(pig,sbwSource,8);
   require(pig.getHealth()==100,"SBW forced-damage fallback bypassed zero multiplier");
   var cbc=cannon(h,"shot");DamageTuning.rule("cbc","mobs",false).multiplier.set(.5);
   require(hurtLiving(pig,direct(cbc,pig),8)==4,"CBC living input tuning missing");
   System.out.println("COMPAT_TEST DAMAGE_TUNING living default=8 tuned=2 AP=2 player="+playerTuned+" zero=0 SBW force=0 CBC=4");h.succeed();
  }finally{level.getServer().setPvpAllowed(oldPvp);resetDamageTuning();pig.discard();tank.discard();}
 }
 @GameTest(template="empty",timeoutTicks=60,batch="damageTuningVehicle")
 public static void damageTuningVehicle(GameTestHelper h) {
  resetDamageTuning();CompatConfig.ADVANCED_ARMOR.set(true);var tank=vehicle(h,"truck");
  h.runAtTickTime(8,()->{try {
   var bullet=rocket(h,tank);bullet.setPos(tank.getBoundingBox().getCenter().add(0,0,30));bullet.setDeltaMovement(0,0,-1);var level=h.getLevel();
   var source=com.tacz.guns.init.ModDamageTypes.Sources.bullet(level.registryAccess(),bullet,bullet.getOwner(),false);
   var ap=com.tacz.guns.init.ModDamageTypes.Sources.bullet(level.registryAccess(),bullet,bullet.getOwner(),true);
   tank.setHealth(250);tank.hurt(source,30);float baseline=250-tank.getHealth();require(baseline>0,"Vehicle baseline failed compute="+tank.getDamageModifier().compute(tank,source,30)+" owner="+source.getEntity()+" immune="+source.is(com.atsuishio.superbwarfare.init.ModTags.DamageTypes.VEHICLE_IMMUNE)+" health="+tank.getHealth());
   var rule=DamageTuning.rule("tacz","vehicles",false);rule.multiplier.set(2.0);rule.weapons.set(java.util.List.of("tacz:rpg7=0.75"));
   float expected=tank.getDamageModifier().compute(tank,source,45);tank.setHealth(250);tank.hurt(source,30);float tuned=250-tank.getHealth();require(Math.abs(tuned-expected)<.01,"Vehicle tuning missing/doubled: "+tuned+" / "+expected);
   rule.multiplier.set(1.0);rule.weapons.set(java.util.List.of());tank.setHealth(250);tank.hurt(ap,30);float apBase=250-tank.getHealth();require(apBase>0,"Vehicle AP baseline failed");
   rule.multiplier.set(.5);expected=tank.getDamageModifier().compute(tank,ap,15);tank.setHealth(250);tank.hurt(ap,30);require(Math.abs(250-tank.getHealth()-expected)<.01,"Vehicle AP bypass escaped scaling");
   rule.multiplier.set(0.0);tank.setHealth(250);tank.hurt(source,30);tank.hurt(ap,30);require(tank.getHealth()==250,"Zero direct vehicle multiplier failed");
   rule.multiplier.set(1.0);bullet.setPos(tank.position().add(0,2,0));var blast=DamageTuning.rule("tacz","vehicles",true);tank.setHealth(250);float blastBase=taczBlast(h,tank,bullet);require(blastBase>0,"Vehicle blast baseline failed");
   blast.multiplier.set(.5);blast.weapons.set(java.util.List.of("tacz:rpg7=0.5"));tank.setHealth(250);require(Math.abs(taczBlast(h,tank,bullet)-blastBase*.25)<.01,"Vehicle blast tuning missing/doubled");
   var sbw=com.atsuishio.superbwarfare.init.ModEntities.PROJECTILE.get().create(level);var sbwSource=com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireAbsoluteDamage(level.registryAccess(),sbw,bullet.getOwner());
   DamageTuning.rule("sbw","vehicles",false).multiplier.set(0.0);tank.setHealth(250);tank.hurt(sbwSource,30);require(tank.getHealth()==250,"SBW vehicle bypass damage escaped tuning");
   CompatConfig.ADVANCED_ARMOR.set(false);var cbc=cannon(h,"shot");var cbcSource=direct(cbc,tank);tank.setHealth(250);tank.hurt(cbcSource,30);float cbcBase=250-tank.getHealth();require(cbcBase>0,"CBC baseline failed");
   DamageTuning.rule("cbc","vehicles",false).multiplier.set(2.0);expected=tank.getDamageModifier().compute(tank,cbcSource,60);tank.setHealth(250);tank.hurt(cbcSource,30);require(Math.abs(250-tank.getHealth()-expected)<.01,"CBC new tuning failed actual="+(250-tank.getHealth())+" expected="+expected);
   var map=new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(DamageTuning.snapshot()),new com.google.gson.reflect.TypeToken<java.util.Map<String,DamageTuning.Values>>(){}.getType());
   require(DamageTuning.previewFactor((java.util.Map<String,DamageTuning.Values>)map,new DamageTuning.Weapon("tacz","tacz:rpg7"),"vehicles",true)==.25,"Preview serialization failed");
   System.out.println("COMPAT_TEST DAMAGE_TUNING vehicle baseline="+baseline+" tuned="+tuned+" AP baseline="+apBase+" blast baseline="+blastBase);h.succeed();
  }finally{resetDamageTuning();CompatConfig.ADVANCED_ARMOR.set(false);tank.discard();}});
 }
 @GameTest(template="empty",timeoutTicks=60,batch="damageTuningExplosionLiving")
 public static void damageTuningExplosionLiving(GameTestHelper h) {
  resetDamageTuning();var pig=damagePig(h);var tank=vehicle(h,"m_1a_2");tank.setPos(pig.position().add(20,0,0));var bullet=rocket(h,tank);bullet.setPos(pig.position().add(0,.5,0));
  h.runAtTickTime(8,()->{try {
   java.util.function.Supplier<Float> blast=()->{pig.setHealth(100);pig.invulnerableTime=0;com.tacz.guns.util.ExplodeUtil.createExplosion(bullet.getOwner(),bullet,8,3,false,false,bullet.position());return 100-pig.getHealth();};
   float baseline=blast.get();require(baseline>0,"Living blast baseline failed");
   DamageTuning.rule("tacz","mobs",false).multiplier.set(0.0);require(Math.abs(blast.get()-baseline)<.01,"Direct setting affected explosion");
   var rule=DamageTuning.rule("tacz","mobs",true);rule.multiplier.set(.5);rule.weapons.set(java.util.List.of("tacz:rpg7=0.5"));require(Math.abs(blast.get()-baseline*.25)<.01,"Living blast multiplier missing/doubled");
   rule.multiplier.set(0.0);require(blast.get()==0,"Zero living blast multiplier failed");
   var sbw=com.atsuishio.superbwarfare.init.ModEntities.CANNON_SHELL.get().create(h.getLevel());sbw.setOwner(bullet.getOwner());sbw.setPos(bullet.position());
   java.util.function.Supplier<Float> sbwBlast=()->{pig.setHealth(100);pig.invulnerableTime=0;new CustomExplosion.Builder(sbw).attacker(bullet.getOwner()).damage(8).radius(3).position(sbw.position()).explode();return 100-pig.getHealth();};
   float sbwBase=sbwBlast.get();require(sbwBase>0,"SBW native blast baseline missing");DamageTuning.rule("sbw","mobs",true).multiplier.set(.5);require(Math.abs(sbwBlast.get()-sbwBase*.5)<.01,"SBW native explosion not tuned");
   var cbc=cannon(h,"he_shell");cbc.setPos(bullet.position());
   java.util.function.Supplier<Float> cbcBlast=()->{pig.setHealth(100);pig.invulnerableTime=0;new ShellExplosion(h.getLevel(),cbc,cbc.indirectArtilleryFire(false),cbc.getX(),cbc.getY(),cbc.getZ(),2,false,Level.ExplosionInteraction.NONE).explode();return 100-pig.getHealth();};
   float cbcBase=cbcBlast.get();require(cbcBase>0,"CBC native blast baseline missing");DamageTuning.rule("cbc","mobs",true).multiplier.set(.5);require(Math.abs(cbcBlast.get()-cbcBase*.5)<.01,"CBC native explosion not tuned");
   System.out.println("COMPAT_TEST DAMAGE_TUNING mob explosion TACZ="+baseline+" SBW="+sbwBase+" CBC="+cbcBase);h.succeed();
  }finally{resetDamageTuning();pig.discard();tank.discard();}});
 }
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
  var original=new ProfileSync.Snapshot(ArmorProfiles.json(),true,false,true,true,new com.google.gson.Gson().toJson(DamageTuning.snapshot()));
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
  var item=new ItemStack(com.atsuishio.superbwarfare.init.ModItems.RPG.get());item.getOrCreateTag().putString("EntityType","superbwarfare:m_1a_2");require(VehicleStats.itemVehicle(item).equals("superbwarfare:m_1a_2"),"Container vehicle ID missing");
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
  require(ArmorProfiles.ammo("gun:maxstuff:excaliber").penetration==600&&ArmorProfiles.ammo("gun:maxstuff:excaliber").blastPenetration==550,"Shipped Excaliber penetration defaults changed");
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


 @GameTest(template="empty",timeoutTicks=60,batch="mountedWeaponSelection")
 public static void mountedWeaponSelection(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  require(player.startRiding(v,true),"Cannot enter vehicle");v.changeSeat(player,0);
  var profiles=new com.google.gson.Gson().fromJson(ArmorProfiles.json(),ArmorProfiles.Data.class);
  var gun=v.getGunData(player);require(gun!=null,"Gunner seat has no selected weapon");
  gun.selectedAmmoType.set(0);var ap=MountedWeapons.inspect(player,profiles,true);
  require(ap!=null&&ap.ammo()!=null&&ap.ammo().penetration==600,"Mounted AP display profile incorrect: "+(ap==null?"none":ap.ammunition()));
  require(ap.origin().distanceTo(v.getShootPos(player,1))<.001&&ap.direction().distanceTo(v.getShootVec(player,1))<.001,"Mounted display does not follow firing ray");
  gun.selectedAmmoType.set(1);var he=MountedWeapons.inspect(player,profiles,true);require(he.ammo().type.equals("HE")&&he.ammo().blastPenetration==35,"Ammo selection did not update display");
  require(MountedWeapons.inspect(player,profiles,false).ammo()==null,"Disabled mounted armor still previews penetration");
  v.setWeaponIndex(0,1);var mg=MountedWeapons.inspect(player,profiles,true);require(mg.ammo()!=null&&mg.ammo().penetration==12,"Machine-gun selection ignored");
  v.changeSeat(player,2);require(MountedWeapons.inspect(player,profiles,true).ammo()==null,"Unarmed passenger borrowed gunner weapon");
  var report=ArmorScan.scan(h.getLevel());var modes=(java.util.List<java.util.Map<String,Object>>)report.get("mountedWeapons");
  require(modes.stream().anyMatch(row->row.get("vehicleId").equals("superbwarfare:m_1a_2")&&row.get("ammoId").equals("superbwarfare:large_shell_he")&&Boolean.TRUE.equals(row.get("mapped"))),"Mounted HE mode missing from scan");
  require(modes.stream().anyMatch(row->row.get("ammoId").equals("FE")&&!Boolean.TRUE.equals(row.get("mapped"))),"Energy ray falsely marked as mapped");
  player.stopRiding();require(MountedWeapons.inspect(player,profiles,true)==null,"On-foot player treated as mounted");v.discard();h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=80,batch="mountedProjectileCapture")
 public static void mountedProjectileCapture(GameTestHelper h) {
  var tank=vehicle(h,"m_1a_2");var target=vehicle(h,"truck");tank.setPos(tank.getX(),200,tank.getZ());target.setPos(tank.position().add(0,0,40));
  var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.startRiding(tank,true);tank.changeSeat(player,0);
  h.runAtTickTime(8,()->{
   var gun=tank.getGunData(player);gun.selectedAmmoType.set(0);
   var start=target.position().add(0,1,-20);var direction=new net.minecraft.world.phys.Vec3(0,0,1);
   var parameters=new com.atsuishio.superbwarfare.data.gun.ShootParameters(tank,player,h.getLevel(),start,direction,gun,0,true,null,null);
   require(gun.item.shootBullet(parameters),"Actual SBW gun did not spawn projectile");
   var shots=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,new net.minecraft.world.phys.AABB(start.subtract(2,2,2),start.add(2,2,2)),e->e.getPersistentData().contains(MountedWeapons.AMMO_TAG));
   require(!shots.isEmpty(),"Actual SBW factory did not capture mounted ammo");var shot=shots.get(0);
   require(ArmorModel.ammoKey(shot).equals("sbwammo:superbwarfare:large_shell_ap"),"Incorrect fired AP profile: "+ArmorModel.ammoKey(shot));
   gun.selectedAmmoType.set(1);require(ArmorProfiles.ammo(ArmorModel.ammoKey(shot)).penetration==600,"Changing ammo changed an in-flight round");
   CompatConfig.MOUNTED_ARMOR.set(false);require(ArmorModel.ammoKey(shot).isEmpty(),"Mounted toggle did not preserve native fallback");CompatConfig.MOUNTED_ARMOR.set(true);CompatConfig.ADVANCED_ARMOR.set(true);CompatConfig.RICOCHETS.set(false);
  });
  h.runAtTickTime(14,()->{require(target.getHealth()<250||!target.isAlive(),"Actual mounted AP flight caused no damage");CompatConfig.ADVANCED_ARMOR.set(false);CompatConfig.RICOCHETS.set(true);player.stopRiding();tank.discard();target.discard();h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=60,batch="mountedShellExplosion")
 public static void mountedShellExplosion(GameTestHelper h) {
  var tank=vehicle(h,"m_1a_2");var target=vehicle(h,"truck");target.setPos(target.position().add(20,0,0));
  h.runAtTickTime(8,()->{
   var gun=tank.getGunData("Cannon");gun.selectedAmmoType.set(1);
   var p=(net.minecraft.world.entity.projectile.Projectile)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare:cannon_shell")).create(h.getLevel());
   p.setOwner(net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel()));p.setPos(target.position().add(0,4,0));p.setDeltaMovement(0,-1,0);MountedWeapons.capture(p,gun);
   CompatConfig.ADVANCED_ARMOR.set(true);CompatConfig.MOUNTED_ARMOR.set(true);
   float exposure=CustomExplosion.getSeenPercentOptimized(h.getLevel(),p.position(),target);double impact=(1-Math.sqrt(target.distanceToSqr(p.position()))/12)*exposure;float raw=(float)((impact*impact+impact)/2*100);
   var source=h.getLevel().damageSources().thrown(p,p.getOwner());var attributed=new AttributedDamageSource(source,p,1,true,p.position());var expected=ArmorModel.compute(target,attributed,raw);
   require(expected!=null&&expected>0,"Mounted HE armor computation missing");
   target.setHealth(250);new CustomExplosion(h.getLevel(),p,source,100,p.getX(),p.getY(),p.getZ(),6,net.minecraft.world.level.Explosion.BlockInteraction.KEEP).explode();
   float loss=250-target.getHealth();require(Math.abs(loss-expected)<.01,"SBW blast missing/doubled: "+loss+" expected "+expected);
   var unmapped=com.atsuishio.superbwarfare.data.gun.GunData.from(new ItemStack(com.atsuishio.superbwarfare.init.ModItems.RPG.get()));
   var ordinary=rocket(h,target);MountedWeapons.capture(ordinary,unmapped);require(!ordinary.getPersistentData().contains(MountedWeapons.AMMO_TAG),"Handheld gun captured as mounted");
   CompatConfig.ADVANCED_ARMOR.set(false);tank.discard();target.discard();h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=60,batch="separateRunningGear")
 public static void separateRunningGear(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{
   var source=h.getLevel().damageSources().generic();v.setLeftWheelHealth(100);float health=v.getHealth();
   RunningGear.prepare(v,source,com.atsuishio.superbwarfare.tools.OBB.Part.WHEEL_LEFT,40);
   v.setLastDamageSource(source);v.setLastDamageStamp(v.level().getGameTime());v.onHurt(0,null,true);
   require(v.getLeftWheelHealth()==60,"Wheel component did not take separate damage");
   require(v.getHealth()==health,"Wheel-only hit damaged hull");
   v.onHurt(0,null,true);require(v.getLeftWheelHealth()==60,"Wheel damage committed twice");
   var ammo=new ArmorProfiles.Ammo();ammo.penetration=25;ammo.normalization=0;
   var wheel=ArmorModel.resolve(ammo,75,12,1,0,0,1,false,false);
   var hull=ArmorModel.resolve(RunningGear.residual(ammo,wheel.penetration()-wheel.effectiveArmor()),wheel.damage(),75,1,0,0,1,false,false);
   require(wheel.damage()>0&&hull.damage()==0,"Low-penetration wheel shot bypassed tank hull");
   ammo.penetration=600;wheel=ArmorModel.resolve(ammo,100,12,1,0,0,1,false,false);
   hull=ArmorModel.resolve(RunningGear.residual(ammo,wheel.penetration()-wheel.effectiveArmor()),wheel.damage(),75,1,0,0,1,false,false);
   require(hull.damage()>0&&hull.damage()<wheel.damage(),"High penetration round failed second plate");
   // Exercise actual M1A2 geometry and the damage mixin, not only the pure two-plate math.
   var projectile=cannon(h,"shot");var profile=ArmorProfiles.ammo("entity:createbigcannons:shot");double oldPen=profile.penetration;double oldSpeed=profile.referenceSpeed;
   profile.penetration=25;profile.referenceSpeed=0;CompatConfig.ADVANCED_ARMOR.set(true);
   boolean exercised=false;
   try {
    outer:for(var box:v.getOBBs())if(RunningGear.wheel(box.part))for(var dir:new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(1,0,0),new net.minecraft.world.phys.Vec3(-1,0,0),new net.minecraft.world.phys.Vec3(0,0,1),new net.minecraft.world.phys.Vec3(0,0,-1)}) {
     var center=new net.minecraft.world.phys.Vec3(box.center.x,box.center.y,box.center.z);var start=center.subtract(dir.scale(10));var hits=ArmorModel.trace(v,start,dir,30);
     if(hits.isEmpty()||!RunningGear.wheel(hits.get(0).part()))continue;
     projectile.setPos(start);projectile.setDeltaMovement(dir);var ds=new AttributedDamageSource(h.getLevel().damageSources().generic(),projectile,1);
     float beforeHull=v.getHealth();v.setLeftWheelHealth(100);v.setRightWheelHealth(100);
     v.hurt(ds,75);
     require(v.getHealth()==beforeHull,"Wheel weakspot still damages hull through native hurt");
     require(v.getLeftWheelHealth()<100||v.getRightWheelHealth()<100,"Native wheel hit did not damage running gear");
     exercised=true;break outer;
    }
    require(exercised,"No native wheel geometry exercised");
   } finally {profile.penetration=oldPen;profile.referenceSpeed=oldSpeed;CompatConfig.ADVANCED_ARMOR.set(false);}
   System.out.println("COMPAT_TEST wheels separateHP=40 hullOnly=0 APthroughTwoPlates="+hull.damage());
   v.discard();h.succeed();
  });
 }

 @GameTest(template="empty",timeoutTicks=80,batch="destroyedTrackRecovery")
 public static void destroyedTrackRecovery(GameTestHelper h){
  var v=vehicle(h,"yx_100");h.runAtTickTime(8,()->{
   CompatConfig.ADVANCED_ARMOR.set(true);CompatConfig.COMPONENT_HITS.set(true);
   v.setLeftWheelHealth(0);v.setRightWheelHealth(50);
   com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEffectUtils.handlePartHealth(v);
   require(v.getLeftWheelHealth()==0,"Destroyed track passively regenerated");
   require(v.getRightWheelHealth()>50,"Undestroyed track lost normal regeneration");
   require(v.getLeftWheelDamaged()&&RunningGear.immobilized(v),"Destroyed track did not immobilize tank");
   try {
    v.setPower(1);v.setTargetSpeed(1);v.setDeltaRot(1);int checked=0;
    for(var method:com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.class.getDeclaredMethods())if(method.getParameterCount()==1&&method.getParameterTypes()[0]==VehicleEntity.class&&(method.getName().endsWith("$power")||method.getName().endsWith("$speed")||method.getName().endsWith("$turn"))){method.setAccessible(true);require(((Number)method.invoke(null,v)).doubleValue()==0,"Destroyed track retains powered input");checked++;}
    require(checked==3,"Native propulsion redirects were not exercised");
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
   h.runAtTickTime(20,()->{
   require(v.getLeftWheelHealth()==0,"Destroyed track regenerated over live ticks");
   v.setLeftWheelHealth(v.getLeftWheelMaxHealth());
   com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEffectUtils.handlePartHealth(v);
   require(!v.getLeftWheelDamaged()&&!RunningGear.immobilized(v),"Manual repair did not restore tank");
   CompatConfig.ADVANCED_ARMOR.set(false);v.discard();h.succeed();
   });
  });
 }

 @GameTest(template="empty",timeoutTicks=80,batch="aircraftStructureBalance")
 public static void aircraftStructureBalance(GameTestHelper h){
  var v=vehicle(h,"a_10a");h.runAtTickTime(8,()->{
   var profile=ArmorProfiles.vehicle("superbwarfare:a_10a");var tank=ArmorProfiles.vehicle("superbwarfare:m_1a_2");
   require(AircraftBalance.hullMultiplier(profile,ArmorProfiles.ammo("gun:tacz:ak47"),false)==.2,"Aircraft rifle structure scaling wrong");
   require(AircraftBalance.hullMultiplier(profile,ArmorProfiles.ammo("gun:tacz:m95"),false)==.45,"Aircraft anti-materiel scaling wrong");
   require(AircraftBalance.hullMultiplier(profile,RunningGear.residual(ArmorProfiles.ammo("gun:maxstuff:excaliber"),600),false)==1,"Endgame auto .50 unintentionally nerfed");
   require(AircraftBalance.hullMultiplier(profile,ArmorProfiles.ammo("gun:tacz:rpg7"),true)==1,"Aircraft rocket blast nerfed");
   require(AircraftBalance.hullMultiplier(tank,ArmorProfiles.ammo("gun:tacz:ak47"),false)==1,"Tank combat changed");
   CompatConfig.ADVANCED_ARMOR.set(true);v.setMainEngineHealth(100);v.setSubEngineHealth(100);float hull=v.getHealth();
   var source=h.getLevel().damageSources().generic();RunningGear.prepare(v,source,com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE,40);v.setLastDamageSource(source);v.setLastDamageStamp(v.level().getGameTime());v.onHurt(10,null,true);
   require(v.getMainEngineHealth()==60&&v.getSubEngineHealth()==100&&v.getHealth()==hull-10,"Aircraft engine/hull damage not independent");
   // Hit a real A10 engine ray with a mapped cannon projectile through native hurt.
   boolean hitEngine=false;
   outer:for(var box:v.getOBBs())if(AircraftBalance.engine(box.part))for(var dir:new net.minecraft.world.phys.Vec3[]{new net.minecraft.world.phys.Vec3(1,0,0),new net.minecraft.world.phys.Vec3(-1,0,0),new net.minecraft.world.phys.Vec3(0,-1,0)}){
    var center=new net.minecraft.world.phys.Vec3(box.center.x,box.center.y,box.center.z);var start=center.subtract(dir.scale(20));var hits=ArmorModel.trace(v,start,dir,50);
    if(hits.isEmpty()||RunningGear.wheel(hits.get(0).part()))continue;
    var projectile=cannon(h,"ap_shot");projectile.setPos(start);projectile.setDeltaMovement(dir.scale(10));v.setMainEngineHealth(500);v.setSubEngineHealth(500);v.setHealth(250);
    v.hurt(new AttributedDamageSource(h.getLevel().damageSources().generic(),projectile,1),40);
    double component=1000-v.getMainEngineHealth()-v.getSubEngineHealth();
    require(component>0&&Math.abs((250-v.getHealth())-component*profile.aircraftEngineHullFraction)<.01,"Actual aircraft engine hit double-damaged hull");hitEngine=true;break outer;
   }
   require(hitEngine,"Actual aircraft engine geometry not exercised");
   v.setMainEngineHealth(0);com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEffectUtils.handlePartHealth(v);require(v.getMainEngineHealth()==0,"Destroyed aircraft engine self-revived");
   v.setMainEngineHealth(v.getMainEngineMaxHealth());com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEffectUtils.handlePartHealth(v);require(!v.getMainEngineDamaged(),"Engine repair failed");
   CompatConfig.ADVANCED_ARMOR.set(false);v.discard();h.succeed();
  });
 }
}
