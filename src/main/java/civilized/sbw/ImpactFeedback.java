package civilized.sbw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraftforge.registries.ForgeRegistries;
public final class ImpactFeedback {
 public enum Outcome { PENETRATION, BLOCKED, RICOCHET }
 public static Outcome outcome(ArmorModel.Result result){return result.ricochet()?Outcome.RICOCHET:result.penetrated()?Outcome.PENETRATION:Outcome.BLOCKED;}
 /** One cue per projectile/vehicle, plus a per-vehicle rate limit for automatic fire. */
 public static boolean reserve(Entity vehicle,Entity projectile,long tick,int cooldown) {
  var shot=projectile.getPersistentData();String key="civilized_sbw_cue_"+vehicle.getUUID();
  if(shot.contains(key))return false;shot.putBoolean(key,true);
  var target=vehicle.getPersistentData();String last="civilized_sbw_last_cue";
  if(target.contains(last)&&tick>=target.getLong(last)&&tick-target.getLong(last)<cooldown)return false;
  target.putLong(last,tick);return true;
 }
 public static void emit(Entity vehicle,Projectile projectile,Vec3 point,ArmorModel.Result result) {
  if(!CompatConfig.IMPACT_SOUNDS.get()||!(vehicle.level() instanceof ServerLevel level))return;
  if(!reserve(vehicle,projectile,level.getGameTime(),CompatConfig.IMPACT_INTERVAL.get()))return;
  Outcome kind=outcome(result);
  String id=switch(kind){case PENETRATION->CompatConfig.PEN_SOUND.get();case BLOCKED->CompatConfig.BLOCK_SOUND.get();case RICOCHET->CompatConfig.RICOCHET_SOUND.get();};
  float pitch=(float)(double)switch(kind){case PENETRATION->CompatConfig.PEN_PITCH.get();case BLOCKED->CompatConfig.BLOCK_PITCH.get();case RICOCHET->CompatConfig.RICOCHET_PITCH.get();};
  SoundEvent sound=ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));if(sound==null)sound=SoundEvents.SHIELD_BLOCK;
  float volume=(float)(double)CompatConfig.IMPACT_VOLUME.get();
  ServerPlayer shooter=CompatConfig.SHOOTER_CUES.get()&&projectile.getOwner() instanceof ServerPlayer p?p:null;
  level.playSound(shooter,point.x,point.y,point.z,sound,SoundSource.PLAYERS,volume,pitch);
  if(shooter!=null)shooter.playNotifySound(sound,SoundSource.PLAYERS,volume,pitch);
 }
}
