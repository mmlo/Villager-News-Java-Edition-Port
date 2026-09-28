package dev.villagernews.runtime;

import com.google.gson.JsonObject;
import dev.villagernews.VillagerNews;
import dev.villagernews.data.OriginalData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

import static dev.villagernews.data.OriginalData.object;

/** Persistent actor properties copied from the Bedrock behavior definitions. */
public final class ActorState {
    public static final String TAG = "VillagerNews";
    private static final Map<String, Map<String, Object>> DEFAULTS = new HashMap<>();

    private static final Map<net.minecraft.world.entity.EntityType<?>, String> ENTITY_ID_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<net.minecraft.world.entity.EntityType<?>, Boolean> OURS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private ActorState() {}

    public static String id(Entity entity) {
        return ENTITY_ID_CACHE.computeIfAbsent(entity.getType(), t -> {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(t);
            return key == null ? "" : key.toString();
        });
    }

    public static boolean ours(Entity entity) {
        return OURS_CACHE.computeIfAbsent(entity.getType(), t -> {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(t);
            return key != null && VillagerNews.ID.equals(key.getNamespace());
        });
    }

    public static void ensure(Entity entity) {
        if (!ours(entity) && !(entity instanceof net.minecraft.world.entity.npc.Villager)) return;
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        String entityId = ours(entity) ? id(entity) : "oreville_vn:villager";
        for (var entry : defaults(entityId).entrySet()) {
            if (tag.contains(entry.getKey())) continue;
            if (entry.getValue() instanceof String text) tag.putString(entry.getKey(), text);
            else tag.putDouble(entry.getKey(), ((Number) entry.getValue()).doubleValue());
        }
        if (tag.getDouble("p:pmpece") == 0d) {
            tag.putDouble("p:pmpece", 1d);
        }
        entity.getPersistentData().put(TAG, tag);
    }

    public static CompoundTag tag(Entity entity) {
        return entity.getPersistentData().getCompound(TAG);
    }

    public static boolean has(CompoundTag tag, String entityId, String key) {
        if ("p:pmpece".equals(key)) return true;
        return tag.contains(key) || defaults(entityId).containsKey(key);
    }

    public static Object property(CompoundTag tag, String entityId, String key) {
        if ("p:pmpece".equals(key)) {
            if (tag != null && tag.contains(key)) {
                double val = tag.getDouble(key);
                if (val > 0) return val;
            }
            return 1d;
        }
        if (tag.contains(key, Tag.TAG_STRING)) return tag.getString(key);
        if (tag.contains(key)) return tag.getDouble(key);
        return defaults(entityId).getOrDefault(key, 0d);
    }

    private static Map<String, Object> defaults(String entityId) {
        return DEFAULTS.computeIfAbsent(entityId, ActorState::load);
    }

    private static Map<String, Object> load(String entityId) {
        Map<String, Object> values = new HashMap<>();
        JsonObject properties = object(object(object(OriginalData.section("behaviors"), entityId), "description"), "properties");
        for (var entry : properties.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject definition = entry.getValue().getAsJsonObject();
            if (!definition.has("default") || !definition.get("default").isJsonPrimitive()) continue;
            var primitive = definition.getAsJsonPrimitive("default");
            if (primitive.isBoolean()) values.put(entry.getKey(), primitive.getAsBoolean() ? 1d : 0d);
            else if (primitive.isNumber()) values.put(entry.getKey(), primitive.getAsDouble());
            else values.put(entry.getKey(), primitive.getAsString());
        }
        if (values.containsKey("p:pmpece") || "oreville_vn:ilvfra".equals(entityId)) {
            values.put("p:pmpece", 1d);
        }
        return values;
    }
}
