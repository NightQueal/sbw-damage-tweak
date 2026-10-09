package civilized.sbw;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
/** Read-only ray against loaded entities, never changes interaction or weapon reach. */
public final class VehicleInspectionTarget {
 public static EntityHitResult trace(Level level,Entity viewer,Vec3 start,Vec3 direction,double range) {
  if(range<=0||direction.lengthSqr()<1e-9)return null;direction=direction.normalize();Vec3 end=start.add(direction.scale(range));
  var block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer));
  double limit=block.getType()==HitResult.Type.MISS?range:start.distanceTo(block.getLocation());
  EntityHitResult selected=null;double nearest=limit;
  for(var entity:level.getEntities(viewer,new AABB(start,start.add(direction.scale(limit))).inflate(4),e->e instanceof VehicleEntity&&e.isAlive())) {
   if(entity==MountedWeapons.vehicle(viewer))continue;
   var hits=ArmorModel.trace((VehicleEntity)entity,start,direction,limit);
   if(!hits.isEmpty()&&hits.get(0).distance()<nearest){nearest=hits.get(0).distance();selected=new EntityHitResult(entity,hits.get(0).point());}
  }return selected;
 }
}
