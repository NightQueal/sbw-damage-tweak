package civilized.sbw.mixin;
import civilized.sbw.CompatConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractCannonProjectile.class,remap=false)
public abstract class CbcImpactMixin {
 @Inject(method="onHitEntity",at=@At("RETURN"),remap=false)
 private void civilized$once(Entity target,ProjectileContext context,CallbackInfoReturnable<Boolean> cir) {
  if(target instanceof VehicleEntity && CompatConfig.CBC_ATTRIBUTION.get() && CompatConfig.CBC_SINGLE_HIT.get())((AbstractCannonProjectile)(Object)this).addAlwaysUntouchableEntity(target);
 }
}
