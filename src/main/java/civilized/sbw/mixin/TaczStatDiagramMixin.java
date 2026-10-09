package civilized.sbw.mixin;
import com.tacz.guns.client.gui.components.refit.GunPropertyDiagrams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import civilized.sbw.*;
@Mixin(GunPropertyDiagrams.class)
public abstract class TaczStatDiagramMixin {
 private static ArmorProfiles.Ammo current(){var p=Minecraft.getInstance().player;return p==null?null:VehicleTooltips.gunProfile(p.getMainHandItem());}
 @Inject(method="getHidePropertyButtonYOffset",at=@At("RETURN"),cancellable=true,remap=false)
 private static void height(CallbackInfoReturnable<Integer> ci){if(current()!=null)ci.setReturnValue(ci.getReturnValue()+40);}
 @Inject(method="draw",at=@At("TAIL"),remap=false)
 private static void rows(GuiGraphics g,Font f,int x,int y,CallbackInfo ci) {
  var a=current();if(a==null)return;int top=y+GunPropertyDiagrams.getHidePropertyButtonYOffset()-51;
  row(g,f,x,top,"Veh. Pen.",a.penetration/500,StatText.number(a.penetration)+"mm");
  row(g,f,x,top+10,"Blast Pen.",a.blastPenetration/50,StatText.number(a.blastPenetration)+"mm");
  row(g,f,x,top+20,"Normalize",a.normalization/45,StatText.number(a.normalization)+"\u00b0");
  row(g,f,x,top+30,"Ricochet",a.ricochetStart/90,ProfileSync.client.ricochets()?StatText.number(a.ricochetStart)+"-"+StatText.number(a.ricochetEnd)+"\u00b0":"OFF");
 }
 private static void row(GuiGraphics g,Font f,int x,int y,String label,double fraction,String value) {
  g.drawString(f,label,x+12,y,0xCCCCCC,false);g.fill(x+83,y+3,x+203,y+7,0xFF000000);
  g.fill(x+83,y+3,x+83+(int)(120*Math.max(0,Math.min(1,fraction))),y+7,0xFFFFFFFF);
  g.drawString(f,value,x+210,y,0xCCCCCC,false);
 }
}
