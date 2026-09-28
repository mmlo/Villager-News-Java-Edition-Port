package dev.villagernews.client;

import dev.villagernews.VillagerNews;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;

public class NewsRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    public NewsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(T entity, float entityYaw, float partial, PoseStack stack, MultiBufferSource buffers, int light) {
        ClientRuntime.Visual visual = ClientRuntime.visual(entity);
        if (visual != null) {
            ClientRuntime.beforeRender(entity, visual, partial);
            BedrockRenderer.render(entity, partial, stack, buffers, light, visual);
        }
        super.render(entity, entityYaw, partial, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return new ResourceLocation(VillagerNews.ID, "textures/oreville/vn/dja.png");
    }
}
