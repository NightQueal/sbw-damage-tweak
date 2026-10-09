package civilized.sbw;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import java.util.Set;

@Mod("civilized_sbw")
public final class CivilizedSbw {
 private static final Set<String> BANNED=Set.of("superbwarfare:netherite_hammer","superbwarfare:dps_generator_deployer");
 public CivilizedSbw() {
  net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,CompatConfig.SPEC,"civilized-sbw-common.toml");
  MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,DamageCompatibility::explosion);
  MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,this::join);
  MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,this::use);
  MinecraftForge.EVENT_BUS.addListener(this::playerTick);
  MinecraftForge.EVENT_BUS.addListener(this::levelTick);
 }
 private static boolean banned(ItemStack s) {return !s.isEmpty() && BANNED.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())));}
 private static boolean dps(Entity e) {return "superbwarfare:dps_generator".equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType())));}
 private void join(EntityJoinLevelEvent e) {
  if(dps(e.getEntity()) || (e.getEntity() instanceof ItemEntity item && banned(item.getItem())))e.setCanceled(true);
 }
 private void use(PlayerInteractEvent.RightClickItem e) {
  if(banned(e.getItemStack()))e.setCanceled(true);
 }
 private void playerTick(TickEvent.PlayerTickEvent e) {
  if(e.phase!=TickEvent.Phase.END || e.player.level().isClientSide)return;
  var inv=e.player.getInventory();
  for(int i=0;i<inv.getContainerSize();i++)if(banned(inv.getItem(i)))inv.setItem(i,ItemStack.EMPTY);
 }
 private void levelTick(TickEvent.LevelTickEvent e) {
  if(e.phase!=TickEvent.Phase.END || !(e.level instanceof ServerLevel level) || level.getGameTime()%20!=0)return;
  for(Entity entity:level.getAllEntities())if(dps(entity))entity.discard();
 }
}
