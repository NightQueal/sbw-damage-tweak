package civilized.sbw;
import snownee.jade.api.*;
import net.minecraft.client.Minecraft;
import com.tacz.guns.api.item.IGun;
public final class LongRangeVehicleInspection {
 public static void register(IWailaClientRegistration registration) {
  registration.addRayTraceCallback((hit,current,original)->{
   var mc=Minecraft.getInstance();var player=mc.player;
   if(player==null||mc.level==null)return current;
   var settings=ProfileSync.client;var profiles=ProfileSync.clientProfiles;
   var mounted=profiles==null?null:MountedWeapons.inspect(player,profiles,settings!=null&&settings.mounted());
   if(mounted==null&&IGun.getIGunOrNull(player.getMainHandItem())==null)return current;
   var target=VehicleInspectionTarget.trace(mc.level,player,mounted==null?player.getEyePosition():mounted.origin(),mounted==null?player.getLookAngle():mounted.direction(),CompatConfig.INSPECTION_RANGE.get());
   if(target==null)return current;
   if(current instanceof EntityAccessor existing&&existing.getEntity()==target.getEntity())return current;
   return registration.entityAccessor().level(mc.level).player(player).entity(target.getEntity()).hit(target)
    .serverData(new net.minecraft.nbt.CompoundTag()).serverConnected(false).showDetails(registration.isShowDetailsPressed()).build();
  });
 }
}
