package dev.villagernews.client;

import dev.villagernews.VillagerNews;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VillagerNews.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ClientRuntime.openGuide = () -> Minecraft.getInstance().setScreen(new GuideScreen());
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        VillagerNews.VILLAGERS.values().forEach(type -> event.registerEntityRenderer(type.get(), NewsRenderer::new));
        event.registerEntityRenderer(VillagerNews.WOOLY.get(), context -> new NewsRenderer<>(context));
        event.registerEntityRenderer(VillagerNews.TRADER.get(), context -> new NewsRenderer<>(context));
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer != null) {
                addCosmeticLayer(renderer);
            }
        }
        var armorStand = event.getRenderer(net.minecraft.world.entity.EntityType.ARMOR_STAND);
        if (armorStand != null) {
            addCosmeticLayer(armorStand);
        }
        var villager = event.getRenderer(net.minecraft.world.entity.EntityType.VILLAGER);
        if (villager != null) {
            addCosmeticLayer(villager);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> void addCosmeticLayer(net.minecraft.client.renderer.entity.LivingEntityRenderer<T, M> renderer) {
        renderer.addLayer(new CosmeticHeadLayer<>(renderer));
    }

    @Mod.EventBusSubscriber(modid = VillagerNews.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onEntityLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                ClientRuntime.remove(event.getEntity().getUUID());
            }
        }

        @SubscribeEvent
        public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) {
                ClientRuntime.clear();
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            ClientRuntime.clear();
        }
    }
}
