package civilized.sbw;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.tools.OBB;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.List;
/** Server-only penetration calculation. Runs only for explicitly mapped ammunition and vehicles. */
public final class ArmorModel {
 public record Hit(double distance,Vec3 point,Vec3 normal,OBB.Part part) {}
 public record Result(double damage,boolean penetrated,boolean ricochet,double effectiveArmor,double penetration) {}
 // Stable per projectile/vehicle/tick, so debug recalculation does not consume RNG or reroll the same hit.
 private static double sample(Entity projectile,Entity vehicle) {
  long x=projectile.getUUID().getLeastSignificantBits()^vehicle.getUUID().getMostSignificantBits()^vehicle.level().getGameTime();
  x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;x^=x>>>31;
  return (x>>>11)*0x1.0p-53;
 }
 public static String ammoKey(Entity projectile) {
  if(projectile instanceof EntityKineticBullet bullet) {
   String gun="gun:"+bullet.getGunId();if(ArmorProfiles.ammo(gun)!=null)return gun;
   return "ammo:"+bullet.getAmmoId();
  }
  return "entity:"+DamageCompatibility.id(projectile);
 }
 public static Result resolve(ArmorProfiles.Ammo ammo,double raw,double plate,double chemical,double angle,double speed,double roll,boolean blast,boolean ricochets) {
  double penetration=blast?ammo.blastPenetration:ammo.penetration;
  boolean shaped=ammo.type.equals("HEAT");
  if(!blast&&!shaped&&ammo.referenceSpeed>0)penetration*=Math.pow(Math.min(3,Math.max(0,speed/ammo.referenceSpeed)),ammo.velocityExponent);
  double theta=Math.max(0,angle-(!blast&&!shaped?ammo.normalization:0));
  double effective=plate*(shaped?chemical:1)/Math.max(.02,Math.cos(Math.toRadians(theta)));
  boolean overmatch=!blast&&!shaped&&!ammo.type.equals("HE")&&plate>0&&ammo.caliber/plate>=ammo.overmatchRatio;
  double probability=ammo.ricochetEnd==ammo.ricochetStart?(angle>=ammo.ricochetEnd?1:0):Math.max(0,Math.min(1,(angle-ammo.ricochetStart)/(ammo.ricochetEnd-ammo.ricochetStart)));
  if(!blast&&ricochets&&!overmatch&&roll<probability)return new Result(0,false,true,effective,penetration);
  if(overmatch)effective=plate;
  if(penetration<=effective)return new Result(blast?raw*ammo.blastLeak:0,false,false,effective,penetration);
  double residual=1-effective/Math.max(.001,penetration);
  double factor=blast?Math.max(ammo.blastLeak,residual):(.25+.75*residual)*ammo.postPenMultiplier;
  return new Result(Math.max(0,raw*factor),true,false,effective,penetration);
 }
 // Ray/slab intersection in an OBB's local coordinates; normal returned in world space.
 public static Hit intersect(OBB box,Vec3 start,Vec3 direction,double maxDistance) {
  Vector3d o=new Vector3d(start.x-box.center.x,start.y-box.center.y,start.z-box.center.z);
  Vector3d d=new Vector3d(direction.x,direction.y,direction.z);
  box.rotation().transformInverse(o);box.rotation().transformInverse(d);
  double[] os={o.x,o.y,o.z},ds={d.x,d.y,d.z},ext={box.extents().x,box.extents().y,box.extents().z};
  double enter=0,exit=maxDistance;int axis=-1;double sign=0;
  for(int i=0;i<3;i++) {
   if(ext[i]<=0)return null;
   if(Math.abs(ds[i])<1e-9){if(Math.abs(os[i])>ext[i])return null;continue;}
   double a=(-ext[i]-os[i])/ds[i],b=(ext[i]-os[i])/ds[i],near=Math.min(a,b),far=Math.max(a,b);
   if(near>enter){enter=near;axis=i;sign=ds[i]>0?-1:1;}
   exit=Math.min(exit,far);if(enter>exit)return null;
  }
  if(axis<0||enter<0||enter>maxDistance)return null;
  Vector3d n=new Vector3d();n.setComponent(axis,sign);box.rotation().transform(n);
  return new Hit(enter,start.add(direction.scale(enter)),new Vec3(n.x,n.y,n.z),box.part);
 }
 public static List<Hit> trace(VehicleEntity vehicle,Vec3 start,Vec3 direction,double length) {
  java.util.ArrayList<Hit> hits=new java.util.ArrayList<>();
  for(OBB box:vehicle.getOBBs()) {
   if(box.part==OBB.Part.COLLISION)continue;
   Hit hit=intersect(box,start,direction,length);if(hit!=null)hits.add(hit);
  }
  hits.sort(java.util.Comparator.comparingDouble(Hit::distance));return hits;
 }
 public static double plate(ArmorProfiles.Vehicle profile,VehicleEntity vehicle,Hit hit) {
  if(hit.part()==OBB.Part.WHEEL_LEFT||hit.part()==OBB.Part.WHEEL_RIGHT)return profile.trackArmor;
  Vector3d local=new Vector3d(hit.normal().x,hit.normal().y,hit.normal().z);
  vehicle.getRotationFromString("Default").transformInverse(local);
  Vec3 n=new Vec3(local.x,local.y,local.z);
  double armor=Math.abs(n.y)>=Math.max(Math.abs(n.x),Math.abs(n.z))?(n.y>0?profile.roof:profile.bottom):Math.abs(n.z)>Math.abs(n.x)?(n.z>0?profile.front:profile.rear):profile.side;
  return armor*(hit.part()==OBB.Part.TURRET?profile.turretMultiplier:1);
 }
 /** null means native SBW armor handles the hit. */
 public static Float compute(Entity entity,DamageSource source,float raw) {
  if(!CompatConfig.ADVANCED_ARMOR.get()||entity.level().isClientSide||!(entity instanceof VehicleEntity vehicle))return null;
  Entity projectile=source.getDirectEntity();if(!(projectile instanceof Projectile))return null;
  ArmorProfiles.Vehicle profile=ArmorProfiles.vehicle(DamageCompatibility.id(entity));
  ArmorProfiles.Ammo ammo=ArmorProfiles.ammo(ammoKey(projectile));if(profile==null||ammo==null)return null;
  boolean blast=source instanceof AttributedDamageSource attributed&&attributed.blast;
  Vec3 flight=projectile.getDeltaMovement().normalize();
  Vec3 direction=flight;
  Vec3 origin=blast?((AttributedDamageSource)source).impactOrigin:projectile.position();
  boolean jet=false;
  List<Hit> hits;
  // A shaped-charge jet only applies along the incoming rocket ray, near its impact point.
  // Nearby off-axis vehicles receive ordinary fragmentation/blast, never the full HEAT penetration.
  if(blast&&ammo.type.equals("HEAT")&&flight.lengthSqr()>1e-9) {
   hits=trace(vehicle,origin.subtract(flight.scale(2)),flight,6);
   jet=!hits.isEmpty();
  } else hits=java.util.List.of();
  if(!jet) {
   if(blast||direction.lengthSqr()<1e-9)direction=vehicle.getBoundingBox().getCenter().subtract(origin).normalize();
   if(direction.lengthSqr()<1e-9)return null;
   Vec3 start=origin.subtract(direction.scale(blast?0:64));
   hits=trace(vehicle,start,direction,blast?origin.distanceTo(vehicle.position())+64:128);
  }
  if(hits.isEmpty())return null; // Missing component geometry: retain native behavior.
  boolean areaBlast=blast&&!jet;
  Hit surface=hits.get(0);
  double angle=Math.toDegrees(Math.acos(Math.min(1,Math.max(0,-direction.dot(surface.normal())))));
  float input=raw;

  Result result=resolve(ammo,input,plate(profile,vehicle,surface),profile.chemicalMultiplier,angle,projectile.getDeltaMovement().length(),sample(projectile,vehicle),areaBlast,CompatConfig.RICOCHETS.get());
  OBB.Part part=surface.part();
  if(!areaBlast&&result.penetrated()&&CompatConfig.COMPONENT_HITS.get()&&part!=OBB.Part.WHEEL_LEFT&&part!=OBB.Part.WHEEL_RIGHT) {
   // Find the first intersected engine behind the exterior plate. Hull is still tested before engine damage.
   for(Hit hit:hits)if(hit.part()==OBB.Part.MAIN_ENGINE||hit.part()==OBB.Part.SUB_ENGINE){part=hit.part();break;}
   OBBHitter.getInstance(projectile).sbw$setCurrentHitPart(part);
  }
  if(areaBlast||!result.penetrated())OBBHitter.getInstance(projectile).sbw$setCurrentHitPart(OBB.Part.EMPTY);
  double damage=result.damage();
  if(!areaBlast&&result.penetrated()&&(part==OBB.Part.MAIN_ENGINE||part==OBB.Part.SUB_ENGINE))damage*=profile.engineDamageMultiplier;
  if(CompatConfig.DEBUG.get())com.mojang.logging.LogUtils.getLogger().info("SBW armor vehicle={} ammo={} part={} angle={} armor={} penetration={} ricochet={} damage={}",DamageCompatibility.id(vehicle),ammoKey(projectile),part,angle,result.effectiveArmor(),result.penetration(),result.ricochet(),damage);
  return (float)Math.min(Float.MAX_VALUE,damage);
 }
}
