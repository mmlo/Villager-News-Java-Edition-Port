package dev.villagernews.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CosmeticHeadLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public record CosmeticInfo(String geometryId, ResourceLocation texture) {}

    public static final Map<String, CosmeticInfo> COSMETICS = Map.of(
        "oreville_vn:qzhdgf", new CosmeticInfo("geometry.oreville_vn.834522044", new ResourceLocation("oreville_vn", "textures/oreville/vn/dil.png")),
        "oreville_vn:odplew", new CosmeticInfo("geometry.oreville_vn.1940352316", new ResourceLocation("oreville_vn", "textures/oreville/vn/ebc.png")),
        "oreville_vn:cryhjc", new CosmeticInfo("geometry.oreville_vn.1064568764", new ResourceLocation("oreville_vn", "textures/oreville/vn/eba.png")),
        "oreville_vn:ufernq", new CosmeticInfo("geometry.oreville_vn.-1144631652", new ResourceLocation("oreville_vn", "textures/oreville/vn/ebd.png"))
    );

    private static final EmptyArmorModel EMPTY_ARMOR_MODEL = new EmptyArmorModel();

    public CosmeticHeadLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    public static HumanoidModel<?> getEmptyArmorModel() {
        return EMPTY_ARMOR_MODEL;
    }

    public static class EmptyArmorModel extends HumanoidModel<LivingEntity> {
        public EmptyArmorModel() {
            super(createBlankPart());
        }

        private static ModelPart createBlankPart() {
            Map<String, ModelPart> children = new HashMap<>();
            ModelPart emptyPart = new ModelPart(Collections.emptyList(), Collections.emptyMap());
            children.put("head", emptyPart);
            children.put("hat", emptyPart);
            children.put("body", emptyPart);
            children.put("right_arm", emptyPart);
            children.put("left_arm", emptyPart);
            children.put("right_leg", emptyPart);
            children.put("left_leg", emptyPart);
            return new ModelPart(Collections.emptyList(), children);
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            // Suppress standard armor rendering completely
        }
    }

    private static final Map<net.minecraft.world.item.Item, CosmeticInfo> ITEM_COSMETICS = new java.util.concurrent.ConcurrentHashMap<>();

    private static CosmeticInfo getCosmetic(net.minecraft.world.item.Item item) {
        return ITEM_COSMETICS.computeIfAbsent(item, i -> {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(i);
            return key == null ? null : COSMETICS.get(key.toString());
        });
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.isEmpty()) return;

        CosmeticInfo info = getCosmetic(headItem.getItem());
        if (info == null) return;

        M model = this.getParentModel();
        ModelPart headPart = null;
        if (model instanceof HeadedModel headed) {
            headPart = headed.getHead();
        }

        if (headPart == null) return;

        poseStack.pushPose();
        headPart.translateAndRotate(poseStack);

        // Bedrock head mapping:
        // Neck is at Y=24 in Bedrock space.
        // In Java head model space, neck is at (0, 0, 0), with Y downward and X inverted.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5F, 0.0F);

        BedrockRenderer.renderStandalone(info.geometryId(), info.texture(), poseStack, buffers, light);

        poseStack.popPose();
    }
}
