package civilized.sbw;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import java.util.*;
@Mod.EventBusSubscriber(modid="civilized_sbw",value=Dist.CLIENT)
public final class VehicleTooltips {
 private static ItemStack cachedStack=ItemStack.EMPTY;
 private static long cacheTime;
 private static com.tacz.guns.resource.modifier.AttachmentCacheProperty cachedProperties;
 private static com.tacz.guns.resource.modifier.AttachmentCacheProperty properties(ItemStack stack,com.tacz.guns.resource.pojo.data.gun.GunData data) {
  long now=System.nanoTime();if(cachedProperties==null||now-cacheTime>1_000_000_000L||!ItemStack.matches(stack,cachedStack)) {
   var next=new com.tacz.guns.resource.modifier.AttachmentCacheProperty();next.eval(stack,data);cachedProperties=next;cachedStack=stack.copy();cacheTime=now;
  }return cachedProperties;
 }
 private static String n(double d){return String.format(Locale.ROOT,"%.1f",d);}
 private static void line(List<Component> out,String s){out.add(Component.literal(s).withStyle(ChatFormatting.GRAY));}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){ProfileSync.client=null;ProfileSync.clientProfiles=null;cachedProperties=null;cachedStack=ItemStack.EMPTY;}
 public static double damageAt(ItemStack stack,double distance) {
  var gun=IGun.getIGunOrNull(stack);if(gun==null)return 0;
  var index=TimelessAPI.getCommonGunIndex(gun.getGunId(stack));if(index.isEmpty())return 0;
  var cache=properties(stack,index.get().getGunData());int i=0;double last=0;
  for(var pair:cache.getCache(com.tacz.guns.api.GunProperties.DAMAGE)){
   last=pair.getDamage();double end=i++==0?cache.getCache(com.tacz.guns.api.GunProperties.EFFECTIVE_RANGE):pair.getDistance();if(distance<end)return last;
  }return last;
 }
 public static ArmorProfiles.Ammo gunProfile(ItemStack stack) {
  var data=ProfileSync.clientProfiles;var settings=ProfileSync.client;
  if(data==null||settings==null||!settings.enabled())return null;
  IGun gun=IGun.getIGunOrNull(stack);if(gun==null)return null;
  var index=TimelessAPI.getCommonGunIndex(gun.getGunId(stack));if(index.isEmpty())return null;
  var base=data.ammunition.get("gun:"+gun.getGunId(stack));
  if(base==null)base=data.ammunition.get("ammo:"+index.get().getGunData().getAmmoId());
  var id=gun.getAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  if(id==null||!data.attachments.containsKey(id.toString()))id=gun.getBuiltInAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  return ArmorProfiles.modified(base,id==null?"":id.toString(),data);
 }
 public static List<Component> attachmentStats(String id) {
  var data=ProfileSync.clientProfiles;var settings=ProfileSync.client;
  if(data==null||settings==null||!settings.enabled())return List.of();
  var mod=data.attachments.get(id);if(mod==null)return List.of();
  List<Component> rows=new ArrayList<>();
  modifier(rows,"Vehicle Penetration",mod.penetrationMultiplier);
  modifier(rows,"Blast Penetration",mod.blastPenetrationMultiplier);
  if(mod.normalizationAdd!=0)rows.add(Component.literal("+"+StatText.number(mod.normalizationAdd)+"\u00b0 Normalization").withStyle(ChatFormatting.GREEN));
  modifier(rows,"Post-Penetration Damage",mod.postPenMultiplier);
  var player=net.minecraft.client.Minecraft.getInstance().player;
  if(player!=null) {
   var stack=player.getMainHandItem();var gun=IGun.getIGunOrNull(stack);
   if(gun!=null) {
    var item=com.tacz.guns.api.item.builder.AttachmentItemBuilder.create().setId(new net.minecraft.resources.ResourceLocation(id)).build();
    if(gun.allowAttachment(stack,item))TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).ifPresent(index->{
     var base=data.ammunition.get("gun:"+gun.getGunId(stack));if(base==null)base=data.ammunition.get("ammo:"+index.getGunData().getAmmoId());
     if(base!=null) {
      var changed=ArmorProfiles.modified(base,id,data);
      line(rows,"Vehicle Penetration: "+StatText.number(base.penetration)+" -> "+StatText.number(changed.penetration)+" mm");
      if(base.blastPenetration!=changed.blastPenetration)line(rows,"Blast Penetration: "+StatText.number(base.blastPenetration)+" -> "+StatText.number(changed.blastPenetration)+" mm");
     }
    });
   }
  }
  return rows;
 }
 private static void modifier(List<Component> rows,String name,double value) {
  if(Math.abs(value-1)<.00001)return;
  rows.add(Component.literal(StatText.change(value)+" "+name).withStyle(value>1?ChatFormatting.GREEN:ChatFormatting.RED));
 }
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
  ItemStack stack=e.getItemStack();
  var profiles=ProfileSync.clientProfiles;var settings=ProfileSync.client;
  if(profiles!=null&&settings!=null&&settings.enabled()) {
   var armor=profiles.vehicles.get(VehicleStats.itemVehicle(stack));
   if(armor!=null){e.getToolTip().addAll(VehicleStats.armor(armor,Screen.hasShiftDown()));return;}
  }
  IGun gun=IGun.getIGunOrNull(stack);if(gun==null)return;
  var ammo=gunProfile(stack);if(ammo==null)return;
  var out=e.getToolTip();
  line(out,"Vehicle Penetration: "+StatText.number(ammo.penetration)+" mm");
  if(ammo.blastPenetration>0)line(out,"Blast Penetration: "+StatText.number(ammo.blastPenetration)+" mm");
  if(!Screen.hasShiftDown())return;
  if(ammo.normalization!=0)line(out,"Normalization: "+StatText.number(ammo.normalization)+"\u00b0");
  if(ProfileSync.client.ricochets())line(out,"Ricochet Angle: "+StatText.number(ammo.ricochetStart)+"-"+StatText.number(ammo.ricochetEnd)+"\u00b0");
  line(out,"Caliber: "+StatText.number(ammo.caliber)+" mm");
  line(out,"Overmatch (Caliber/Armor): "+StatText.number(ammo.overmatchRatio)+"x");
  if(ammo.postPenMultiplier!=1)line(out,"Post-Penetration Damage: "+StatText.number(ammo.postPenMultiplier)+"x");
  if(ammo.blastLeak>0)line(out,"Blocked Blast Damage: "+StatText.number(ammo.blastLeak*100)+"%");
  var index=TimelessAPI.getCommonGunIndex(gun.getGunId(stack));if(index.isEmpty())return;
  try {
   var cache=properties(stack,index.get().getGunData());var pairs=cache.getCache(com.tacz.guns.api.GunProperties.DAMAGE);
   double low=0;int count=0;for(var pair:pairs) {
    if(count++>=4)break;double high=count==1?cache.getCache(com.tacz.guns.api.GunProperties.EFFECTIVE_RANGE):pair.getDistance();
    line(out,"Damage ("+StatText.range(low,high)+"): "+StatText.number(pair.getDamage())+" HP");low=high;
   }
   var explosion=cache.getCache(com.tacz.guns.api.GunProperties.EXPLOSION);
   if(explosion!=null&&explosion.isExplode())line(out,"Blast Damage: "+StatText.number(explosion.getDamage())+" HP | Radius: "+StatText.number(explosion.getRadius())+"m");
  }catch(RuntimeException ignored){}
 }
}
