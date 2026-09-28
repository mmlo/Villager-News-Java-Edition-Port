package dev.villagernews.data;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginalDataTest {
    @Test
    void clientEntitiesResolveGeometry() {
        JsonObject entities = OriginalData.section("entities");
        JsonObject geometry = OriginalData.section("geometry");
        List<String> missing = new ArrayList<>();
        assertFalse(entities.keySet().isEmpty());
        for (var entity : entities.entrySet()) {
            JsonObject description = entity.getValue().getAsJsonObject();
            JsonObject geometries = description.getAsJsonObject("geometry");
            if (geometries == null || geometries.size() == 0) missing.add(entity.getKey() + " has no geometry");
            else for (var entry : geometries.entrySet()) {
                String id = entry.getValue().getAsString();
                if (!geometry.has(id)) missing.add(entity.getKey() + " -> " + id);
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    @Test
    void ptBrLocalizationAndGuideAreValid() {
        assertFalse(OriginalData.GUIDE_PT == null, "GUIDE_PT should be loaded");
        assertTrue(OriginalData.GUIDE_PT.size() > 0, "GUIDE_PT should contain handbook sections");
        
        JsonObject ptLang = OriginalData.load("lang/pt_br.json");
        assertFalse(ptLang == null, "pt_br.json should exist");
        assertTrue(ptLang.size() >= 3800, "pt_br.json should contain translations for all items, entities and subtitles");
        assertTrue(ptLang.has("item.oreville_vn.kfjmlk"), "Should have Portuguese manual item name");
        assertTrue(ptLang.has("item.oreville_vn.dsojot"), "Should have Portuguese microphone item name");
        assertTrue(ptLang.has("entity.oreville_vn.ilvfra"), "Should have Portuguese mayor entity name");

        // Check sounds.json has pt_br sound definitions
        assertTrue(OriginalData.SOUNDS.has("pt_br.dqhfqu"), "Should have pt_br sound registered in sounds.json");
    }

    @Test
    void itemModelsAndTexturesAreConsistent() throws Exception {
        List<String> items = List.of("kfjmlk", "cryhjc", "dsojot", "odplew", "ufernq", "qzhdgf");
        List<String> missing = new ArrayList<>();
        for (String id : items) {
            JsonObject model = OriginalData.load("models/item/" + id + ".json");
            JsonObject textures = model.getAsJsonObject("textures");
            assertFalse(textures == null, "Model " + id + " should have textures block");
            for (var entry : textures.entrySet()) {
                String textureRes = entry.getValue().getAsString();
                String relPath = textureRes.replace("oreville_vn:", "") + ".png";
                try (var is = OriginalData.class.getResourceAsStream("/assets/oreville_vn/textures/" + relPath)) {
                    if (is == null) {
                        missing.add("Item " + id + " missing texture: " + relPath);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing item textures:\n" + String.join("\n", missing));
    }

    @Test
    void allSoundsInRegistryHaveValidAudioFiles() throws Exception {
        List<String> missing = new ArrayList<>();
        int countChecked = 0;
        for (var entry : OriginalData.SOUNDS.entrySet()) {
            JsonObject soundObj = entry.getValue().getAsJsonObject();
            var soundsArray = soundObj.getAsJsonArray("sounds");
            if (soundsArray != null) {
                for (var s : soundsArray) {
                    countChecked++;
                    String soundPath = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                    String relPath = soundPath.replace("oreville_vn:", "") + ".ogg";
                    try (var is = OriginalData.class.getResourceAsStream("/assets/oreville_vn/sounds/" + relPath)) {
                        if (is == null) {
                            missing.add("Sound key " + entry.getKey() + " missing file: " + relPath);
                        } else {
                            byte[] header = new byte[4];
                            int read = is.read(header);
                            if (read < 4 || header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S') {
                                missing.add("Sound file corrupted / not OggS header: " + relPath);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(countChecked > 4000, "Should have checked over 4000 sound references, but found: " + countChecked);
        assertTrue(missing.isEmpty(), "Sound files missing or invalid (" + missing.size() + "):\n" + String.join("\n", missing.subList(0, Math.min(20, missing.size()))));
    }

    @Test
    void dualLanguageTranslationsCoverAllEntitiesAndItems() {
        JsonObject ptLang = OriginalData.load("lang/pt_br.json");
        JsonObject enLang = OriginalData.load("lang/en_us.json");
        List<String> entityIds = List.of("villager", "ilvfra", "poztxf", "vwpagn", "xcrjxf", "ghibss", "mlkxjo", "txczvv");
        List<String> itemIds = List.of("kfjmlk", "cryhjc", "dsojot", "odplew", "ufernq", "qzhdgf");

        for (String id : entityIds) {
            assertTrue(ptLang.has("entity.oreville_vn." + id), "pt_br missing entity: " + id);
            assertTrue(enLang.has("entity.oreville_vn." + id), "en_us missing entity: " + id);
        }
        for (String id : itemIds) {
            assertTrue(ptLang.has("item.oreville_vn." + id), "pt_br missing item: " + id);
            assertTrue(enLang.has("item.oreville_vn." + id), "en_us missing item: " + id);
        }
    }

    @Test
    void allBedrockDialogueTriggersAndCosmeticsResolve() throws Exception {
        JsonObject geometry = OriginalData.section("geometry");
        List<String> triggers = List.of(
            "pkrkml", "ueviuf", "kmkrwf", "bbkhrp", "egqivb", "fsepbc",
            "okjbgd", "wylybx", "wlacvx", "vqqkna", "bmceek", "amhagx",
            "iyhtlh", "tvlnxx", "ngfhdb", "oqkfej", "nkkrdb", "rsifiu",
            "vyabfg", "ygtfyv"
        );

        for (String trigger : triggers) {
            assertTrue(OriginalData.GROUPS.containsKey(trigger), "Dialogue group trigger missing: " + trigger);
            JsonObject group = OriginalData.GROUPS.get(trigger);
            var slhkqn = group.getAsJsonArray("slhkqn");
            assertTrue(slhkqn != null && slhkqn.size() > 0, "Dialogue group " + trigger + " has no variants");
        }

        // Validate cosmetics mapping and 3D geometries
        Map<String, String> cosmeticGeometries = Map.of(
            "oreville_vn:qzhdgf", "geometry.oreville_vn.834522044",
            "oreville_vn:odplew", "geometry.oreville_vn.1940352316",
            "oreville_vn:cryhjc", "geometry.oreville_vn.1064568764",
            "oreville_vn:ufernq", "geometry.oreville_vn.-1144631652"
        );

        for (var entry : cosmeticGeometries.entrySet()) {
            assertTrue(geometry.has(entry.getValue()), "Cosmetic " + entry.getKey() + " geometry missing: " + entry.getValue());
            JsonObject geo = geometry.getAsJsonObject(entry.getValue());
            var bones = geo.getAsJsonArray("bones");
            assertTrue(bones != null && bones.size() > 0, "Geometry " + entry.getValue() + " must have bones");
        }
    }
}
