package civilized.sbw;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraftforge.fml.loading.FMLPaths;
/** Explicit, editable gameplay estimates. No third-party implementation is copied. */
public final class ArmorProfiles {
 public static final class Vehicle {
  public double front=10,side=8,rear=6,roof=4,bottom=4,chemicalMultiplier=1,turretMultiplier=1,engineDamageMultiplier=1,trackArmor=8;
 }
 public static final class Ammo {
  public String type="AP";
  public double penetration=20,caliber=7.62,referenceSpeed=0,velocityExponent=1.43,normalization=0,ricochetStart=60,ricochetEnd=75,overmatchRatio=7,postPenMultiplier=1,blastPenetration=2,blastLeak=0;
 }
 public static final class Data {
  public String notes="Gameplay RHA-equivalent estimates, not measured/classified armor. Restart to reload.";
  public Map<String,Vehicle> vehicles=Map.of();
  public Map<String,Ammo> ammunition=Map.of();
 }
 private static Data data=new Data();
 public static Vehicle vehicle(String id){return data.vehicles.get(id);}
 public static Ammo ammo(String id){return data.ammunition.get(id);}
 public static void load() {
  Path file=FMLPaths.CONFIGDIR.get().resolve("civilized-sbw-armor.json");
  try {
   Files.createDirectories(file.getParent());
   if(!Files.exists(file))try(var in=ArmorProfiles.class.getResourceAsStream("/civilized-sbw-armor.json")){if(in==null)throw new IllegalStateException("Missing default armor profiles");Files.copy(in,file);}
   Data parsed=new Gson().fromJson(Files.readString(file),Data.class);
   if(parsed==null||parsed.vehicles==null||parsed.ammunition==null)throw new IllegalArgumentException("Missing profile maps");
   for(var entry:parsed.vehicles.entrySet()) {
    Vehicle v=entry.getValue();if(v==null)throw new IllegalArgumentException(entry.getKey());
    for(double n:new double[]{v.front,v.side,v.rear,v.roof,v.bottom,v.chemicalMultiplier,v.turretMultiplier,v.engineDamageMultiplier,v.trackArmor})range(n,0,10000);
   }
   for(var entry:parsed.ammunition.entrySet()) {
    Ammo a=entry.getValue();if(a==null||!java.util.Set.of("FMJ","AP","APC","APCR","APHE","HE","HEAT").contains(a.type))throw new IllegalArgumentException("Invalid ammunition "+entry.getKey());
    for(double n:new double[]{a.penetration,a.caliber,a.referenceSpeed,a.postPenMultiplier,a.blastPenetration})range(n,0,10000);
    range(a.velocityExponent,0,4);range(a.normalization,0,45);range(a.ricochetStart,0,90);range(a.ricochetEnd,0,90);range(a.overmatchRatio,1,100);range(a.blastLeak,0,1);
    if(a.ricochetEnd<a.ricochetStart)throw new IllegalArgumentException("Reversed ricochet angles");
   }
   data=parsed;
  } catch(Exception e){throw new IllegalStateException("Cannot load "+file+": "+e.getMessage(),e);}
 }
 private static void range(double n,double lo,double hi){if(!Double.isFinite(n)||n<lo||n>hi)throw new IllegalArgumentException("Profile number out of range: "+n);}
}
