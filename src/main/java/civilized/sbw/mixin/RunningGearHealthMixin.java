package civilized.sbw.mixin;
import civilized.sbw.RunningGear;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEffectUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value=VehicleEffectUtils.class,remap=false)
public abstract class RunningGearHealthMixin {
 @Redirect(method="handlePartHealth",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;setLeftWheelHealth(F)V"))
 private static void left(VehicleEntity v,float proposed){v.setLeftWheelHealth(RunningGear.regenerate(v,v.getLeftWheelHealth(),proposed));}
 @Redirect(method="handlePartHealth",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;setRightWheelHealth(F)V"))
 private static void right(VehicleEntity v,float proposed){v.setRightWheelHealth(RunningGear.regenerate(v,v.getRightWheelHealth(),proposed));}
}
