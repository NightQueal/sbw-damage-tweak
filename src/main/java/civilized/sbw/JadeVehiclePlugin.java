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
   boolean details=accessor.showDetails()||(accessor.getPlayer()!=null&&MountedWeapons.vehicle(accessor.getPlayer())!=null&&CompatConfig.MOUNTED_DETAILS.get());
   tooltip.addAll(VehicleStats.armor(armor,details));if(!details)return;
   var player=accessor.getPlayer();if(player==null)return;
   var mounted=MountedWeapons.inspect(player,profiles,settings.mounted());
   if(mounted!=null&&mounted.weapon().isEmpty())mounted=null;
   var ammo=mounted==null?VehicleTooltips.gunProfile(player.getMainHandItem()):mounted.ammo();if(ammo==null)return;
   if(mounted!=null){tooltip.add(Component.translatable(mounted.weapon(), ""));tooltip.add(Component.literal("Ammo: ").append(mounted.ammunition().startsWith("superbwarfare:")?Component.translatable("item.superbwarfare."+mounted.ammunition().substring(14)):Component.literal(mounted.ammunition())));}
   Vec3 start=mounted==null?player.getEyePosition():mounted.origin();Vec3 end=accessor.getHitResult().getLocation();Vec3 delta=end.subtract(start);
   if(delta.lengthSqr()<1e-9)return;Vec3 direction=mounted==null?delta.normalize():mounted.direction().normalize();var hits=ArmorModel.trace(v,start,direction,delta.length()+8);if(hits.isEmpty())return;
   var hit=hits.get(0);double angle=Math.toDegrees(Math.acos(Math.max(0,Math.min(1,-direction.dot(hit.normal())))));
   var result=VehicleStats.preview(ammo,ArmorModel.plate(armor,v,hit),armor.chemicalMultiplier,angle,settings.ricochets());
   tooltip.add(Component.literal("Impact Angle: "+StatText.number(angle)+"\u00b0"));
   tooltip.add(Component.literal("Effective Armor: "+StatText.number(result.armor())+" mm"));
   tooltip.add(Component.literal("Nominal Penetration: "+StatText.number(result.penetration())+" mm"));
   if(mounted!=null&&ammo.blastPenetration>0)tooltip.add(Component.literal("Blast Penetration: "+StatText.number(ammo.blastPenetration)+" mm"));
   tooltip.add(Component.literal("Pen. Chance: "+StatText.number(result.penetrationChance())+"%"));
   tooltip.add(Component.literal("Ricochet Chance: "+StatText.number(result.ricochetChance())+"%"));
   tooltip.add(Component.literal("Post-Pen. Damage: "+StatText.number(result.damagePercent())+"%"));
  }
 }
}
