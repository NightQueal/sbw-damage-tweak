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
 @Redirect(method="handlePartHealth",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;setMainEngineHealth(F)V"))
 private static void main(com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity v,float proposed){v.setMainEngineHealth(civilized.sbw.AircraftBalance.regenerateEngine(v,v.getMainEngineHealth(),proposed));}
 @Redirect(method="handlePartHealth",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;setSubEngineHealth(F)V"))
 private static void sub(com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity v,float proposed){v.setSubEngineHealth(civilized.sbw.AircraftBalance.regenerateEngine(v,v.getSubEngineHealth(),proposed));}
}
