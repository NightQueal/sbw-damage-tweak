package civilized.sbw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import com.google.gson.Gson;
/** Server settings are sent once at login, not per tooltip or per tick. */
public final class ProfileSync {
 public record Snapshot(String json,boolean enabled,boolean ricochets,boolean components,boolean mounted) {
  public void write(FriendlyByteBuf b){b.writeUtf(json,262144);b.writeBoolean(enabled);b.writeBoolean(ricochets);b.writeBoolean(components);b.writeBoolean(mounted);}
  public static Snapshot read(FriendlyByteBuf b){return new Snapshot(b.readUtf(262144),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
 }
 public static volatile Snapshot client;
 public static volatile ArmorProfiles.Data clientProfiles;
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("civilized_sbw","profiles"),()->"3", "3"::equals,"3"::equals);
 public static void init() {
  CHANNEL.messageBuilder(Snapshot.class,0,NetworkDirection.PLAY_TO_CLIENT)
   .encoder(Snapshot::write)
   .decoder(Snapshot::read)
   .consumerMainThread((p,c)->{clientProfiles=new Gson().fromJson(p.json,ArmorProfiles.Data.class);client=p;c.get().setPacketHandled(true);}).add();
  MinecraftForge.EVENT_BUS.addListener(ProfileSync::login);
 }
 private static void login(PlayerEvent.PlayerLoggedInEvent e) {
  if(e.getEntity() instanceof ServerPlayer p)CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(ArmorProfiles.json(),CompatConfig.ADVANCED_ARMOR.get(),CompatConfig.RICOCHETS.get(),CompatConfig.COMPONENT_HITS.get(),CompatConfig.MOUNTED_ARMOR.get()));
 }
}
