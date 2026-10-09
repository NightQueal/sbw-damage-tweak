package civilized.sbw;
import com.google.gson.GsonBuilder;
import com.tacz.guns.api.TimelessAPI;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.nio.file.Files;
/** Explicit operator-triggered inventory of loaded registries; never writes armor profiles. */
public final class ArmorScan {
 public static void register(RegisterCommandsEvent event) {
  event.getDispatcher().register(Commands.literal("sbwcompat").requires(s->s.hasPermission(2)).then(Commands.literal("armor").then(Commands.literal("scan").executes(ctx->{
   try {
    Map<String,Object> report=scan(ctx.getSource().getLevel());
    var file=FMLPaths.CONFIGDIR.get().resolve("civilized-sbw-armor-scan.json");
    Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    ctx.getSource().sendSuccess(()->Component.literal("Armor scan saved: config/civilized-sbw-armor-scan.json. Unmapped vehicles="+report.get("unmappedVehicleCount")+", guns="+report.get("unmappedGunCount")+", ammo="+report.get("unmappedAmmoCount")+", projectiles="+report.get("unmappedProjectileCount")+". Live armor profiles unchanged."),false);
    return 1;
   } catch(Exception e){ctx.getSource().sendFailure(Component.literal("Armor scan failed: "+e.getMessage()));return 0;}
  }))));
 }
 public static Map<String,Object> scan(ServerLevel level) {
  Map<String,Object> report=new LinkedHashMap<>();
  List<Map<String,Object>> vehicles=new ArrayList<>(),projectiles=new ArrayList<>(),guns=new ArrayList<>(),ammo=new ArrayList<>(),mounted=new ArrayList<>();
  List<String> errors=new ArrayList<>();Set<String> discoveredVehicles=new TreeSet<>(),discoveredAmmo=new TreeSet<>();
  int missingVehicles=0,missingProjectiles=0,missingGuns=0,missingAmmo=0;
  var profiles=new com.google.gson.Gson().fromJson(ArmorProfiles.json(),ArmorProfiles.Data.class);
  // Instances are only inspected, never spawned/added to the level. One pass per command, no ticking scan.
  for(var entry:ForgeRegistries.ENTITY_TYPES.getEntries().stream().sorted(Comparator.comparing(e->e.getKey().location().toString())).toList()) {
   String id=entry.getKey().location().toString();
   try {
    var entity=entry.getValue().create(level);if(entity==null)continue;
    if(entity instanceof VehicleEntity) {
     boolean mapped=ArmorProfiles.vehicle(id)!=null;discoveredVehicles.add(id);if(!mapped)missingVehicles++;
     vehicles.add(Map.of("id",id,"mapped",mapped,"profileKey",id));
     VehicleEntity v=(VehicleEntity)entity;
     for(var weapon:v.getGunDataMap().entrySet()) {
      var gun=weapon.getValue().copy();int modes=gun.get(com.atsuishio.superbwarfare.data.gun.GunProp.AMMO_CONSUMER).size();
      for(int mode=0;mode<modes;mode++) {
       gun.selectedAmmoType.set(mode);String gunKey=MountedWeapons.gunKey(gun),ammoKey=MountedWeapons.ammoKey(gun);
       discoveredAmmo.add(gunKey);discoveredAmmo.add(ammoKey);String effective=MountedWeapons.key(gun,profiles);
       String projectile=gun.get(com.atsuishio.superbwarfare.data.gun.GunProp.PROJECTILE).getId();
       boolean supported=projectile.startsWith("superbwarfare:")&&ArmorProfiles.ammo(effective)!=null;
       Map<String,Object> row=new LinkedHashMap<>();row.put("vehicleId",id);row.put("weapon",weapon.getKey());row.put("ammoMode",mode);row.put("ammoId",MountedWeapons.ammoId(gun));row.put("projectileId",projectile);row.put("mapped",supported);row.put("effectiveProfileKey",supported?effective:"");row.put("suggestedGunProfileKey",gunKey);row.put("suggestedAmmoProfileKey",ammoKey);mounted.add(row);
      }
     }
    } else if(entity instanceof Projectile && (id.startsWith("createbigcannons:")||id.startsWith("superbwarfare:"))) {
     String key="entity:"+id;boolean mapped=ArmorProfiles.ammo(key)!=null;discoveredAmmo.add(key);if(!mapped)missingProjectiles++;
     projectiles.add(Map.of("id",id,"mapped",mapped,"profileKey",key,"note","Unmapped includes utility/noncombat entities. Mapping alone does not add missing source attribution or explosion hooks."));
    }
   } catch(RuntimeException|LinkageError e){errors.add(id+": "+e.getClass().getSimpleName()+": "+e.getMessage());}
  }
  for(var entry:TimelessAPI.getAllCommonGunIndex().stream().sorted(Comparator.comparing(e->e.getKey().toString())).toList()) {
   String id=entry.getKey().toString(),gunKey="gun:"+id,ammoKey="ammo:"+entry.getValue().getGunData().getAmmoId();
   discoveredAmmo.add(gunKey);discoveredAmmo.add(ammoKey);
   String effective=ArmorProfiles.ammo(gunKey)!=null?gunKey:ArmorProfiles.ammo(ammoKey)!=null?ammoKey:"";
   if(effective.isEmpty())missingGuns++;
   var bullet=entry.getValue().getBulletData();Map<String,Object> row=new LinkedHashMap<>();
   row.put("id",id);row.put("ammoId",entry.getValue().getGunData().getAmmoId().toString());row.put("mapped",!effective.isEmpty());row.put("effectiveProfileKey",effective);row.put("suggestedGunProfileKey",gunKey);
   row.put("baseDamagePerProjectile",bullet.getDamageAmount());row.put("projectilesPerShot",bullet.getBulletAmount());row.put("configuredSpeed",bullet.getSpeed());
   var explosion=bullet.getExplosionData();if(explosion!=null){row.put("explosive",explosion.isExplode());row.put("configuredBlastDamage",explosion.getDamage());}
   guns.add(row);
  }
  for(var entry:TimelessAPI.getAllCommonAmmoIndex().stream().sorted(Comparator.comparing(e->e.getKey().toString())).toList()) {
   String id=entry.getKey().toString(),key="ammo:"+id;discoveredAmmo.add(key);boolean mapped=ArmorProfiles.ammo(key)!=null;if(!mapped)missingAmmo++;
   ammo.add(Map.of("id",id,"mapped",mapped,"profileKey",key,"note","An unmapped ammo can still be covered by a gun-specific override; consult guns."));
  }
  Set<String> orphanVehicles=new TreeSet<>(ArmorProfiles.vehicleKeys());orphanVehicles.removeAll(discoveredVehicles);
  Set<String> orphanAmmo=new TreeSet<>(ArmorProfiles.ammoKeys());orphanAmmo.removeAll(discoveredAmmo);
  report.put("notes","Snapshot of loaded server registries and TACZ gun packs. Mapped means an ID profile exists, not guaranteed hook/geometry coverage. Unmapped utility projectiles may intentionally remain native. No penetration/armor values are guessed or applied.");
  report.put("unmappedVehicleCount",missingVehicles);report.put("unmappedProjectileCount",missingProjectiles);report.put("unmappedGunCount",missingGuns);report.put("unmappedAmmoCount",missingAmmo);
  report.put("mountedWeapons",mounted);report.put("unmappedMountedWeaponModeCount",mounted.stream().filter(row->!Boolean.TRUE.equals(row.get("mapped"))).count());
  report.put("vehicles",vehicles);report.put("projectiles",projectiles);report.put("guns",guns);report.put("ammo",ammo);report.put("orphanVehicleProfiles",orphanVehicles);report.put("orphanAmmoProfiles",orphanAmmo);report.put("inspectionErrors",errors);
  return report;
 }
}
