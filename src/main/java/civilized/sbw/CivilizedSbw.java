package civilized.sbw;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
/** Combat compatibility only. Pack item restrictions belong to optional KubeJS scripts. */
@Mod("civilized_sbw")
public final class CivilizedSbw {
 public CivilizedSbw() {
  ArmorProfiles.load();ProfileSync.init();
  MinecraftForge.EVENT_BUS.addListener(ArmorScan::register);
  net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CompatConfig.SPEC,"civilized-sbw-common.toml");
  MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,DamageCompatibility::explosion);
 }
}
