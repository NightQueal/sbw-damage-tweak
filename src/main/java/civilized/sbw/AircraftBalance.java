package civilized.sbw;
import com.atsuishio.superbwarfare.tools.OBB;
/** Aircraft-specific structure scaling; penetration and component damage remain separate. */
public final class AircraftBalance {
 public static boolean engine(OBB.Part p){return p==OBB.Part.MAIN_ENGINE||p==OBB.Part.SUB_ENGINE;}
 public static double hullMultiplier(ArmorProfiles.Vehicle v,ArmorProfiles.Ammo a,boolean blast){
  if(!v.aircraft||blast||a.caliber>=v.smallArmsMaxCaliber||a.penetration>v.smallArmsMaxPenetration||a.type.equals("HEAT")||a.type.equals("HE")||a.type.equals("APHE"))return 1;
  return a.caliber>=12?v.heavySmallArmsHullMultiplier:v.smallArmsHullMultiplier;
 }
 public static float regenerateEngine(com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity v,float current,float proposed){
  var profile=ArmorProfiles.vehicle(DamageCompatibility.id(v));return RunningGear.active(v)&&profile!=null&&profile.aircraft&&current<=0?0:proposed;
 }
}
