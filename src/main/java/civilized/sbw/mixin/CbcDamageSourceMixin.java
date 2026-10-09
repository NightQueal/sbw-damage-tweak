package civilized.sbw.mixin;
import civilized.sbw.DamageCompatibility;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value={AbstractCannonProjectile.class,AbstractBigCannonProjectile.class},remap=false)
public abstract class CbcDamageSourceMixin {
 @Inject(method="getEntityDamage",at=@At("RETURN"),cancellable=true,remap=false)
 private void civilized$attribute(Entity target,CallbackInfoReturnable<DamageSource> cir) {
  cir.setReturnValue(DamageCompatibility.directSource((Entity)(Object)this,target,cir.getReturnValue()));
 }
}
