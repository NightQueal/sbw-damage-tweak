package civilized.sbw.mixin;
import civilized.sbw.TaczExplosionAccess;
import com.tacz.guns.util.block.ProjectileExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ProjectileExplosion.class,remap=false)
public interface TaczExplosionAccessor extends TaczExplosionAccess {
 @Accessor(value="power",remap=false) float civilized$getDamage();
 @Accessor(value="radius",remap=false) float civilized$getRadius();
}
