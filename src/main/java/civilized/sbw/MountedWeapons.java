package civilized.sbw;
import com.atsuishio.superbwarfare.data.gun.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
/** Selected mounted ammo is read live for inspection and frozen on the fired projectile. */
public final class MountedWeapons {
 public static final String AMMO_TAG="civilized_sbw_mounted_ammo",GUN_TAG="civilized_sbw_mounted_gun";
 public record Inspection(Vec3 origin,Vec3 direction,ArmorProfiles.Ammo ammo,String weapon,String ammunition,boolean mounted) {}
 public static VehicleEntity vehicle(Entity viewer) {
  for(Entity e=viewer;e!=null;e=e.getVehicle())if(e instanceof VehicleEntity v)return v;
  return null;
 }
 public static String ammoId(GunData data){return data.selectedAmmoConsumer().getAmmo();}
 public static String gunKey(GunData data){return "sbwgun:"+data.get(GunProp.NAME)+"|"+ammoId(data);}
 public static String ammoKey(GunData data){return "sbwammo:"+ammoId(data);}
 public static String key(GunData data,ArmorProfiles.Data profiles){String gun=gunKey(data);return profiles.ammunition.containsKey(gun)?gun:ammoKey(data);}
 public static ArmorProfiles.Ammo profile(GunData data,ArmorProfiles.Data profiles){return profiles.ammunition.get(key(data,profiles));}
 public static void capture(Entity entity,GunData data) {
  if(!(entity instanceof Projectile)||!data.stack.is(ModItems.VEHICLE_GUN.get()))return;
  entity.getPersistentData().putString(AMMO_TAG,ammoKey(data));entity.getPersistentData().putString(GUN_TAG,gunKey(data));
 }
 public static String projectileKey(Entity projectile) {
  var tag=projectile.getPersistentData();String gun=tag.getString(GUN_TAG);
  return ArmorProfiles.ammo(gun)!=null?gun:tag.getString(AMMO_TAG);
 }
 public static Inspection inspect(Entity viewer,ArmorProfiles.Data profiles,boolean enabled) {
  var v=vehicle(viewer);if(v==null)return null;
  var gun=v.getGunData(viewer);
  if(gun==null)return new Inspection(viewer.getEyePosition(),viewer.getLookAngle(),null,"","",true);
  return new Inspection(v.getShootPos(viewer,1),v.getShootVec(viewer,1),enabled?profile(gun,profiles):null,gun.get(GunProp.NAME),ammoId(gun),true);
 }
}
