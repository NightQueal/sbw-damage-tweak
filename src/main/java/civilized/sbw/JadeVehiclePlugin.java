package civilized.sbw;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
@WailaPlugin("civilized_sbw")
public final class JadeVehiclePlugin implements IWailaPlugin {
 @Override public void registerClient(IWailaClientRegistration registration){registration.registerEntityComponent(new Provider(),VehicleEntity.class);registration.markAsClientFeature(new ResourceLocation("civilized_sbw","vehicle_armor"));LongRangeVehicleInspection.register(registration);}
 public static final class Provider implements IEntityComponentProvider {
  @Override public ResourceLocation getUid(){return new ResourceLocation("civilized_sbw","vehicle_armor");}
  @Override public void appendTooltip(ITooltip tooltip,EntityAccessor accessor,IPluginConfig config) {
   var profiles=ProfileSync.clientProfiles;var settings=ProfileSync.client;
   if(profiles==null||settings==null||!settings.enabled()||!(accessor.getEntity() instanceof VehicleEntity v))return;
   var armor=profiles.vehicles.get(DamageCompatibility.id(v));if(armor==null)return;
   String gear=v.getEngineInfo() instanceof com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.Track?"Track":"Wheel";
   boolean details=accessor.showDetails()||(accessor.getPlayer()!=null&&MountedWeapons.vehicle(accessor.getPlayer())!=null&&CompatConfig.MOUNTED_DETAILS.get());
   if(armor.aircraft)tooltip.add(Component.literal("Engine HP 1/2: "+StatText.number(v.getMainEngineHealth())+" / "+StatText.number(v.getSubEngineHealth())).withStyle(v.getMainEngineHealth()<=0?net.minecraft.ChatFormatting.RED:net.minecraft.ChatFormatting.GREEN));
   else tooltip.add(Component.literal(gear+" HP L/R: "+StatText.number(v.getLeftWheelHealth())+" / "+StatText.number(v.getRightWheelHealth())+" HP").withStyle(v.getLeftWheelHealth()<=0||v.getRightWheelHealth()<=0?net.minecraft.ChatFormatting.RED:net.minecraft.ChatFormatting.GREEN));
   if(settings.components()&&v.getEngineInfo() instanceof com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.Track&&(v.getLeftWheelHealth()<=0||v.getRightWheelHealth()<=0))tooltip.add(Component.literal("IMMOBILIZED").withStyle(net.minecraft.ChatFormatting.RED));
   var player=accessor.getPlayer();if(player==null)return;
   var mounted=MountedWeapons.inspect(player,profiles,settings.mounted());
   if(mounted!=null&&mounted.weapon().isEmpty())mounted=null;
   var ammo=mounted==null?VehicleTooltips.gunProfile(player.getMainHandItem()):mounted.ammo();if(ammo==null)return;
   if(details&&mounted!=null){tooltip.add(Component.translatable(mounted.weapon(), ""));tooltip.add(Component.literal("Ammo: ").append(mounted.ammunition().startsWith("superbwarfare:")?Component.translatable("item.superbwarfare."+mounted.ammunition().substring(14)):Component.literal(mounted.ammunition())));}
   Vec3 start=mounted==null?player.getEyePosition():mounted.origin();Vec3 end=accessor.getHitResult().getLocation();Vec3 delta=end.subtract(start);
   if(delta.lengthSqr()<1e-9)return;Vec3 direction=mounted==null?delta.normalize():mounted.direction().normalize();var hits=ArmorModel.trace(v,start,direction,delta.length()+8);if(hits.isEmpty())return;
   var hit=hits.get(0);double angle=Math.toDegrees(Math.acos(Math.max(0,Math.min(1,-direction.dot(hit.normal())))));
   var result=VehicleStats.preview(ammo,ArmorModel.plate(armor,v,hit),armor.chemicalMultiplier,angle,settings.ricochets());
   double raw=mounted==null?VehicleTooltips.damageAt(player.getMainHandItem(),delta.length()):MountedWeapons.vehicle(player).getGunData(player).get(com.atsuishio.superbwarfare.data.gun.GunProp.DAMAGE);
   DamageTuning.Weapon damageWeapon=mounted==null&&player.getMainHandItem().getItem() instanceof com.tacz.guns.api.item.IGun gun?new DamageTuning.Weapon("tacz",String.valueOf(gun.getGunId(player.getMainHandItem()))):mounted==null?null:new DamageTuning.Weapon("sbw",MountedWeapons.damageKey(MountedWeapons.vehicle(player).getGunData(player)));
   raw*=DamageTuning.previewFactor(ProfileSync.clientDamageTuning,damageWeapon,"vehicles",false);
   double finalDamage=raw*result.damagePercent()/100;double wheelDamage=0;
   if(RunningGear.wheel(hit.part())&&settings.components()) {
    var hull=hits.stream().filter(h->h.distance()>hit.distance()+.001&&!RunningGear.wheel(h.part())&&h.part()!=com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE&&h.part()!=com.atsuishio.superbwarfare.tools.OBB.Part.SUB_ENGINE).findFirst().orElse(null);
    wheelDamage=finalDamage;finalDamage=0;
    if(hull!=null&&result.damagePercent()>0){double hullAngle=Math.toDegrees(Math.acos(Math.max(0,Math.min(1,-direction.dot(hull.normal())))));finalDamage=ArmorModel.resolve(RunningGear.residual(ammo,result.penetration()-result.armor()),wheelDamage,ArmorModel.plate(armor,v,hull),armor.chemicalMultiplier,hullAngle,0,1,false,settings.ricochets()).damage();}

   }
   if(!RunningGear.wheel(hit.part())&&settings.components()&&result.damagePercent()>0&&hits.stream().anyMatch(h->h.part()==com.atsuishio.superbwarfare.tools.OBB.Part.MAIN_ENGINE||h.part()==com.atsuishio.superbwarfare.tools.OBB.Part.SUB_ENGINE))finalDamage*=armor.engineDamageMultiplier;
   if(armor.aircraft){
    boolean engineHit=!RunningGear.wheel(hit.part())&&settings.components()&&result.damagePercent()>0&&hits.stream().anyMatch(h->AircraftBalance.engine(h.part()));
    if(engineHit){tooltip.add(Component.literal("Est. Engine Damage: "+StatText.number(finalDamage)+" HP").withStyle(net.minecraft.ChatFormatting.YELLOW));finalDamage*=armor.aircraftEngineHullFraction;}
    else finalDamage*=AircraftBalance.hullMultiplier(armor,ammo,false);
   }
   tooltip.add(Component.literal("Est. Hull Damage: "+StatText.number(finalDamage)+" HP ("+StatText.number(raw>0?finalDamage/raw*100:0)+"%)").withStyle(finalDamage>0?net.minecraft.ChatFormatting.GOLD:net.minecraft.ChatFormatting.RED));
   if(RunningGear.wheel(hit.part()))tooltip.add(Component.literal("Est. "+gear+" Damage: "+StatText.number(wheelDamage)+" HP").withStyle(net.minecraft.ChatFormatting.YELLOW));
   tooltip.add(Component.literal((RunningGear.wheel(hit.part())?gear+" Pen: ":"Pen: ")+StatText.number(result.penetrationChance())+"% | Ricochet: "+StatText.number(result.ricochetChance())+"%").withStyle(net.minecraft.ChatFormatting.AQUA));
   if(!details)return;
   tooltip.add(Component.literal("Armor").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
   for(var line:VehicleStats.armor(armor,true))tooltip.add(line.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
   tooltip.add(Component.literal("Impact: "+StatText.number(angle)+"\u00b0 | Armor: "+StatText.number(result.armor())+" mm").withStyle(net.minecraft.ChatFormatting.GRAY));
   tooltip.add(Component.literal("Weapon").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
   tooltip.add(Component.literal("Damage: "+StatText.number(raw)+" HP | Pen: "+StatText.number(result.penetration())+" mm").withStyle(net.minecraft.ChatFormatting.GRAY));
   if(mounted!=null){var gun=MountedWeapons.vehicle(player).getGunData(player);double blast=gun.get(com.atsuishio.superbwarfare.data.gun.GunProp.EXPLOSION_DAMAGE);if(blast>0)tooltip.add(Component.literal("Blast: "+StatText.number(blast)+" HP | "+StatText.number(gun.get(com.atsuishio.superbwarfare.data.gun.GunProp.EXPLOSION_RADIUS))+"m | Pen: "+StatText.number(ammo.blastPenetration)+" mm").withStyle(net.minecraft.ChatFormatting.GRAY));}
  }
 }
}
