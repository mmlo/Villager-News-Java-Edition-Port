package dev.villagernews.runtime;

import dev.villagernews.VillagerNews;
import dev.villagernews.entity.NewsVillager;
import dev.villagernews.network.NewsNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 100% fidelity server-side interaction and reaction engine mirroring the original Bedrock addon.
 * Handles shearing, nose removal/restoration, cosmetics, signs, proximity disguise, and dialogue triggers.
 */
public final class Reactions {
    private static final Map<UUID, Item> LAST_HEAD_COSMETIC = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> SIGN_TO_INDEX = new HashMap<>();
    private static final Map<Integer, Item> INDEX_TO_SIGN = new HashMap<>();

    static {
        Item[] signs = new Item[] {
            Items.OAK_SIGN, Items.SPRUCE_SIGN, Items.BIRCH_SIGN, Items.JUNGLE_SIGN,
            Items.ACACIA_SIGN, Items.DARK_OAK_SIGN, Items.MANGROVE_SIGN, Items.CHERRY_SIGN,
            Items.BAMBOO_SIGN, Items.CRIMSON_SIGN, Items.WARPED_SIGN
        };
        for (int i = 0; i < signs.length; i++) {
            SIGN_TO_INDEX.put(signs[i], i);
            INDEX_TO_SIGN.put(i, signs[i]);
        }
    }

    @SubscribeEvent
    public void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof LightningBolt lightning) {
            // Thunderstruck dialogue trigger matching Bedrock rsifiu
            var nearbyVillagers = lightning.level().getEntitiesOfClass(
                LivingEntity.class, lightning.getBoundingBox().inflate(64.0),
                e -> ActorState.ours(e) || e instanceof Villager
            );
            if (!nearbyVillagers.isEmpty()) {
                LivingEntity nearest = nearbyVillagers.get(0);
                DialogueSystem.speak(nearest, "rsifiu", false);
            }
            return;
        }

        if (ActorState.ours(event.getEntity()) || event.getEntity() instanceof Villager) {
            ActorState.ensure(event.getEntity());
        }
    }

    @SubscribeEvent
    public void track(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ActorState.ours(event.getTarget()) && !(event.getTarget() instanceof Villager)) return;

        ActorState.ensure(event.getTarget());
        NewsNetwork.stateTo(event.getTarget(), player);
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (living.isSleeping()) return;

        ItemStack held = event.getItemStack();
        String heldId = ForgeRegistries.ITEMS.getKey(held.getItem()) != null ?
                ForgeRegistries.ITEMS.getKey(held.getItem()).toString() : "";

        // =========================================================
        // 1. Wooly Sheep (mlkxjo) Interactions
        // =========================================================
        boolean isWooly = living.getType() == VillagerNews.WOOLY.get() ||
                (living instanceof Sheep && ActorState.ours(living));

        if (isWooly) {
            Sheep sheep = (Sheep) living;
            if (held.getItem() instanceof ShearsItem) {
                if (sheep.isSheared()) {
                    // Already sheared reaction (wlacvx / afxbav)
                    DialogueSystem.speak(sheep, "wlacvx", false);
                } else {
                    // Shearing Wooly
                    sheep.setSheared(true);
                    sheep.spawnAtLocation(new ItemStack(Items.WHITE_WOOL, 1 + sheep.getRandom().nextInt(3)));
                    sheep.level().playSound(null, sheep.getX(), sheep.getY(), sheep.getZ(),
                            SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);

                    if (!player.isCreative()) {
                        held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
                    }

                    // Sheared reaction (vqqkna / edrtbe)
                    DialogueSystem.speak(sheep, "vqqkna", false);
                    NewsNetwork.broadcast(sheep, 1, "", sheep.level().getGameTime());
                }
            } else {
                // Regular interact with Wooly
                if (sheep.isSheared()) {
                    DialogueSystem.speak(sheep, "wlacvx", false);
                } else {
                    DialogueSystem.speak(sheep, "bmceek", false);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // =========================================================
        // 2. Villagers (NewsVillagers or Vanilla Villagers / Traders)
        // =========================================================
        if (!ActorState.ours(living) && !(living instanceof Villager) && !(living instanceof WanderingTrader)) return;

        ActorState.ensure(living);
        CompoundTag tag = ActorState.tag(living);
        long now = living.level().getGameTime();

        // 2.1 Untouchable Villager (ghibss) taunt
        if (living instanceof NewsVillager nv && "ghibss".equals(nv.kind())) {
            DialogueSystem.speak(living, "amhagx", false);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // 2.2 Player using Shears on Villager
        if (held.getItem() instanceof ShearsItem) {
            double sign = tag.contains("p:sign") ? tag.getDouble("p:sign") : -1d;
            String cosmetic = tag.contains("p:mlxeez") ? tag.getString("p:mlxeez") : "none";
            double nose = tag.contains("p:gcfsvg") ? tag.getDouble("p:gcfsvg") : 1d;

            if (sign >= 0) {
                // Remove protest sign
                tag.putDouble("p:sign", -1d);
                Item signItem = INDEX_TO_SIGN.getOrDefault((int) sign, Items.OAK_SIGN);
                living.spawnAtLocation(new ItemStack(signItem));
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.isCreative()) {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
                }
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }

            if (!cosmetic.isEmpty() && !"none".equals(cosmetic)) {
                // Remove cosmetic
                tag.putString("p:mlxeez", "none");
                living.spawnAtLocation(new ItemStack(VillagerNews.item(cosmetic)));
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.isCreative()) {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
                }

                // Cosmetic removed dialogue (bbkhrp / ckjbyd)
                DialogueSystem.speak(living, "bbkhrp", false);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }

            if (nose > 0.5 && !living.isBaby()) {
                // Cut off nose!
                tag.putDouble("p:gcfsvg", 0d);
                living.spawnAtLocation(new ItemStack(VillagerNews.item("qzhdgf")));
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);

                if (!player.isCreative()) {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
                }

                // Sheared nose reaction (pkrkml / jktrnd)
                DialogueSystem.speak(living, "pkrkml", false);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }
        }

        // 2.3 Player using Villager Nose (qzhdgf) on Villager
        if (heldId.equals("oreville_vn:qzhdgf")) {
            double nose = tag.contains("p:gcfsvg") ? tag.getDouble("p:gcfsvg") : 1d;
            if (nose < 0.5) {
                // Return nose!
                tag.putDouble("p:gcfsvg", 1d);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Nose returned reaction (ueviuf / kxrhxt)
                DialogueSystem.speak(living, "ueviuf", false);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            } else {
                // Second nose complaint (kmkrwf / akfekx)
                DialogueSystem.speak(living, "kmkrwf", false);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }
        }

        // 2.4 Player using Axe on Villager with sign
        if (held.getItem() instanceof AxeItem) {
            double sign = tag.contains("p:sign") ? tag.getDouble("p:sign") : -1d;
            if (sign >= 0) {
                int curVariant = tag.contains("p:wjnyei") ? (int) tag.getDouble("p:wjnyei") : 0;
                int delta = player.isShiftKeyDown() ? -1 : 1;
                int nextVariant = (curVariant + delta + 87) % 87;
                tag.putDouble("p:wjnyei", nextVariant);

                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }
        }

        // 2.5 Player using SignItem on Villager
        if (held.getItem() instanceof SignItem && !living.isBaby()) {
            Integer signIndex = SIGN_TO_INDEX.get(held.getItem());
            if (signIndex != null) {
                // Drop microphone if currently equipped
                if ("dsojot".equals(tag.getString("p:mlxeez"))) {
                    tag.putString("p:mlxeez", "none");
                    living.spawnAtLocation(new ItemStack(VillagerNews.item("dsojot")));
                }

                double oldSign = tag.contains("p:sign") ? tag.getDouble("p:sign") : -1d;
                if (oldSign >= 0 && (int) oldSign != signIndex) {
                    living.spawnAtLocation(new ItemStack(INDEX_TO_SIGN.getOrDefault((int) oldSign, Items.OAK_SIGN)));
                }

                tag.putDouble("p:sign", signIndex);
                if (oldSign < 0) {
                    tag.putDouble("p:wjnyei", living.getRandom().nextInt(87));
                }

                if (!player.isCreative()) {
                    held.shrink(1);
                }

                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Sign protest reaction (ygtfyv / vqlrqf)
                DialogueSystem.speak(living, "ygtfyv", false);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }
        }

        // 2.6 Player equipping Cosmetics on Villager (cryhjc, odplew, ufernq, dsojot)
        if (List.of("oreville_vn:cryhjc", "oreville_vn:odplew", "oreville_vn:ufernq", "oreville_vn:dsojot").contains(heldId)) {
            String cosmeticName = heldId.replace("oreville_vn:", "");
            String curCosmetic = tag.contains("p:mlxeez") ? tag.getString("p:mlxeez") : "none";

            if ("none".equals(curCosmetic)) {
                if ("dsojot".equals(cosmeticName) && tag.contains("p:sign") && tag.getDouble("p:sign") >= 0) {
                    // Drop held sign first
                    int sIndex = (int) tag.getDouble("p:sign");
                    tag.putDouble("p:sign", -1d);
                    living.spawnAtLocation(new ItemStack(INDEX_TO_SIGN.getOrDefault(sIndex, Items.OAK_SIGN)));
                }

                tag.putString("p:mlxeez", cosmeticName);
                if (!player.isCreative()) {
                    held.shrink(1);
                }

                living.level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);

                // Cosmetic equipped dialogue
                String dialogueGroup = switch (cosmeticName) {
                    case "cryhjc" -> "egqivb";
                    case "ufernq" -> "fsepbc";
                    case "odplew" -> "okjbgd";
                    case "dsojot" -> "wylybx";
                    default -> "ueviuf";
                };
                DialogueSystem.speak(living, dialogueGroup, false);
                NewsNetwork.broadcast(living, 1, "", now);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
                event.setCanceled(true);
                return;
            }
        }

        // 2.7 Microphone interview trigger
        if (heldId.equals("oreville_vn:dsojot")) {
            DialogueSystem.triggerRandom(living, DialogueSystem.INTERVIEWS);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // 2.8 Proximity and contextual reactions on regular interact
        ItemStack playerHead = player.getItemBySlot(EquipmentSlot.HEAD);
        String playerHeadId = ForgeRegistries.ITEMS.getKey(playerHead.getItem()) != null ?
                ForgeRegistries.ITEMS.getKey(playerHead.getItem()).toString() : "";

        // Player wearing Mayor Hat near Mayor (ilvfra)
        if (playerHeadId.equals("oreville_vn:cryhjc") && living instanceof NewsVillager nv && "ilvfra".equals(nv.kind())) {
            DialogueSystem.speak(living, "tvlnxx", false);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // Player wearing Moustache near Villager #5 (vwpagn)
        if (playerHeadId.equals("oreville_vn:odplew") && living instanceof NewsVillager nv && "vwpagn".equals(nv.kind())) {
            DialogueSystem.speak(living, "ngfhdb", false);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // Villager wearing Helmet
        String villagerCosmetic = tag.getString("p:mlxeez");
        if (living instanceof NewsVillager nv && "poztxf".equals(nv.kind()) && "ufernq".equals(villagerCosmetic)) {
            DialogueSystem.speak(living, "oqkfej", false);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        if (!villagerCosmetic.isEmpty() && !"none".equals(villagerCosmetic)) {
            DialogueSystem.speak(living, "nkkrdb", false);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            event.setCanceled(true);
            return;
        }

        // General greeting
        DialogueSystem.triggerRandom(living, DialogueSystem.GREETINGS);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity living = event.getEntity();
        if (ActorState.ours(living) || living instanceof Villager) {
            DialogueSystem.triggerRandom(living, DialogueSystem.HURT_REACTIONS);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        ServerPlayer player = (ServerPlayer) event.player;

        // Check every 10 ticks (0.5 seconds)
        if (player.tickCount % 10 != 0) return;

        // Disguise detection: Player putting on Villager Nose (qzhdgf)
        ItemStack currentHead = player.getItemBySlot(EquipmentSlot.HEAD);
        Item currentItem = currentHead.getItem();
        Item prevItem = LAST_HEAD_COSMETIC.put(player.getUUID(), currentItem);

        if (currentHead.is(VillagerNews.item("qzhdgf")) && currentItem != prevItem) {
            // Find nearby villager to trigger disguise line (iyhtlh / kejscw)
            var nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(16.0),
                e -> (ActorState.ours(e) || e instanceof Villager) && !(e instanceof Sheep)
            );
            if (!nearby.isEmpty()) {
                DialogueSystem.speak(nearby.get(0), "iyhtlh", false);
            }
        }

        // Ambient idle greetings every 5 seconds
        if (player.tickCount % 100 == 0) {
            var nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(5.0),
                e -> (ActorState.ours(e) || e instanceof Villager) && !(e instanceof Sheep)
            );
            for (LivingEntity entity : nearby) {
                if (DialogueSystem.canSpeak(entity, player.level().getGameTime())) {
                    DialogueSystem.triggerRandom(entity, DialogueSystem.GREETINGS);
                    break;
                }
            }
        }
    }
}
