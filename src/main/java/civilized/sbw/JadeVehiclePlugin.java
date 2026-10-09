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
   tooltip.addAll(VehicleStats.armor(armor,accessor.showDetails()));if(!accessor.showDetails())return;
   var player=accessor.getPlayer();if(player==null)return;
   var ammo=VehicleTooltips.gunProfile(player.getMainHandItem());if(ammo==null)return;
   Vec3 start=player.getEyePosition();Vec3 end=accessor.getHitResult().getLocation();Vec3 delta=end.subtract(start);
   if(delta.lengthSqr()<1e-9)return;Vec3 direction=delta.normalize();var hits=ArmorModel.trace(v,start,direction,delta.length()+8);if(hits.isEmpty())return;
   var hit=hits.get(0);double angle=Math.toDegrees(Math.acos(Math.max(0,Math.min(1,-direction.dot(hit.normal())))));
   var result=VehicleStats.preview(ammo,ArmorModel.plate(armor,v,hit),armor.chemicalMultiplier,angle,settings.ricochets());
   tooltip.add(Component.literal("Impact Angle: "+StatText.number(angle)+"\u00b0"));
   tooltip.add(Component.literal("Effective Armor: "+StatText.number(result.armor())+" mm"));
   tooltip.add(Component.literal("Nominal Penetration: "+StatText.number(result.penetration())+" mm"));
   tooltip.add(Component.literal("Pen. Chance: "+StatText.number(result.penetrationChance())+"%"));
   tooltip.add(Component.literal("Ricochet Chance: "+StatText.number(result.ricochetChance())+"%"));
   tooltip.add(Component.literal("Post-Pen. Damage: "+StatText.number(result.damagePercent())+"%"));
  }
 }
}
