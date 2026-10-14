package civilized.sbw.mixin;
import civilized.sbw.DamageTuning;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
/** Both mapped development and Forge runtime names; also covers armor-bypassing rounds. */
@Mixin(value=VehicleEntity.class,remap=false)
public abstract class VehicleDamageTuningMixin {
 @ModifyVariable(method={"hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z","m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"},at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
 private float civilized$damageTuning(float input,DamageSource source,float original) {
  VehicleEntity target=(VehicleEntity)(Object)this;
  return target.level().isClientSide?input:DamageTuning.scale(input,DamageTuning.factor(target,source));
 }
}
