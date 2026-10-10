package civilized.sbw.mixin;
import civilized.sbw.RunningGear;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Remove powered acceleration and steering; preserve gravity, towing and momentum. */
@Mixin(value=VehicleEngineUtils.class,remap=false)
public abstract class RunningGearEngineMixin {
 @Redirect(method="trackEngine",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;getTargetSpeed()D"))
 private static double speed(VehicleEntity v){return RunningGear.immobilized(v)?0:v.getTargetSpeed();}
 @Redirect(method="trackEngine",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;getPower()F"))
 private static float power(VehicleEntity v){return RunningGear.immobilized(v)?0:v.getPower();}
 @Redirect(method="trackEngine",at=@At(value="INVOKE",target="Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;getDeltaRot()F"))
 private static float turn(VehicleEntity v){return RunningGear.immobilized(v)?0:v.getDeltaRot();}
}
