package civilized.sbw.mixin;
import civilized.sbw.RunningGear;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=VehicleEntity.class,remap=false)
public abstract class RunningGearMixin {
 @Inject(method="onHurt",at=@At("HEAD"))
 private void runningGear(float damage,Entity attacker,boolean flag,CallbackInfo ci){RunningGear.commit((VehicleEntity)(Object)this);}
}
