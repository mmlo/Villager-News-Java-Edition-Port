package dev.villagernews;
import dev.villagernews.data.OriginalData;
import dev.villagernews.entity.NewsVillager;
import dev.villagernews.network.NewsNetwork;
import dev.villagernews.runtime.Reactions;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import java.util.*;
@Mod(VillagerNews.ID)
public final class VillagerNews {
 public static final String ID="oreville_vn";
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
 public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
 public static final Map<String,RegistryObject<Item>> ITEM_MAP=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<EntityType<NewsVillager>>> VILLAGERS=new LinkedHashMap<>();
 public static final RegistryObject<EntityType<Sheep>> WOOLY=ENTITIES.register("mlkxjo",()->EntityType.Builder.of(Sheep::new,MobCategory.CREATURE).sized(.9f,1.3f).clientTrackingRange(10).build(ID+":mlkxjo"));
 public static final RegistryObject<EntityType<WanderingTrader>> TRADER=ENTITIES.register("txczvv",()->EntityType.Builder.of(WanderingTrader::new,MobCategory.CREATURE).sized(.6f,1.95f).clientTrackingRange(10).build(ID+":txczvv"));
 static {
  Map<String, int[]> colors = Map.of(
      "villager", new int[]{0x946638, 0xbd8b72},
      "ilvfra",   new int[]{0x222222, 0xe5a93b},
      "poztxf",   new int[]{0xf1c40f, 0xb01414},
      "vwpagn",   new int[]{0x486581, 0xd9b38c},
      "xcrjxf",   new int[]{0x34495e, 0x27ae60},
      "ghibss",   new int[]{0x16a085, 0xf39c12}
  );
  for(String id:List.of("villager","ilvfra","poztxf","vwpagn","xcrjxf","ghibss")){
   var type=ENTITIES.register(id,()->EntityType.Builder.of(NewsVillager::new,MobCategory.CREATURE).sized(id.equals("ilvfra")?.98f:.6f,id.equals("ilvfra")?1.96f:1.95f).clientTrackingRange(10).build(ID+":"+id));VILLAGERS.put(id,type);
   int[] c = colors.getOrDefault(id, new int[]{0x946638, 0xbd8b72});
   ITEM_MAP.put(id+"_spawn_egg",ITEMS.register(id+"_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(type,c[0],c[1],new Item.Properties())));
  }
  ITEM_MAP.put("mlkxjo_spawn_egg",ITEMS.register("mlkxjo_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(WOOLY,0xe7e7e7,0x996e55,new Item.Properties())));
  ITEM_MAP.put("txczvv_spawn_egg",ITEMS.register("txczvv_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(TRADER,0x274680,0xb38661,new Item.Properties())));
  ITEM_MAP.put("kfjmlk",ITEMS.register("kfjmlk",()->new dev.villagernews.item.GuideBookItem("kfjmlk")));
  ITEM_MAP.put("dsojot",ITEMS.register("dsojot",()->new dev.villagernews.item.MicrophoneItem("dsojot")));
  for(String id:List.of("cryhjc","odplew","ufernq","qzhdgf")) ITEM_MAP.put(id,ITEMS.register(id,()->new dev.villagernews.item.CosmeticHeadItem(id)));
  for(String id:OriginalData.SOUNDS.keySet()) SOUNDS.register(id,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,id)));
  TABS.register("news",()->CreativeModeTab.builder().title(Component.literal("Villager News")).icon(()->new ItemStack(item("kfjmlk"))).displayItems((p,o)->ITEM_MAP.values().forEach(i->o.accept(i.get()))).build());
 }
 public VillagerNews(){IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();ITEMS.register(bus);ENTITIES.register(bus);SOUNDS.register(bus);TABS.register(bus);bus.addListener(this::attributes);ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,NewsConfig.SERVER);ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,NewsConfig.CLIENT);NewsNetwork.init();MinecraftForge.EVENT_BUS.register(new Reactions());MinecraftForge.EVENT_BUS.register(dev.villagernews.command.NewsCommands.class);}
 void attributes(EntityAttributeCreationEvent e){VILLAGERS.values().forEach(t->e.put(t.get(),Villager.createAttributes().build()));e.put(WOOLY.get(),Sheep.createAttributes().build());e.put(TRADER.get(),net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityType.WANDERING_TRADER));}
 public static Item item(String id){return ITEM_MAP.get(id).get();}
}
