package dev.villagernews.entity;

import dev.villagernews.VillagerNews;
import dev.villagernews.runtime.DialogueSystem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class NewsVillager extends Villager {
    private long lastTauntTick = 0;

    public NewsVillager(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        dev.villagernews.runtime.ActorState.ensure(this);
    }

    private String cachedKind;

    public String kind() {
        if (cachedKind == null) {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(getType());
            cachedKind = key != null ? key.getPath() : "";
        }
        return cachedKind;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        if ("ghibss".equals(kind())) {
            // "Can't Catch Me!" untargetable fleeing behavior
            this.goalSelector.addGoal(0, new AvoidEntityGoal<>(this, Player.class, 16.0F, 0.8D, 1.2D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && "ghibss".equals(kind()) && tickCount % 10 == 0) {
            Player nearestPlayer = level().getNearestPlayer(this, 16.0);
            if (nearestPlayer != null) {
                double dist = distanceTo(nearestPlayer);
                long now = level().getGameTime();
                if (dist <= 6.0 && (now - lastTauntTick > 80)) {
                    lastTauntTick = now;
                    DialogueSystem.speak(this, "amhagx", true);
                }
                if (dist < 2.5) {
                    // Cornered! Teleport 12-16 blocks away
                    for (int i = 0; i < 16; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double radius = 10.0 + random.nextDouble() * 6.0;
                        double targetX = getX() + Math.cos(angle) * radius;
                        double targetY = getY() + (random.nextDouble() * 6.0 - 3.0);
                        double targetZ = getZ() + Math.sin(angle) * radius;
                        if (randomTeleport(targetX, targetY, targetZ, true)) {
                            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void updateTrades() {
        String reward = switch (kind()) {
            case "ilvfra" -> "cryhjc";
            case "poztxf" -> "ufernq";
            case "vwpagn" -> "odplew";
            case "xcrjxf" -> "dsojot";
            default -> "";
        };
        if (reward.isEmpty()) {
            super.updateTrades();
            return;
        }
        getOffers().add(new MerchantOffer(
            new ItemStack(Items.EMERALD, kind().equals("ilvfra") ? 24 : 16),
            new ItemStack(VillagerNews.item(reward)),
            16, 2, 0.1f
        ));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!kind().equals("villager") && !kind().equals("ghibss") && !isBaby() && getVillagerData().getProfession() == VillagerProfession.NONE) {
            setVillagerData(getVillagerData().setProfession(VillagerProfession.NITWIT));
        }
        if (!kind().equals("villager") && !kind().equals("ghibss") && !isBaby() && !player.getItemInHand(hand).is(Items.VILLAGER_SPAWN_EGG)) {
            if (!level().isClientSide && !getOffers().isEmpty()) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (kind().equals("ghibss")) return false;
        return super.hurt(source, amount);
    }
}
