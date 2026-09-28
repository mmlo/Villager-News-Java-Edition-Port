package dev.villagernews.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.villagernews.VillagerNews;
import dev.villagernews.data.OriginalData;
import dev.villagernews.network.NewsNetwork;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Robust dialogue trigger and playback system for Villager News.
 * Faithfully mirrors the original dialogue groups, variant weighting, sound playback,
 * network animation dispatch, and speech cooldowns.
 */
public final class DialogueSystem {
    private static final Map<UUID, Long> LAST_ENTITY_SPEECH = new ConcurrentHashMap<>();
    private static long lastGlobalSpeech = 0;
    private static final Random RANDOM = new Random();

    // Representative dialogue groups mapped from original triggers
    public static final List<String> GREETINGS = List.of("ueviuf", "mzmwtx", "vnltwn", "pkrkml", "aozdsb");
    public static final List<String> INTERVIEWS = List.of("jmggiv", "mmjdlb", "rzcfxh", "aauayl", "crddtp");
    public static final List<String> HURT_REACTIONS = List.of("ueviuf", "aozdsb", "crddtp");

    public record Variant(String animationName, String soundId, double duration, double weight, Map<Double, String> subtitles) {}
    public record GroupData(List<Variant> variants, double totalWeight) {}

    private static final Map<String, GroupData> CACHED_GROUPS = new ConcurrentHashMap<>();
    private static final Map<String, SoundEvent> SOUND_CACHE = new ConcurrentHashMap<>();

    private static SoundEvent getSound(String id) {
        return SOUND_CACHE.computeIfAbsent(id, k -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(VillagerNews.ID, k)));
    }

    public static boolean canSpeak(LivingEntity entity, long currentTime) {
        if (currentTime - lastGlobalSpeech < 60) return false; // 3 seconds global cooldown
        if (currentTime % 600 == 0) {
            LAST_ENTITY_SPEECH.entrySet().removeIf(e -> currentTime - e.getValue() > 1200);
        }
        Long last = LAST_ENTITY_SPEECH.get(entity.getUUID());
        return last == null || (currentTime - last >= 160); // 8 seconds per-entity cooldown
    }

    private static GroupData loadGroup(String groupId) {
        JsonObject group = OriginalData.GROUPS.get(groupId);
        if (group == null && OriginalData.DIALOGUES.has(groupId)) {
            group = OriginalData.DIALOGUES.getAsJsonObject(groupId);
        }
        if (group == null && !OriginalData.GROUPS.isEmpty()) {
            var iterator = OriginalData.DIALOGUES.keySet().iterator();
            if (iterator.hasNext()) group = OriginalData.DIALOGUES.getAsJsonObject(iterator.next());
        }
        if (group == null) return null;

        JsonArray slhkqn = OriginalData.array(group, "slhkqn");
        if (slhkqn.size() == 0) return null;

        List<Variant> variants = new ArrayList<>();
        double totalWeight = 0;
        for (JsonElement elem : slhkqn) {
            JsonObject obj = elem.getAsJsonObject();
            String anim = OriginalData.string(obj, "animationName", "");
            String sound = OriginalData.string(obj, "soundId", "");
            double dur = OriginalData.number(obj, "duration", 2.0);
            double weight = OriginalData.number(obj, "weight", 1.0);
            if (weight <= 0) weight = 1.0;

            Map<Double, String> subs = new LinkedHashMap<>();
            JsonObject aswuwr = OriginalData.object(obj, "aswuwr");
            for (var entry : aswuwr.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    String subKey = OriginalData.string(entry.getValue().getAsJsonObject(), "ysyeto", "");
                    if (!subKey.isEmpty()) {
                        try {
                            subs.put(Double.parseDouble(entry.getKey()), subKey);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            Variant v = new Variant(anim, sound, dur, weight, subs);
            variants.add(v);
            totalWeight += weight;
        }
        if (variants.isEmpty()) return null;
        return new GroupData(variants, totalWeight);
    }

    public static boolean speak(LivingEntity entity, String groupId, boolean ignoreCooldown) {
        if (entity.level().isClientSide()) return false;
        long now = entity.level().getGameTime();
        if (!ignoreCooldown && !canSpeak(entity, now)) return false;

        GroupData groupData = CACHED_GROUPS.computeIfAbsent(groupId, DialogueSystem::loadGroup);
        if (groupData == null || groupData.variants.isEmpty()) return false;

        // Choose variant
        double r = RANDOM.nextDouble() * groupData.totalWeight;
        Variant chosen = groupData.variants.get(0);
        double cumulative = 0;
        for (Variant v : groupData.variants) {
            cumulative += v.weight;
            if (r <= cumulative) {
                chosen = v;
                break;
            }
        }

        // Play Sound per player locale
        String rawSoundId = chosen.soundId.startsWith("oreville_vn:") ? chosen.soundId.substring(12) : chosen.soundId;
        String ptSoundId = "pt_br." + rawSoundId;
        SoundEvent enSoundEvent = getSound(rawSoundId);
        SoundEvent ptSoundEvent = getSound(ptSoundId);
        if (ptSoundEvent == null) {
            ptSoundEvent = enSoundEvent;
        }

        List<ServerPlayer> nearbyPlayers = entity.level().getEntitiesOfClass(
            ServerPlayer.class, entity.getBoundingBox().inflate(32.0)
        );

        if (!nearbyPlayers.isEmpty()) {
            for (ServerPlayer player : nearbyPlayers) {
                String lang = player.getLanguage();
                boolean isPt = lang != null && lang.toLowerCase().startsWith("pt");
                SoundEvent targetEvent = isPt ? ptSoundEvent : enSoundEvent;
                if (targetEvent != null) {
                    player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(targetEvent),
                        SoundSource.VOICE,
                        entity.getX(), entity.getY(), entity.getZ(),
                        1.0f, 1.0f,
                        RANDOM.nextLong()
                    ));
                }
            }
        } else if (enSoundEvent != null) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), enSoundEvent, SoundSource.VOICE, 1.0f, 1.0f);
        }

        // Broadcast Animation & Timing to Clients
        NewsNetwork.broadcast(entity, 1, chosen.animationName, now);

        // Update Cooldowns
        LAST_ENTITY_SPEECH.put(entity.getUUID(), now);
        lastGlobalSpeech = now;

        // Display Subtitles in action bar to nearby players
        if (!chosen.subtitles.isEmpty()) {
            MutableComponent text = Component.empty();
            boolean first = true;
            for (String subKey : chosen.subtitles.values()) {
                if (!first) {
                    text.append(" ");
                }
                text.append(Component.translatable(subKey));
                first = false;
            }
            for (ServerPlayer player : nearbyPlayers) {
                player.displayClientMessage(text, true);
            }
        }

        return true;
    }

    public static boolean triggerRandom(LivingEntity entity, List<String> groupList) {
        if (groupList.isEmpty()) return false;
        String gid = groupList.get(RANDOM.nextInt(groupList.size()));
        return speak(entity, gid, false);
    }
}
