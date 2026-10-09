package civilized.sbw;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.damagesource.DamageSource;
/** Pending component damage is committed only after native permission/friendly-fire checks. */
public final class RunningGear {
 private record Pending(DamageSource source,OBB.Part part,float damage,long tick) {}
 private static final java.util.Map<VehicleEntity,Pending> pending=new java.util.WeakHashMap<>();
 public static boolean wheel(OBB.Part part){return part==OBB.Part.WHEEL_LEFT||part==OBB.Part.WHEEL_RIGHT;}
 public static ArmorProfiles.Ammo residual(ArmorProfiles.Ammo ammo,double penetration){
  var a=new ArmorProfiles.Ammo();a.type=ammo.type;a.caliber=ammo.caliber;a.normalization=ammo.normalization;a.ricochetStart=ammo.ricochetStart;a.ricochetEnd=ammo.ricochetEnd;a.overmatchRatio=ammo.overmatchRatio;a.postPenMultiplier=ammo.postPenMultiplier;a.blastPenetration=ammo.blastPenetration;a.blastLeak=ammo.blastLeak;
  a.penetration=Math.max(0,penetration);a.referenceSpeed=0;return a;
 }
 public static void prepare(VehicleEntity v,DamageSource source,OBB.Part part,float damage){pending.put(v,new Pending(source,part,damage,v.level().getGameTime()));}
 public static void commit(VehicleEntity v){
  var p=pending.remove(v);if(p==null||p.source()!=v.getLastDamageSource()||p.tick()!=v.level().getGameTime()||p.damage()<=0)return;
  if(p.part()==OBB.Part.WHEEL_LEFT)v.setLeftWheelHealth(Math.max(0,v.getLeftWheelHealth()-p.damage()));
  else v.setRightWheelHealth(Math.max(0,v.getRightWheelHealth()-p.damage()));
 }
}
