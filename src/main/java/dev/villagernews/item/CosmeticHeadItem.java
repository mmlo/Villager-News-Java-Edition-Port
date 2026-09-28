package dev.villagernews.item;

import dev.villagernews.client.CosmeticHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class CosmeticHeadItem extends ArmorItem {
    public final String id;

    public CosmeticHeadItem(String id) {
        super(CosmeticArmorMaterial.COSMETIC, Type.HELMET, new Properties().stacksTo(64));
        this.id = id;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original) {
                return CosmeticHeadLayer.getEmptyArmorModel();
            }
        });
    }
}
