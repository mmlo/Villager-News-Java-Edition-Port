package dev.villagernews.network;
import dev.villagernews.VillagerNews;
import dev.villagernews.client.ClientRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.UUID;
public final class NewsNetwork {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(VillagerNews.ID,"main"),()->"1","1"::equals,"1"::equals);
 public record Update(int kind,int entity,UUID uuid,long start,String animation,CompoundTag state){
  public void encode(FriendlyByteBuf b){b.writeVarInt(kind);b.writeVarInt(entity);b.writeUUID(uuid);b.writeLong(start);b.writeUtf(animation,256);b.writeNbt(state);}
  public static Update decode(FriendlyByteBuf b){return new Update(b.readVarInt(),b.readVarInt(),b.readUUID(),b.readLong(),b.readUtf(256),b.readNbt());}
 }
 public static void init(){CHANNEL.messageBuilder(Update.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(Update::encode).decoder(Update::decode).consumerMainThread((m,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRuntime.receive(m));c.get().setPacketHandled(true);}).add();}
 public static void broadcast(Entity e,int kind,String animation,long start){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->e),new Update(kind,e.getId(),e.getUUID(),start,animation,e.getPersistentData().getCompound("VillagerNews").copy()));}
 public static void stateTo(Entity e,ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(1,e.getId(),e.getUUID(),e.level().getGameTime(),"",e.getPersistentData().getCompound("VillagerNews").copy()));}
 public static void openGuide(ServerPlayer p){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(2,p.getId(),p.getUUID(),0,"",new CompoundTag()));}
}
