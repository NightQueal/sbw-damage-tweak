package civilized.sbw.mixin;
import civilized.sbw.DamageCompatibility;
import civilized.sbw.ArmorModel;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
 @Inject(method="compute",at=@At("RETURN"),cancellable=true,remap=false)
 private void civilized$armor(Entity entity,DamageSource source,float input,CallbackInfoReturnable<Float> cir) {
  Float result=ArmorModel.compute(entity,source,input);if(result!=null)cir.setReturnValue(result);
 }
}

