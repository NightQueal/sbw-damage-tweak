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
  public Map<String,Attachment> attachments=defaultAttachments();
 }
 public static final class Attachment {
  public String type="FMJ";
  public double penetrationMultiplier=1,blastPenetrationMultiplier=1,normalizationAdd=0,postPenMultiplier=1;
 }
 private static Map<String,Attachment> defaultAttachments() {
  var map=new java.util.LinkedHashMap<String,Attachment>();
  String[] ids={"fmj","hp","i","he","slug"};String[] types={"FMJ","HP","INCENDIARY","HE","FMJ"};double[] penetration={1.35,.45,.85,.6,1.2};
  for(int i=0;i<ids.length;i++){Attachment a=new Attachment();a.type=types[i];a.penetrationMultiplier=penetration[i];if(i==0)a.normalizationAdd=3;if(i==3)a.blastPenetrationMultiplier=3;map.put("tacz:ammo_mod_"+ids[i],a);}return map;
 }
 public static Attachment attachment(String id){return data.attachments.get(id);}
 public static Ammo modified(Ammo base,String id) {
  return modified(base,id,data);
 }
 public static Ammo modified(Ammo base,String id,Data source) {
  Attachment mod=source.attachments.get(id);if(base==null||mod==null||base.type.equals("HEAT"))return base;
  Ammo a=new Ammo();a.type=base.type;a.penetration=base.penetration;a.caliber=base.caliber;a.referenceSpeed=base.referenceSpeed;a.velocityExponent=base.velocityExponent;a.normalization=base.normalization;a.ricochetStart=base.ricochetStart;a.ricochetEnd=base.ricochetEnd;a.overmatchRatio=base.overmatchRatio;a.postPenMultiplier=base.postPenMultiplier;a.blastPenetration=base.blastPenetration;a.blastLeak=base.blastLeak;
  a.type=mod.type;a.penetration*=mod.penetrationMultiplier;a.blastPenetration*=mod.blastPenetrationMultiplier;
  a.normalization=Math.min(45,a.normalization+mod.normalizationAdd);a.postPenMultiplier*=mod.postPenMultiplier;return a;
 }
 public static String json(){return new Gson().toJson(data);}
 private static Data data=new Data();
 public static Vehicle vehicle(String id){return data.vehicles.get(id);}
 public static Ammo ammo(String id){return data.ammunition.get(id);}
 public static java.util.Set<String> vehicleKeys(){return java.util.Set.copyOf(data.vehicles.keySet());}
 public static java.util.Set<String> ammoKeys(){return java.util.Set.copyOf(data.ammunition.keySet());}
 public static void load() {
  Path file=FMLPaths.CONFIGDIR.get().resolve("civilized-sbw-armor.json");
  try {
   Files.createDirectories(file.getParent());
   if(!Files.exists(file))try(var in=ArmorProfiles.class.getResourceAsStream("/civilized-sbw-armor.json")){if(in==null)throw new IllegalStateException("Missing default armor profiles");Files.copy(in,file);}
   var original=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
   Data parsed=new Gson().fromJson(original,Data.class);
   if(parsed==null||parsed.vehicles==null||parsed.ammunition==null)throw new IllegalArgumentException("Missing profile maps");
   for(var entry:parsed.vehicles.entrySet()) {
    Vehicle v=entry.getValue();if(v==null)throw new IllegalArgumentException(entry.getKey());
    for(double n:new double[]{v.front,v.side,v.rear,v.roof,v.bottom,v.chemicalMultiplier,v.turretMultiplier,v.engineDamageMultiplier,v.trackArmor})range(n,0,10000);
   }
   for(var entry:parsed.ammunition.entrySet()) {
    Ammo a=entry.getValue();if(a==null||!java.util.Set.of("FMJ","AP","APC","APCR","APHE","HE","HEAT","HP","INCENDIARY").contains(a.type))throw new IllegalArgumentException("Invalid ammunition "+entry.getKey());
    for(double n:new double[]{a.penetration,a.caliber,a.referenceSpeed,a.postPenMultiplier,a.blastPenetration})range(n,0,10000);
    range(a.velocityExponent,0,4);range(a.normalization,0,45);range(a.ricochetStart,0,90);range(a.ricochetEnd,0,90);range(a.overmatchRatio,1,100);range(a.blastLeak,0,1);
    if(a.ricochetEnd<a.ricochetStart)throw new IllegalArgumentException("Reversed ricochet angles");
   }
   if(parsed.attachments==null)throw new IllegalArgumentException("Missing attachment profiles");
   for(var entry:parsed.attachments.entrySet()) {
    Attachment a=entry.getValue();if(a==null||!java.util.Set.of("FMJ","AP","APC","APCR","APHE","HE","HP","INCENDIARY").contains(a.type))throw new IllegalArgumentException("Invalid attachment "+entry.getKey());
    range(a.penetrationMultiplier,0,100);range(a.blastPenetrationMultiplier,0,100);range(a.normalizationAdd,0,45);range(a.postPenMultiplier,0,100);
   }
   if(!original.has("attachments")) {
    original.add("attachments",new Gson().toJsonTree(parsed.attachments));
    Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(original)+"\n");
   }
   data=parsed;
  } catch(Exception e){throw new IllegalStateException("Cannot load "+file+": "+e.getMessage(),e);}
 }
 private static void range(double n,double lo,double hi){if(!Double.isFinite(n)||n<lo||n>hi)throw new IllegalArgumentException("Profile number out of range: "+n);}
}
