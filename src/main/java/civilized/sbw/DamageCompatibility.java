package civilized.sbw;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
public final class DamageCompatibility {
 private static final org.slf4j.Logger LOG=LogUtils.getLogger();
 public static String id(Entity entity){return entity==null?"":String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));}
 public static double vehicleFactor(Entity vehicle){return CompatConfig.factor(CompatConfig.VEHICLE_OVERRIDES.get(),id(vehicle));}
 public static DamageSource directSource(Entity projectile,Entity target,DamageSource original) {
  if(!(target instanceof VehicleEntity)||!CompatConfig.CBC_ATTRIBUTION.get())return original;
  double scale=CompatConfig.CBC_DIRECT_SCALE.get()*CompatConfig.factor(CompatConfig.CBC_DIRECT_OVERRIDES.get(),id(projectile))*vehicleFactor(target);
  return new AttributedDamageSource(original,projectile,scale);
 }
 public static void explosion(ExplosionEvent.Detonate event) {
  if(event.getLevel().isClientSide)return;
  Explosion explosion=event.getExplosion();Entity projectile=explosion.getDirectSourceEntity();
  boolean tacz=explosion instanceof TaczExplosionAccess && CompatConfig.TACZ_EXPLOSIONS.get();
  boolean cbc=(explosion instanceof rbasamoyai.createbigcannons.munitions.ShellExplosion || explosion instanceof rbasamoyai.createbigcannons.munitions.ImpactExplosion || explosion instanceof rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion || explosion instanceof rbasamoyai.createbigcannons.munitions.big_cannon.mortar_stone.MortarStoneExplosion) && CompatConfig.CBC_ATTRIBUTION.get() && id(projectile).startsWith("createbigcannons:");
  boolean sbw=explosion instanceof CustomExplosion && projectile!=null && projectile.getPersistentData().contains(MountedWeapons.AMMO_TAG) && CompatConfig.ADVANCED_ARMOR.get() && CompatConfig.MOUNTED_ARMOR.get() && ArmorProfiles.ammo(MountedWeapons.projectileKey(projectile))!=null;
  if(!tacz&&!cbc&&!sbw)return;
  double radius=(tacz?((TaczExplosionAccess)explosion).civilized$getRadius():((ExplosionAccess)explosion).superbwarfare$getRadius())*2.0;
  if(radius<=0 || !Double.isFinite(radius))return;
  double scale=sbw?1:tacz?CompatConfig.TACZ_SCALE.get():CompatConfig.CBC_BLAST_SCALE.get();
  if(tacz&&projectile instanceof EntityKineticBullet bullet)scale*=CompatConfig.factor(CompatConfig.TACZ_OVERRIDES.get(),String.valueOf(bullet.getGunId()));
  if(cbc)scale*=CompatConfig.factor(CompatConfig.CBC_BLAST_OVERRIDES.get(),id(projectile));
  var iterator=event.getAffectedEntities().iterator();
  while(iterator.hasNext()) {
   Entity target=iterator.next();if(!(target instanceof VehicleEntity vehicle))continue;
   if(sbw&&ArmorProfiles.vehicle(id(vehicle))==null)continue;
   // Remove once, before SBW's normal-priority handler and the original explosion's own hurt loop.
   iterator.remove();if(vehicle.ignoreExplosion())continue;
   var pos=explosion.getPosition();double ratio=Math.sqrt(vehicle.distanceToSqr(pos))/radius;
   if(ratio>1)continue;
   double dx=vehicle.getX()-pos.x,dy=vehicle.getEyeY()-pos.y,dz=vehicle.getZ()-pos.z;
   if(dx*dx+dy*dy+dz*dz==0)continue;
   float exposure=!vehicle.enableAABB()?CustomExplosion.getSeenPercentOptimized(event.getLevel(),pos,vehicle):Explosion.getSeenPercent(pos,vehicle);
   double impact=(1-ratio)*exposure;
   double raw=sbw?(impact*impact+impact)/2.0*((CustomExplosion)explosion).getDamage():tacz?(impact*impact+impact)/2.0*((TaczExplosionAccess)explosion).civilized$getDamage():(impact*impact+impact)/2.0*7*radius+1;
   float input=(float)Math.min(Float.MAX_VALUE,raw*scale*(sbw?1:vehicleFactor(vehicle)));
   DamageSource source=projectile instanceof net.minecraft.world.entity.projectile.Projectile?new AttributedDamageSource(explosion.getDamageSource(),projectile,1,true,pos):explosion.getDamageSource();
   float before=vehicle.getHealth();vehicle.hurt(source,input);
   if(CompatConfig.DEBUG.get())LOG.info("SBW compat {} blast projectile={} vehicle={} exposure={} input={} healthLoss={}",sbw?"SBW":tacz?"TACZ":"CBC",id(projectile),id(vehicle),exposure,input,before-vehicle.getHealth());
  }
 }
 public static float directInput(float input,Entity entity,DamageSource source) {
  if(!(entity instanceof VehicleEntity)||!(source instanceof AttributedDamageSource attributed))return input;
  float scaled=(float)Math.min(Float.MAX_VALUE,input*attributed.inputMultiplier);
  if(CompatConfig.DEBUG.get())LOG.info("SBW compat CBC input projectile={} vehicle={} original={} scaled={}",id(source.getDirectEntity()),id(entity),input,scaled);
  return scaled;
 }
}
