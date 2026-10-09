package civilized.sbw.mixin;
import civilized.sbw.DamageCompatibility;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(value=DamageModifier.class,remap=false)
public abstract class VehicleInputMixin {
 @ModifyVariable(method="compute",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
 private float civilized$scale(float input,Entity entity,DamageSource source,float original) {
  return DamageCompatibility.directInput(input,entity,source);
 }
}
