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
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e) {
  ItemStack stack=e.getItemStack();IGun gun=IGun.getIGunOrNull(stack);IAttachment attachment=IAttachment.getIAttachmentOrNull(stack);
  if(gun==null&&attachment==null)return;
  var data=ProfileSync.clientProfiles;var settings=ProfileSync.client;
  if(data==null||settings==null){line(e.getToolTip(),"Vehicle armor stats: connect to a server to load settings.");return;}
  var out=e.getToolTip();
  if(attachment!=null) {
   String id=attachment.getAttachmentId(stack).toString();var mod=data.attachments.get(id);if(mod==null)return;
   out.add(Component.literal("Vehicle ammunition: "+mod.type).withStyle(ChatFormatting.GOLD));
   line(out,"Penetration x"+n(mod.penetrationMultiplier)+" | Blast penetration x"+n(mod.blastPenetrationMultiplier));
   line(out,"Normalization +"+n(mod.normalizationAdd)+" deg | Post-penetration damage x"+n(mod.postPenMultiplier));
   TimelessAPI.getCommonAttachmentIndex(attachment.getAttachmentId(stack)).ifPresent(index->{
    for(String key:new String[]{"damage","ammo_speed","effective_range","rpm"}) {
     var prop=index.getData().getModifier().get(key);
     if(prop!=null&&prop.getValue() instanceof com.tacz.guns.resource.pojo.data.attachment.Modifier m)
      line(out,"TACZ "+key+": x"+n(m.getMultiplier())+", add "+n(m.getAddend())+", percent "+n(m.getPercent()));
    }
   });
   line(out,"TACZ damage/speed effects apply separately, once.");
   line(out,"HEAT rounds retain their shaped-charge profile.");
   if(!settings.enabled())line(out,"Advanced vehicle armor is OFF on this server.");return;
  }
  var index=TimelessAPI.getCommonGunIndex(gun.getGunId(stack));if(index.isEmpty())return;
  String key="gun:"+gun.getGunId(stack);var base=data.ammunition.get(key);
  if(base==null){key="ammo:"+index.get().getGunData().getAmmoId();base=data.ammunition.get(key);}
  if(base==null){line(out,"Vehicle armor: unmapped ammunition; native SBW rules.");return;}
  var id=gun.getAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  if(id==null||!data.attachments.containsKey(id.toString()))id=gun.getBuiltInAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  var ammo=ArmorProfiles.modified(base,id==null?"":id.toString(),data);
  out.add(Component.literal("Vehicle armor: "+ammo.type).withStyle(ChatFormatting.GOLD));
  if(!settings.enabled()){line(out,"Advanced vehicle armor is OFF; values below are inactive.");}
  line(out,"Penetration "+n(ammo.penetration)+" mm | Blast "+n(ammo.blastPenetration)+" mm");
  line(out,"At 0 deg; angle is measured from the surface normal.");
  if(id!=null&&data.attachments.containsKey(id.toString()))line(out,"Installed ammunition: "+id);
  if(!Screen.hasShiftDown()){line(out,"Hold Shift for angle, component and vehicle examples.");return;}
  line(out,"Normalization "+n(ammo.normalization)+" deg | Post-penetration damage x"+n(ammo.postPenMultiplier));
  line(out,"Ricochet "+(settings.ricochets()?n(ammo.ricochetStart)+"-"+n(ammo.ricochetEnd)+" deg":"OFF")+" | Overmatch caliber/plate >= "+n(ammo.overmatchRatio));
  line(out,"Blast leak on blocked armor: "+n(ammo.blastLeak*100)+"%");
  line(out,ammo.referenceSpeed>0?"Penetration scales with impact speed (exponent "+n(ammo.velocityExponent)+").":"Penetration fixed with range; TACZ damage still has falloff.");
  line(out,"Engines: "+(settings.components()?"penetrating ray must intersect engine; vehicle multiplier applies.":"component routing OFF."));
  if(ammo.type.equals("HEAT"))line(out,"HEAT: chemical armor applies; full jet only along rocket impact.");
  if(ammo.type.equals("INCENDIARY"))line(out,"Ignition follows TACZ; no additional vehicle fire damage.");
  if(ammo.type.equals("HE"))line(out,"Explosion damage/radius come from TACZ gun data.");
  try {
   var cache=properties(stack,index.get().getGunData());
   var pairs=cache.getCache(com.tacz.guns.api.GunProperties.DAMAGE);
   float firstRange=cache.getCache(com.tacz.guns.api.GunProperties.EFFECTIVE_RANGE);
   int count=0;
   for(var pair:pairs) {if(count++>=4){line(out,"Additional damage bands omitted.");break;}float distance=count==1?firstRange:pair.getDistance();
    line(out,"TACZ "+n(pair.getDamage())+" HP "+(Float.isInfinite(distance)?"in the final range band":("below "+n(distance)+" blocks")));
   }
   line(out,"TACZ launch speed: "+n(cache.getCache(com.tacz.guns.api.GunProperties.AMMO_SPEED))+" m/s");
   var explosion=cache.getCache(com.tacz.guns.api.GunProperties.EXPLOSION);
   if(explosion!=null&&explosion.isExplode())line(out,"TACZ blast: "+n(explosion.getDamage())+" HP, radius "+n(explosion.getRadius())+" (before exposure/armor).");
   line(out,"Gun-pack estimates exclude shooter scripts, pellet splitting and hit location.");
  }catch(RuntimeException ex){line(out,"TACZ live damage preview unavailable; vehicle profile remains valid.");}
  line(out,"Examples: % of incoming direct damage, hull side at 0/30/60 deg.");
  for(String vehicle:new String[]{"truck","bmp_2","m_1a_2"}) {
   var v=data.vehicles.get("superbwarfare:"+vehicle);if(v==null)continue;
   StringBuilder values=new StringBuilder();for(int angle:new int[]{0,30,60}) {
    var r=ArmorModel.resolve(ammo,100,v.side,v.chemicalMultiplier,angle,ammo.referenceSpeed,1,false,settings.ricochets());
    if(values.length()>0)values.append(" / ");values.append(n(r.damage())).append("%");
   }
   line(out,vehicle+": "+values+" | engine x"+n(v.engineDamageMultiplier));
  }
  line(out,"Examples are non-ricochet hits; exclude engines, blast, cover and scaling.");
 }
}
