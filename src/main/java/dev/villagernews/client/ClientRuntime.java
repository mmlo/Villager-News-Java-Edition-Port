package dev.villagernews.client;

import com.google.gson.JsonObject;
import dev.villagernews.data.OriginalData;
import dev.villagernews.network.NewsNetwork;
import dev.villagernews.runtime.ActorState;
import dev.villagernews.runtime.Molang;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.villagernews.data.OriginalData.object;

/** Per-entity animation state shared by the network handler and the renderer. */
public final class ClientRuntime {
    public static final class Visual {
        public final String id;
        public final JsonObject description;
        public final AnimationMachine machine;
        public Visual(String id, JsonObject description, AnimationMachine machine) {
            this.id = id;
            this.description = description;
            this.machine = machine;
        }
    }

    /** Installed by the client setup. Empty on a dedicated server, where this method is not called. */
    public static Runnable openGuide = () -> {};

    private static final Map<UUID, Visual> VISUALS = new ConcurrentHashMap<>();
    private static final Map<UUID, NewsNetwork.Update> STATES = new ConcurrentHashMap<>();
    private static final List<VillagerProfession> PROFESSIONS = List.of(
            VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.SHEPHERD, VillagerProfession.FLETCHER,
            VillagerProfession.LIBRARIAN, VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.ARMORER,
            VillagerProfession.WEAPONSMITH, VillagerProfession.TOOLSMITH, VillagerProfession.BUTCHER, VillagerProfession.LEATHERWORKER,
            VillagerProfession.MASON, VillagerProfession.NITWIT);
    private static final List<VillagerType> BIOMES = List.of(
            VillagerType.PLAINS, VillagerType.DESERT, VillagerType.JUNGLE, VillagerType.SAVANNA,
            VillagerType.SNOW, VillagerType.SWAMP, VillagerType.TAIGA);

    private ClientRuntime() {}

    public static void receive(NewsNetwork.Update update) {
        if (update.kind() == 2) {
            openGuide.run();
            return;
        }
        STATES.put(update.uuid(), update);
    }

    public static void remove(UUID uuid) {
        VISUALS.remove(uuid);
        STATES.remove(uuid);
    }

    public static void clear() {
        VISUALS.clear();
        STATES.clear();
    }

    public static Visual visual(LivingEntity entity) {
        String id = ActorState.id(entity);
        JsonObject description = object(OriginalData.section("entities"), id);
        if (description.size() == 0) return null;
        return VISUALS.computeIfAbsent(entity.getUUID(), ignored -> new Visual(id, description, new AnimationMachine(description)));
    }

    public static void beforeRender(LivingEntity entity, Visual visual, float partial) {
        visual.machine.context.queries = (name, args) -> query(entity, visual, name, args, partial);
        Speech speech = speech(entity, partial);
        visual.machine.frame((entity.tickCount + partial) / 20d, speech.name, speech.time);
    }

    private record Speech(String name, double time) {}

    private static Speech speech(LivingEntity entity, float partial) {
        NewsNetwork.Update update = STATES.get(entity.getUUID());
        if (update == null || update.animation() == null || update.animation().isEmpty()) return new Speech("", 0);
        double elapsed = (entity.level().getGameTime() - update.start() + partial) / 20d;
        JsonObject line = OriginalData.LINES.get(update.animation());
        double duration = line == null ? 8d : OriginalData.number(line, "duration", 8d);
        if (elapsed < 0 || elapsed > duration) return new Speech("", 0);
        return new Speech(update.animation(), elapsed);
    }

    private static CompoundTag state(LivingEntity entity) {
        NewsNetwork.Update update = STATES.get(entity.getUUID());
        if (update != null && update.state() != null && !update.state().isEmpty()) return update.state();
        return ActorState.tag(entity);
    }

    private static Object query(LivingEntity entity, Visual visual, String name, List<Object> args, float partial) {
        return switch (name) {
            case "is_baby" -> entity.isBaby() ? 1d : 0d;
            case "is_alive" -> entity.isAlive() ? 1d : 0d;
            case "is_sleeping" -> entity.isSleeping() ? 1d : 0d;
            case "is_on_ground" -> entity.onGround() ? 1d : 0d;
            case "is_in_water" -> entity.isInWater() ? 1d : 0d;
            case "is_riding" -> entity.isPassenger() ? 1d : 0d;
            case "is_first_person", "is_in_ui" -> 0d;
            case "life_time" -> (entity.tickCount + partial) / 20d;
            case "frame_alpha" -> (double) partial;
            case "body_y_rotation" -> (double) Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
            case "target_y_rotation" -> (double) Mth.rotLerp(partial, entity.yHeadRotO, entity.yHeadRot);
            case "target_x_rotation" -> (double) Mth.lerp(partial, entity.xRotO, entity.getXRot());
            case "ground_speed" -> Math.sqrt(Mth.square(entity.getX() - entity.xo) + Mth.square(entity.getZ() - entity.zo)) * 20d;
            case "vertical_speed" -> (entity.getY() - entity.yo) * 20d;
            case "variant" -> (double) profession(entity);
            case "mark_variant", "skin_id" -> (double) biome(entity);
            case "trade_tier" -> entity instanceof Villager villager ? Math.max(0, villager.getVillagerData().getLevel() - 1) : 0d;
            case "graphics_mode_is_any" -> args.stream().anyMatch(arg -> "fancy".equals(Molang.str(arg))) ? 1d : 0d;
            case "any" -> any(args);
            case "position" -> position(entity, args);
            case "property" -> args.isEmpty() ? 0d : ActorState.property(state(entity), visual.id, Molang.str(args.get(0)));
            case "has_property" -> !args.isEmpty() && ActorState.has(state(entity), visual.id, Molang.str(args.get(0))) ? 1d : 0d;
            case "is_name_any" -> named(entity, args);
            default -> 0d;
        };
    }

    private static int profession(LivingEntity entity) {
        if (!(entity instanceof Villager villager)) return 0;
        int index = PROFESSIONS.indexOf(villager.getVillagerData().getProfession());
        return index < 0 ? 14 : index;
    }

    private static int biome(LivingEntity entity) {
        if (!(entity instanceof Villager villager)) return 0;
        int index = BIOMES.indexOf(villager.getVillagerData().getType());
        return Math.max(index, 0);
    }

    private static double any(List<Object> args) {
        if (args.size() < 2) return 0d;
        double value = Molang.num(args.get(0));
        for (int i = 1; i < args.size(); i++) if (Molang.num(args.get(i)) == value) return 1d;
        return 0d;
    }

    private static double position(LivingEntity entity, List<Object> args) {
        int axis = args.isEmpty() ? 0 : (int) Molang.num(args.get(0));
        return switch (axis) {
            case 0 -> entity.getX();
            case 1 -> entity.getY();
            case 2 -> entity.getZ();
            default -> 0d;
        };
    }

    private static double named(LivingEntity entity, List<Object> args) {
        String name = entity.getName().getString();
        for (Object arg : args) if (name.equals(Molang.str(arg))) return 1d;
        return 0d;
    }
}
