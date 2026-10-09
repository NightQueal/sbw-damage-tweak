package civilized.sbw;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.nbt.CompoundTag;
public final class VehicleStats {
 public record Preview(double angle,double armor,double penetration,double penetrationChance,double ricochetChance,double damagePercent) {}
 public static String itemVehicle(ItemStack stack) {
  if(stack.getItem() instanceof SpawnEggItem egg)return String.valueOf(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(egg.getType(stack.getTag())));
  var tag=stack.getTag();if(tag==null)return "";
  String id=entityId(tag);if(!id.isEmpty())return id;
  return entityId(tag.getCompound("BlockEntityTag"));
 }
 private static String entityId(CompoundTag tag){if(tag.contains("EntityType",8))return tag.getString("EntityType");return tag.getCompound("Entity").getString("id");}
 public static List<Component> armor(ArmorProfiles.Vehicle v,boolean details) {
  var out=new ArrayList<Component>();out.add(Component.literal("Armor F/S/R: "+StatText.number(v.front)+" / "+StatText.number(v.side)+" / "+StatText.number(v.rear)+" mm"));
  if(details){
   out.add(Component.literal("Roof / Bottom: "+StatText.number(v.roof)+" / "+StatText.number(v.bottom)+" mm"));
   out.add(Component.literal("Tracks / Wheels: "+StatText.number(v.trackArmor)+" mm"));
   out.add(Component.literal("HEAT Protection: "+StatText.number(v.chemicalMultiplier)+"x"));
   out.add(Component.literal("Turret Armor: "+StatText.number(v.turretMultiplier)+"x"));
   out.add(Component.literal("Engine Damage: "+StatText.number(v.engineDamageMultiplier)+"x"));
  }return out;
 }
 public static Preview preview(ArmorProfiles.Ammo ammo,double plate,double chemical,double angle,boolean ricochets) {
  var result=ArmorModel.resolve(ammo,100,plate,chemical,angle,ammo.referenceSpeed,1,false,ricochets);
  boolean overmatch=!ammo.type.equals("HEAT")&&!ammo.type.equals("HE")&&plate>0&&ammo.caliber/plate>=ammo.overmatchRatio;
  double probability=ammo.ricochetEnd==ammo.ricochetStart?(angle>=ammo.ricochetEnd?1:0):Math.max(0,Math.min(1,(angle-ammo.ricochetStart)/(ammo.ricochetEnd-ammo.ricochetStart)));
  double bounce=ricochets&&!overmatch?probability:0;
  return new Preview(angle,result.effectiveArmor(),result.penetration(),result.penetrated()?100*(1-bounce):0,100*bounce,result.damage());
 }
}
