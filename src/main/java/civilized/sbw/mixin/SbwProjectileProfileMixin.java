package civilized.sbw.mixin;
import civilized.sbw.MountedWeapons;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.data.gun.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** SBW 0.8.9.1 factory hook, before world insertion and before any hit. */
@Mixin(value=GunItem.class,remap=false)
public abstract class SbwProjectileProfileMixin {
 @Inject(method="shootBullet$lambda$19",at=@At("RETURN"),remap=false)
 private static void civilized$capture(ServerLevel level,String projectileId,Entity shooter,ItemStack stack,GunData gun,UUID target,Vec3 targetPos,ProjectileInfo info,double damage,AtomicReference<Entity> holder,double headshot,boolean zoom,double bypass,float velocity,EntityType<?> type,CallbackInfoReturnable<kotlin.Unit> cir) {
  if(holder.get()!=null)MountedWeapons.capture(holder.get(),gun);
 }
}
