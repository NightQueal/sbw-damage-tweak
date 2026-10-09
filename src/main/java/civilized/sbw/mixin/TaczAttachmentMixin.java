package civilized.sbw.mixin;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.pojo.data.gun.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityKineticBullet.class)
public abstract class TaczAttachmentMixin {
 @Inject(method="<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",at=@At("RETURN"),remap=false)
 private void capture(EntityType<? extends Projectile> type,Level level,LivingEntity owner,ItemStack stack,ResourceLocation gunId,ResourceLocation displayId,ResourceLocation ammoId,boolean tracer,GunData gunData,BulletData bulletData,CallbackInfo ci) {
  IGun gun=IGun.getIGunOrNull(stack);if(gun==null)return;
  ResourceLocation id=gun.getAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  if(id==null||civilized.sbw.ArmorProfiles.attachment(id.toString())==null)id=gun.getBuiltInAttachmentId(stack,AttachmentType.EXTENDED_MAG);
  if(id!=null)((EntityKineticBullet)(Object)this).getPersistentData().putString("civilized_sbw_ammo_attachment",id.toString());
 }
}
