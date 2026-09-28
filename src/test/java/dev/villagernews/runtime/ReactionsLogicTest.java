package dev.villagernews.runtime;

import dev.villagernews.VillagerNews;
import dev.villagernews.client.CosmeticHeadLayer;
import dev.villagernews.item.CosmeticHeadItem;
import dev.villagernews.item.GuideBookItem;
import dev.villagernews.item.MicrophoneItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReactionsLogicTest {

    @Test
    void cosmeticItemsSuppressVanillaArmorModel() {
        HumanoidModel<?> emptyModel = CosmeticHeadLayer.getEmptyArmorModel();
        assertNotNull(emptyModel);
        assertNotNull(emptyModel.head);
        assertNotNull(emptyModel.hat);
        assertNotNull(emptyModel.body);
        assertNotNull(emptyModel.rightArm);
        assertNotNull(emptyModel.leftArm);
        assertNotNull(emptyModel.rightLeg);
        assertNotNull(emptyModel.leftLeg);
    }

    @Test
    void cosmeticLayersRegisteredForHeadWearables() {
        assertTrue(CosmeticHeadLayer.COSMETICS.containsKey("oreville_vn:qzhdgf"), "Nose cosmetic must be in CosmeticHeadLayer");
        assertTrue(CosmeticHeadLayer.COSMETICS.containsKey("oreville_vn:odplew"), "Moustache cosmetic must be in CosmeticHeadLayer");
        assertTrue(CosmeticHeadLayer.COSMETICS.containsKey("oreville_vn:cryhjc"), "Mayor Hat cosmetic must be in CosmeticHeadLayer");
        assertTrue(CosmeticHeadLayer.COSMETICS.containsKey("oreville_vn:ufernq"), "Helmet cosmetic must be in CosmeticHeadLayer");

        for (var entry : CosmeticHeadLayer.COSMETICS.entrySet()) {
            assertNotNull(entry.getValue().geometryId());
            assertNotNull(entry.getValue().texture());
            assertFalse(entry.getValue().geometryId().isEmpty());
            assertTrue(entry.getValue().texture().getPath().endsWith(".png"));
        }
    }

    @Test
    void actorStateDefaultsAndPropertiesMatchBedrockDefinitions() {
        CompoundTag villagerTag = new CompoundTag();
        assertEquals("none", ActorState.property(villagerTag, "oreville_vn:villager", "p:mlxeez"));
        assertEquals(1.0d, (Double) ActorState.property(villagerTag, "oreville_vn:villager", "p:gcfsvg"), 0.001);
        assertEquals(-1.0d, (Double) ActorState.property(villagerTag, "oreville_vn:villager", "p:sign"), 0.001);

        CompoundTag woolyTag = new CompoundTag();
        assertEquals(0.0d, (Double) ActorState.property(woolyTag, "oreville_vn:mlkxjo", "p:gmtjzx"), 0.001);

        CompoundTag ghibssTag = new CompoundTag();
        assertEquals(0.0d, (Double) ActorState.property(ghibssTag, "oreville_vn:ghibss", "p:zsuvyv"), 0.001);
    }
}
