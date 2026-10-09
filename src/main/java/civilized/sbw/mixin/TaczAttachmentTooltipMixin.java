package civilized.sbw.mixin;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientAttachmentItemTooltip.class)
public abstract class TaczAttachmentTooltipMixin {
 @Shadow @Final private ResourceLocation attachmentId;
 @Shadow @Final private List<Component> components;
 @Inject(method="addText",at=@At("TAIL"),remap=false)
 private void vehicleStats(AttachmentType type,CallbackInfo ci){components.addAll(civilized.sbw.VehicleTooltips.attachmentStats(attachmentId.toString()));}
}
