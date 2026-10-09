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
 @GameTest(template="empty",timeoutTicks=60)
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
 @GameTest(template="empty",timeoutTicks=60)
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
 @GameTest(template="empty",timeoutTicks=60)
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
 @GameTest(template="empty",timeoutTicks=80)
 public static void realCbcFlight(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{var p=cannon(h,"ap_shot");p.setPos(v.position().add(0,7,0));p.setDeltaMovement(0,-3,0);h.getLevel().addFreshEntity(p);});
  h.runAtTickTime(24,()->{float loss=250-v.getHealth();System.out.println("COMPAT_TEST CBC AP actual flight healthLoss="+loss);require(loss>0&&loss<10,"Actual CBC AP shot flight failed or native armor bypassed: "+loss);v.discard();h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void nativeArmorAndScope(GameTestHelper h) {
  var v=vehicle(h,"m_1a_2");h.runAtTickTime(8,()->{var p=cannon(h,"ap_shot");p.setPos(v.position().add(0,1,6));var front=direct(p,v);var scripted=new com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier().reduce(20).multiply(.2f).custom((entity,source,damage)->((VehicleEntity)entity).getSourceAngle(source,.3f)*damage);float a=scripted.compute(v,front,50);p.setPos(v.position().add(0,1,-6));float b=scripted.compute(v,direct(p,v),50);System.out.println("COMPAT_TEST directional raw="+v.getSourceAngle(front,.3f)+" front="+a+" rear="+b);require(Math.abs(a-b)>1,"Directional armor lost: "+a+" / "+b);
  var holder=h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("tacz:bullet")));
  var bullet=new DamageSource(holder,p,p);require(v.getDamageModifier().compute(v,bullet,100)==0,"Native ordinary-bullet immunity changed");
  var nonVehicle=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());require(direct(p,nonVehicle).getDirectEntity()==null,"CBC non-vehicle source changed");
  CompatConfig.VEHICLE_OVERRIDES.set(java.util.List.of("superbwarfare:m_1a_2=0.0"));v.setHealth(250);v.hurt(direct(p,v),50);require(v.getHealth()==250,"Per-vehicle disable failed");CompatConfig.VEHICLE_OVERRIDES.set(java.util.List.of());
  System.out.println("COMPAT_TEST CBC AP directional native armor="+a+" / "+b);v.discard();h.succeed();});
 }
 @GameTest(template="empty",timeoutTicks=40)
 public static void resourceRules(GameTestHelper h) {
  var tag=TagKey.create(Registries.ITEM,new ResourceLocation("forge:ingots/tungsten"));
  require(!new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare:tungsten_ingot"))).is(tag)&&new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("northstar:tungsten_ingot"))).is(tag),"Tungsten separation broken");
  var recipes=h.getLevel().getRecipeManager();for(String id:new String[]{"civilized_sbw:pressing/steel_plate","civilized_sbw:pressing/copper_plate","civilized_sbw:compacting/armor_plate","civilized_sbw:vehicle_key"})require(recipes.byKey(new ResourceLocation(id)).isPresent(),"Missing "+id);
  for(String id:new String[]{"civilized_sbw:drilling/scheelite","civilized_sbw:ore_vein_type/galena","superbwarfare:netherite_hammer","superbwarfare:dps_generator_deployer"})require(recipes.byKey(new ResourceLocation(id)).isEmpty(),"Disabled recipe active "+id);
  var dps=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare:dps_generator")).create(h.getLevel());require(!h.getLevel().addFreshEntity(dps),"DPS spawn enforcement lost");h.succeed();
 }
}
