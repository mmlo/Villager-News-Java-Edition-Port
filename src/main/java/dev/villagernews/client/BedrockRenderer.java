package dev.villagernews.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.villagernews.data.OriginalData;
import dev.villagernews.runtime.Molang;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.villagernews.data.OriginalData.*;

public final class BedrockRenderer {
    private static final class BakedCube {
        final boolean hasTranslate;
        final float tx, ty, tz;
        final boolean hasRotate;
        final float rx, ry, rz;
        final int faceCount;
        final float[] data;

        BakedCube(boolean hasTranslate, float tx, float ty, float tz, boolean hasRotate, float rx, float ry, float rz, int faceCount, float[] data) {
            this.hasTranslate = hasTranslate;
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.hasRotate = hasRotate;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.faceCount = faceCount;
            this.data = data;
        }

        void render(PoseStack stack, VertexConsumer out, int light, int overlay, float r, float g, float b, float a, float uOff, float vOff) {
            stack.pushPose();
            if (hasTranslate) stack.translate(tx, ty, tz);
            if (hasRotate) rotate(stack, rx, ry, rz);
            var poseMat = stack.last().pose();
            var normMat = stack.last().normal();
            int idx = 0;
            for (int f = 0; f < faceCount; f++) {
                for (int i = 0; i < 4; i++) {
                    float x = data[idx++];
                    float y = data[idx++];
                    float z = data[idx++];
                    float u = data[idx++] + uOff;
                    float v = data[idx++] + vOff;
                    float nx = data[idx++];
                    float ny = data[idx++];
                    float nz = data[idx++];
                    out.vertex(poseMat, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light).normal(normMat, nx, ny, nz).endVertex();
                }
            }
            stack.popPose();
        }
    }

    private record Bone(String name, String parent, double[] pivot, double[] rotation, double[] scale, List<BakedCube> cubes) {}
    private record Geometry(double width, double height, List<Bone> roots, Map<String, List<Bone>> children) {}

    private static final Map<String, Geometry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final AnimationMachine.Transform IDENTITY_TRANSFORM = new AnimationMachine.Transform();
    private static final double[] ORIGIN = {0, 0, 0};
    private static final String[] CATEGORIES = {"geometry", "textures", "materials"};

    public static void render(LivingEntity entity, float partial, PoseStack stack, MultiBufferSource buffers, int light, ClientRuntime.Visual visual) {
        var machine = visual.machine;
        JsonObject desc = visual.description;
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180 - net.minecraft.util.Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot)));
        double scale = desc.has("scripts") && object(desc, "scripts").has("scale") ? machine.number(object(desc, "scripts").get("scale")) : 1;
        if (!Double.isFinite(scale)) scale = 1;
        stack.scale((float) scale, (float) scale, (float) scale);
        for (JsonElement entry : array(desc, "render_controllers")) {
            if (entry.isJsonPrimitive()) controller(entry.getAsString(), machine, desc, stack, buffers, light, entity.hurtTime > 0);
            else for (var e : entry.getAsJsonObject().entrySet()) if (Molang.truth(machine.evaluate(e.getValue()))) controller(e.getKey(), machine, desc, stack, buffers, light, entity.hurtTime > 0);
        }
        stack.popPose();
    }

    private static void controller(String id, AnimationMachine machine, JsonObject desc, PoseStack stack, MultiBufferSource buffers, int light, boolean hurt) {
        JsonObject render = object(OriginalData.section("renders"), id);
        if (render.size() == 0) return;
        for (String category : CATEGORIES) {
            for (var e : object(desc, category).entrySet()) {
                machine.context.set((category.equals("textures") ? "texture" : category.equals("materials") ? "material" : "geometry") + "." + e.getKey(), e.getValue().getAsString());
            }
        }
        for (var category : object(render, "arrays").entrySet()) {
            for (var a : category.getValue().getAsJsonObject().entrySet()) {
                List<Object> values = new ArrayList<>();
                for (JsonElement e : a.getValue().getAsJsonArray()) values.add(machine.evaluate(e));
                machine.context.set(a.getKey(), values);
            }
        }
        String geometry = Molang.str(machine.evaluate(render.get("geometry")));
        Geometry geo = CACHE.computeIfAbsent(geometry, BedrockRenderer::bake);
        if (geo == null) return;
        Map<String, Boolean> visible = new LinkedHashMap<>();
        for (JsonElement e : array(render, "part_visibility")) {
            for (var p : e.getAsJsonObject().entrySet()) visible.put(p.getKey(), Molang.truth(machine.evaluate(p.getValue())));
        }
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(hurt));
        float r = 1, g = 1, b = 1, a = 1;
        JsonObject tint = object(render, "color");
        if (tint.size() > 0) {
            r = (float) machine.number(tint.get("r"));
            g = (float) machine.number(tint.get("g"));
            b = (float) machine.number(tint.get("b"));
            a = (float) machine.number(tint.get("a"));
        }
        for (JsonElement texture : array(render, "textures")) {
            String path = Molang.str(machine.evaluate(texture));
            if (path.isBlank()) continue;
            ResourceLocation loc = resolveTexture(path);
            double uOffset = 0, vOffset = 0;
            JsonObject uvAnim = object(render, "uv_anim");
            if (uvAnim.size() > 0 && uvAnim.has("offset")) {
                JsonArray off = uvAnim.getAsJsonArray("offset");
                uOffset = machine.number(off.get(0));
                vOffset = machine.number(off.get(1));
            }
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(loc));
            for (Bone root : geo.roots) {
                bone(root, ORIGIN, geo, machine, visible, stack, consumer, light, overlay, r, g, b, a, uOffset, vOffset, 0);
            }
        }
    }

    public static void renderStandalone(String geometryId, ResourceLocation texture, PoseStack stack, MultiBufferSource buffers, int light) {
        Geometry geo = CACHE.computeIfAbsent(geometryId, BedrockRenderer::bake);
        if (geo == null) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (Bone root : geo.roots) {
            bone(root, ORIGIN, geo, null, Collections.emptyMap(), stack, consumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f, 0, 0, 0);
        }
    }

    private static ResourceLocation resolveTexture(String path) {
        return TEXTURE_CACHE.computeIfAbsent(path, BedrockRenderer::computeTexture);
    }

    private static ResourceLocation computeTexture(String path) {
        String clean = path.endsWith(".png") ? path : path + ".png";
        if (clean.startsWith("textures/entity/sign")) {
            String wood = clean.replace("textures/entity/sign", "").replace(".png", "").replace("_", "");
            if (wood.isEmpty()) wood = "oak";
            if ("darkoak".equals(wood)) wood = "dark_oak";
            return new ResourceLocation("minecraft", "textures/entity/signs/" + wood + ".png");
        }
        if (clean.startsWith("textures/entity/")) {
            String name = clean.replace("textures/entity/", "").replace(".png", "");
            if (name.endsWith("_sign")) {
                String wood = name.replace("_sign", "");
                return new ResourceLocation("minecraft", "textures/entity/signs/" + wood + ".png");
            }
            return new ResourceLocation("minecraft", clean);
        }
        return new ResourceLocation("oreville_vn", clean);
    }

    private static Geometry bake(String id) {
        JsonObject obj = object(OriginalData.section("geometry"), id);
        if (obj.size() == 0) return null;
        var d = object(obj, "description");
        double texWidth = number(d, "texture_width", 64);
        double texHeight = number(d, "texture_height", 64);
        List<Bone> roots = new ArrayList<>();
        Map<String, List<Bone>> children = new HashMap<>();
        Set<String> names = new HashSet<>();
        for (JsonElement v : array(obj, "bones")) names.add(string(v.getAsJsonObject(), "name", ""));
        for (JsonElement e : array(obj, "bones")) {
            var o = e.getAsJsonObject();
            double[] pivot = vec(o.get("pivot"), 0);
            List<BakedCube> cubes = new ArrayList<>();
            for (JsonElement c : array(o, "cubes")) {
                BakedCube bc = bakeCube(c.getAsJsonObject(), pivot, texWidth, texHeight);
                if (bc != null) cubes.add(bc);
            }
            Bone bone = new Bone(string(o, "name", ""), string(o, "parent", ""), pivot, vec(o.get("rotation"), 0), vec(o.get("scale"), 1), cubes);
            if (bone.parent.isEmpty() || !names.contains(bone.parent)) roots.add(bone);
            else children.computeIfAbsent(bone.parent, k -> new ArrayList<>()).add(bone);
        }
        return new Geometry(texWidth, texHeight, roots, children);
    }

    private static BakedCube bakeCube(JsonObject cube, double[] bonePivot, double geoWidth, double geoHeight) {
        double[] origin = vec(cube.get("origin"), 0);
        double[] size = vec(cube.get("size"), 0);
        double[] pivot = cube.has("pivot") ? vec(cube.get("pivot"), 0) : bonePivot;
        double[] rot = vec(cube.get("rotation"), 0);
        double inflate = number(cube, "inflate", 0);

        boolean hasTranslate = (pivot[0] != bonePivot[0]) || (pivot[1] != bonePivot[1]) || (pivot[2] != bonePivot[2]);
        float tx = (float) ((pivot[0] - bonePivot[0]) / 16.0);
        float ty = (float) ((pivot[1] - bonePivot[1]) / 16.0);
        float tz = (float) ((pivot[2] - bonePivot[2]) / 16.0);

        boolean hasRotate = (rot[0] != 0) || (rot[1] != 0) || (rot[2] != 0);
        float rx = (float) rot[0], ry = (float) rot[1], rz = (float) rot[2];

        float x0 = (float) (origin[0] - pivot[0] - inflate) / 16f;
        float y0 = (float) (origin[1] - pivot[1] - inflate) / 16f;
        float z0 = (float) (origin[2] - pivot[2] - inflate) / 16f;
        float x1 = (float) (origin[0] + size[0] - pivot[0] + inflate) / 16f;
        float y1 = (float) (origin[1] + size[1] - pivot[1] + inflate) / 16f;
        float z1 = (float) (origin[2] + size[2] - pivot[2] + inflate) / 16f;

        float[][][] points = {
            {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}}, // north
            {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}}, // south
            {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}}, // west
            {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}}, // east
            {{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}}, // up
            {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}}  // down
        };
        String[] faces = {"north", "south", "west", "east", "up", "down"};
        float[][] normals = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

        JsonElement uv = cube.get("uv");
        if (uv == null) return null;

        double u = uv.isJsonArray() ? uv.getAsJsonArray().get(0).getAsDouble() : 0;
        double v = uv.isJsonArray() ? uv.getAsJsonArray().get(1).getAsDouble() : 0;
        double dx = size[0], dy = size[1], dz = size[2];
        double[][] box = {
            {u + dz, v + dz, dx, dy},
            {u + 2 * dz + dx, v + dz, dx, dy},
            {u + dz + dx, v + dz, dz, dy},
            {u, v + dz, dz, dy},
            {u + dz, v, dx, dz},
            {u + dz + dx, v + dz, dx, -dz}
        };

        float[] buffer = new float[6 * 32];
        int faceCount = 0;
        int idx = 0;

        for (int f = 0; f < 6; f++) {
            double[] t = box[f];
            if (uv.isJsonObject()) {
                var face = object(uv.getAsJsonObject(), faces[f]);
                if (face.size() == 0) continue;
                var p = face.getAsJsonArray("uv");
                var s = face.getAsJsonArray("uv_size");
                t = new double[]{p.get(0).getAsDouble(), p.get(1).getAsDouble(), s.get(0).getAsDouble(), s.get(1).getAsDouble()};
            }
            float u0 = (float) (t[0] / geoWidth);
            float v0 = (float) (t[1] / geoHeight);
            float u1 = (float) ((t[0] + t[2]) / geoWidth);
            float v1 = (float) ((t[1] + t[3]) / geoHeight);
            if (bool(cube, "mirror", false)) {
                float temp = u0; u0 = u1; u1 = temp;
            }
            float[][] coords = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};

            for (int i = 0; i < 4; i++) {
                float[] p = points[f][i];
                buffer[idx++] = p[0];
                buffer[idx++] = p[1];
                buffer[idx++] = p[2];
                buffer[idx++] = coords[i][0];
                buffer[idx++] = coords[i][1];
                buffer[idx++] = normals[f][0];
                buffer[idx++] = normals[f][1];
                buffer[idx++] = normals[f][2];
            }
            faceCount++;
        }

        float[] data = Arrays.copyOf(buffer, idx);
        return new BakedCube(hasTranslate, tx, ty, tz, hasRotate, rx, ry, rz, faceCount, data);
    }

    private static boolean matchPattern(String text, String pattern) {
        if (pattern.equals("*") || pattern.equals(text)) return true;
        if (!pattern.contains("*")) return false;
        if (pattern.startsWith("*") && pattern.endsWith("*") && pattern.length() > 2) {
            return text.contains(pattern.subSequence(1, pattern.length() - 1));
        }
        if (pattern.endsWith("*")) {
            return text.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return text.endsWith(pattern.substring(1));
        }
        return text.matches(pattern.replace("*", ".*"));
    }

    private static void bone(Bone bone, double[] parent, Geometry geo, AnimationMachine machine, Map<String, Boolean> visible, PoseStack stack, VertexConsumer out, int light, int overlay, float r, float g, float b, float a, double uOff, double vOff, int depth) {
        if (depth > 64) return;
        boolean show = true;
        if (visible != null && !visible.isEmpty()) {
            for (var e : visible.entrySet()) {
                if (matchPattern(bone.name, e.getKey())) show = e.getValue();
            }
        }
        var anim = (machine != null && machine.pose != null) ? machine.pose.get(bone.name) : null;
        if (anim == null) anim = IDENTITY_TRANSFORM;

        stack.pushPose();
        stack.translate((bone.pivot[0] - parent[0] + anim.position[0]) / 16, (bone.pivot[1] - parent[1] + anim.position[1]) / 16, (bone.pivot[2] - parent[2] + anim.position[2]) / 16);
        rotate(stack, bone.rotation[0] + anim.rotation[0], bone.rotation[1] + anim.rotation[1], bone.rotation[2] + anim.rotation[2]);
        stack.scale((float) (bone.scale[0] * anim.scale[0]), (float) (bone.scale[1] * anim.scale[1]), (float) (bone.scale[2] * anim.scale[2]));
        if (show) {
            float fuOff = (float) uOff;
            float fvOff = (float) vOff;
            for (var cube : bone.cubes) cube.render(stack, out, light, overlay, r, g, b, a, fuOff, fvOff);
        }
        List<Bone> children = geo.children.get(bone.name);
        if (children != null) {
            for (Bone child : children) {
                bone(child, bone.pivot, geo, machine, visible, stack, out, light, overlay, r, g, b, a, uOff, vOff, depth + 1);
            }
        }
        stack.popPose();
    }

    private static void rotate(PoseStack p, double x, double y, double z) {
        if (z != 0) p.mulPose(Axis.ZP.rotationDegrees((float) z));
        if (y != 0) p.mulPose(Axis.YP.rotationDegrees((float) y));
        if (x != 0) p.mulPose(Axis.XP.rotationDegrees((float) x));
    }

    public static double[] vec(JsonElement v, double fallback) {
        double[] out = {fallback, fallback, fallback};
        if (v == null) return out;
        for (int i = 0; i < 3; i++) out[i] = v.isJsonArray() ? v.getAsJsonArray().get(i).getAsDouble() : v.getAsDouble();
        return out;
    }
}