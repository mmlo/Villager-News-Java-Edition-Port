package dev.villagernews.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.villagernews.VillagerNews;
import dev.villagernews.network.NewsNetwork;
import dev.villagernews.runtime.ActorState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = VillagerNews.ID, value = Dist.CLIENT)
public final class ClientAutomation {
    private static boolean active = false;
    private static int step = 0;
    private static int delay = 0;
    private static boolean worldRequested = false;
    private static int worldLoadDelay = 0;

    private static double baseX = 0.0;
    private static double baseY = 140.0;
    private static double baseZ = 0.0;

    private static final List<Entity> spawnedEntities = new ArrayList<>();
    private static Villager testVillager = null;
    private static Sheep testWooly = null;

    private static final double[] LINEUP_OFFSETS = {-5.6, -4.0, -2.4, -0.8, 0.8, 2.4, 4.0, 5.6};

    static {
        if ("true".equalsIgnoreCase(System.getProperty("vn.autotest"))) {
            active = true;
        }
    }

    public static void start() {
        active = true;
        step = 0;
        delay = 0;
        worldRequested = false;
        worldLoadDelay = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        // 1. If at Title Screen, automatically request loading of "Novo mundo"
        if (mc.screen instanceof TitleScreen && !worldRequested) {
            delay++;
            if (delay > 30) {
                worldRequested = true;
                delay = 0;
                System.out.println("[VillagerNews E2E] Loading singleplayer world 'Novo mundo'...");
                mc.createWorldOpenFlows().loadLevel(mc.screen, "Novo mundo");
            }
            return;
        }

        // 2. Wait until world loading screen is completely gone (mc.screen must be null or GuideScreen)
        if (mc.screen != null && !(mc.screen instanceof GuideScreen)) {
            return;
        }

        // 3. Wait until player and level are ready
        if (mc.player == null || mc.level == null) return;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        ServerLevel overworld = server.overworld();

        // Give chunks a short buffer to settle after world finishes loading
        if (step == 0 && worldLoadDelay < 30) {
            worldLoadDelay++;
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        switch (step) {
            case 0 -> {
                System.out.println("[VillagerNews E2E] Initializing studio staging area...");
                baseX = Math.floor(mc.player.getX());
                baseZ = Math.floor(mc.player.getZ());
                baseY = 140.0;

                server.execute(() -> {
                    overworld.setDayTime(6000);
                    overworld.setWeatherParameters(60000, 0, false, false);
                    sp.setGameMode(GameType.CREATIVE);
                    sp.getInventory().clearContent();

                    // Build 40x40 platform at baseY - 1 (Y=139)
                    int bx = (int) baseX;
                    int bz = (int) baseZ;
                    int by = (int) baseY;
                    for (int x = bx - 20; x <= bx + 20; x++) {
                        for (int z = bz - 10; z <= bz + 30; z++) {
                            overworld.setBlockAndUpdate(new BlockPos(x, by - 1, z), Blocks.WHITE_CONCRETE.defaultBlockState());
                            for (int y = by; y <= by + 12; y++) {
                                overworld.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                            }
                        }
                    }

                    // Clear non-player entities in overworld
                    for (Entity e : overworld.getAllEntities()) {
                        if (!(e instanceof Player)) e.discard();
                    }
                });

                positionPlayer(mc, sp, overworld, baseX, baseY, baseZ, 180.0F, 0.0F);
                mc.options.hideGui = true;
                step++;
                delay = 25;
            }

            // ========================================================
            // Group A: Player Using / Wearing Each Item
            // ========================================================
            case 1 -> {
                // Shot 1: Player wearing Villager Nose (qzhdgf)
                server.execute(() -> sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("qzhdgf"))));
                mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("qzhdgf")));
                positionPlayer(mc, sp, overworld, baseX, baseY, baseZ, 180.0F, 0.0F);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.fov().set(35);
                step++;
                delay = 20;
            }
            case 2 -> {
                saveScreenshot("01_player_villager_nose.png");
                // Shot 2: Player wearing Moustache (odplew)
                server.execute(() -> sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("odplew"))));
                mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("odplew")));
                step++;
                delay = 20;
            }
            case 3 -> {
                saveScreenshot("02_player_moustache.png");
                // Shot 3: Player wearing Mayor Hat (cryhjc)
                server.execute(() -> sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("cryhjc"))));
                mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("cryhjc")));
                step++;
                delay = 20;
            }
            case 4 -> {
                saveScreenshot("03_player_mayor_hat.png");
                // Shot 4: Player wearing Testificate Helmet (ufernq)
                server.execute(() -> sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("ufernq"))));
                mc.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(VillagerNews.item("ufernq")));
                step++;
                delay = 20;
            }
            case 5 -> {
                saveScreenshot("04_player_testificate_helmet.png");
                // Shot 5: Player holding Microphone (dsojot) in 3rd person front
                ItemStack mic = new ItemStack(VillagerNews.item("dsojot"));
                server.execute(() -> {
                    sp.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    sp.setItemInHand(InteractionHand.MAIN_HAND, mic);
                });
                mc.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, mic);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.fov().set(42);
                step++;
                delay = 20;
            }
            case 6 -> {
                saveScreenshot("05_player_microphone_hand.png");
                // Shot 6: Guide Book Manual GUI (kfjmlk)
                mc.options.hideGui = false;
                mc.setScreen(new GuideScreen());
                step++;
                delay = 25;
            }
            case 7 -> {
                saveScreenshot("06_gui_guide_manual.png");
                mc.setScreen(null);
                mc.options.hideGui = true;

                // ========================================================
                // Group B: Spawn Eggs & Spawned Entities Lineup
                // ========================================================
                server.execute(() -> {
                    for (Entity e : spawnedEntities) e.discard();
                    spawnedEntities.clear();

                    List<EntityType<?>> types = List.of(
                        VillagerNews.VILLAGERS.get("villager").get(),
                        VillagerNews.VILLAGERS.get("ilvfra").get(),
                        VillagerNews.VILLAGERS.get("poztxf").get(),
                        VillagerNews.VILLAGERS.get("vwpagn").get(),
                        VillagerNews.VILLAGERS.get("xcrjxf").get(),
                        VillagerNews.VILLAGERS.get("ghibss").get(),
                        VillagerNews.WOOLY.get(),
                        VillagerNews.TRADER.get()
                    );

                    for (int i = 0; i < types.size(); i++) {
                        Entity ent = types.get(i).create(overworld);
                        if (ent != null) {
                            ent.moveTo(baseX + LINEUP_OFFSETS[i], baseY, baseZ + 6.0, 0.0F, 0.0F);
                            if (ent instanceof LivingEntity le) {
                                le.setYRot(0.0F);
                                le.yRotO = 0.0F;
                                le.yHeadRot = 0.0F;
                                le.yHeadRotO = 0.0F;
                                le.yBodyRot = 0.0F;
                                le.yBodyRotO = 0.0F;
                            }
                            if (ent instanceof net.minecraft.world.entity.Mob mob) {
                                mob.setNoAi(true);
                            }
                            overworld.addFreshEntity(ent);
                            ActorState.ensure(ent);
                            if (i == 1) { // Mayor (ilvfra)
                                ActorState.tag(ent).putDouble("p:pmpece", 1d);
                            }
                            NewsNetwork.stateTo(ent, sp);
                            spawnedEntities.add(ent);
                        }
                    }
                });

                positionPlayer(mc, sp, overworld, baseX, baseY + 0.6, baseZ + 13.5, 180.0F, 4.0F);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(75);

                step++;
                delay = 25;
            }
            case 8 -> {
                saveScreenshot("07_all_spawn_eggs_lineup.png");
                // Focus Entity 0: Villager
                focusLineupEntity(mc, sp, overworld, 0);
                step++;
                delay = 20;
            }
            case 9 -> {
                saveScreenshot("08_entity_villager.png");
                // Focus Entity 1: Mayor (ilvfra)
                focusLineupEntity(mc, sp, overworld, 1);
                step++;
                delay = 20;
            }
            case 10 -> {
                saveScreenshot("09_entity_mayor.png");
                // Focus Entity 2: Villager #4 (poztxf)
                focusLineupEntity(mc, sp, overworld, 2);
                step++;
                delay = 20;
            }
            case 11 -> {
                saveScreenshot("10_entity_villager4.png");
                // Focus Entity 3: Villager #5 (vwpagn)
                focusLineupEntity(mc, sp, overworld, 3);
                step++;
                delay = 20;
            }
            case 12 -> {
                saveScreenshot("11_entity_villager5.png");
                // Focus Entity 4: Villager #9 (xcrjxf)
                focusLineupEntity(mc, sp, overworld, 4);
                step++;
                delay = 20;
            }
            case 13 -> {
                saveScreenshot("12_entity_villager9.png");
                // Focus Entity 5: Untouchable (ghibss)
                focusLineupEntity(mc, sp, overworld, 5);
                step++;
                delay = 20;
            }
            case 14 -> {
                saveScreenshot("13_entity_untouchable_ghibss.png");
                // Focus Entity 6: Wooly Sheep (mlkxjo)
                focusLineupEntity(mc, sp, overworld, 6);
                step++;
                delay = 20;
            }
            case 15 -> {
                saveScreenshot("14_entity_wooly_sheep.png");
                // Focus Entity 7: Wandering Trader (txczvv)
                focusLineupEntity(mc, sp, overworld, 7);
                step++;
                delay = 20;
            }
            case 16 -> {
                saveScreenshot("15_entity_trader.png");

                // ========================================================
                // Group C: Results of Using Items on Entities
                // ========================================================
                server.execute(() -> {
                    for (Entity e : spawnedEntities) e.discard();
                    spawnedEntities.clear();

                    testVillager = VillagerNews.VILLAGERS.get("villager").get().create(overworld);
                    if (testVillager != null) {
                        testVillager.moveTo(baseX, baseY, baseZ + 4.0, 0.0F, 0.0F);
                        testVillager.setYRot(0.0F);
                        testVillager.yRotO = 0.0F;
                        testVillager.yHeadRot = 0.0F;
                        testVillager.yHeadRotO = 0.0F;
                        testVillager.yBodyRot = 0.0F;
                        testVillager.yBodyRotO = 0.0F;
                        testVillager.setNoAi(true);
                        overworld.addFreshEntity(testVillager);
                        ActorState.ensure(testVillager);

                        // 16: Result of Shearing Nose (nose cut off)
                        ActorState.tag(testVillager).putDouble("p:gcfsvg", 0d);
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());

                        ItemEntity droppedNose = new ItemEntity(overworld, baseX + 0.25, baseY, baseZ + 4.8, new ItemStack(VillagerNews.item("qzhdgf")));
                        droppedNose.setDeltaMovement(0, 0, 0);
                        overworld.addFreshEntity(droppedNose);
                        spawnedEntities.add(droppedNose);
                    }
                });

                positionPlayer(mc, sp, overworld, baseX, baseY + 0.45, baseZ + 6.5, 180.0F, 8.0F);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(45);
                step++;
                delay = 20;
            }
            case 17 -> {
                saveScreenshot("16_result_sheared_nose.png");

                // 17: Result of Returning Nose
                server.execute(() -> {
                    if (testVillager != null) {
                        ActorState.tag(testVillager).putDouble("p:gcfsvg", 1d);
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                    for (Entity e : spawnedEntities) e.discard();
                    spawnedEntities.clear();
                });

                positionPlayer(mc, sp, overworld, baseX, baseY + 0.45, baseZ + 6.5, 180.0F, 2.0F);
                step++;
                delay = 20;
            }
            case 18 -> {
                saveScreenshot("17_result_restored_nose.png");

                // 18: Result of Shearing Wooly Sheep
                server.execute(() -> {
                    if (testVillager != null) testVillager.discard();
                    for (Entity e : spawnedEntities) e.discard();
                    spawnedEntities.clear();

                    testWooly = VillagerNews.WOOLY.get().create(overworld);
                    if (testWooly != null) {
                        testWooly.moveTo(baseX, baseY, baseZ + 4.0, 0.0F, 0.0F);
                        testWooly.setYRot(0.0F);
                        testWooly.yRotO = 0.0F;
                        testWooly.yHeadRot = 0.0F;
                        testWooly.yHeadRotO = 0.0F;
                        testWooly.yBodyRot = 0.0F;
                        testWooly.yBodyRotO = 0.0F;
                        testWooly.setSheared(true);
                        testWooly.setNoAi(true);
                        overworld.addFreshEntity(testWooly);
                    }

                    ItemEntity droppedWool = new ItemEntity(overworld, baseX + 0.25, baseY, baseZ + 4.8, new ItemStack(Items.WHITE_WOOL, 2));
                    droppedWool.setDeltaMovement(0, 0, 0);
                    overworld.addFreshEntity(droppedWool);
                    spawnedEntities.add(droppedWool);
                });

                positionPlayer(mc, sp, overworld, baseX, baseY + 0.35, baseZ + 6.5, 180.0F, 6.0F);
                step++;
                delay = 20;
            }
            case 19 -> {
                saveScreenshot("18_result_wooly_sheared.png");

                // Respawn test villager for cosmetics
                server.execute(() -> {
                    if (testWooly != null) testWooly.discard();
                    for (Entity e : spawnedEntities) e.discard();
                    spawnedEntities.clear();

                    testVillager = VillagerNews.VILLAGERS.get("villager").get().create(overworld);
                    if (testVillager != null) {
                        testVillager.moveTo(baseX, baseY, baseZ + 4.0, 0.0F, 0.0F);
                        testVillager.setYRot(0.0F);
                        testVillager.yRotO = 0.0F;
                        testVillager.yHeadRot = 0.0F;
                        testVillager.yHeadRotO = 0.0F;
                        testVillager.yBodyRot = 0.0F;
                        testVillager.yBodyRotO = 0.0F;
                        testVillager.setNoAi(true);
                        overworld.addFreshEntity(testVillager);
                        ActorState.ensure(testVillager);

                        // 19: Villager wearing Mayor Hat (cryhjc)
                        ActorState.tag(testVillager).putString("p:mlxeez", "cryhjc");
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                });

                positionPlayer(mc, sp, overworld, baseX, baseY + 0.45, baseZ + 6.5, 180.0F, 2.0F);
                step++;
                delay = 20;
            }
            case 20 -> {
                saveScreenshot("19_villager_wearing_mayor_hat.png");

                // 20: Villager wearing Moustache (odplew)
                server.execute(() -> {
                    if (testVillager != null) {
                        ActorState.tag(testVillager).putString("p:mlxeez", "odplew");
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                });

                step++;
                delay = 20;
            }
            case 21 -> {
                saveScreenshot("20_villager_wearing_moustache.png");

                // 21: Villager wearing Helmet (ufernq)
                server.execute(() -> {
                    if (testVillager != null) {
                        ActorState.tag(testVillager).putString("p:mlxeez", "ufernq");
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                });

                step++;
                delay = 20;
            }
            case 22 -> {
                saveScreenshot("21_villager_wearing_helmet.png");

                // 22: Villager holding Microphone (dsojot)
                server.execute(() -> {
                    if (testVillager != null) {
                        ActorState.tag(testVillager).putString("p:mlxeez", "dsojot");
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                });

                step++;
                delay = 20;
            }
            case 23 -> {
                saveScreenshot("22_villager_holding_microphone.png");

                // 23: Villager holding Protest Sign
                server.execute(() -> {
                    if (testVillager != null) {
                        ActorState.tag(testVillager).putString("p:mlxeez", "none");
                        ActorState.tag(testVillager).putDouble("p:sign", 0d);
                        ActorState.tag(testVillager).putDouble("p:wjnyei", 1d);
                        NewsNetwork.broadcast(testVillager, 1, "", overworld.getGameTime());
                    }
                });

                step++;
                delay = 20;
            }
            case 24 -> {
                saveScreenshot("23_villager_holding_sign.png");
                System.out.println("[VillagerNews E2E] =========================================");
                System.out.println("[VillagerNews E2E] ALL 23 SCREENSHOTS SUCCESSFULLY CAPTURED!");
                System.out.println("[VillagerNews E2E] =========================================");
                active = false;
                step++;
                mc.stop();
            }
        }
    }

    private static void focusLineupEntity(Minecraft mc, ServerPlayer sp, ServerLevel overworld, int index) {
        double targetX = baseX + LINEUP_OFFSETS[index];
        double targetZ = baseZ + 6.0;
        positionPlayer(mc, sp, overworld, targetX, baseY + 0.45, targetZ + 2.5, 180.0F, 2.0F);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.fov().set(45);
    }

    private static void positionPlayer(Minecraft mc, ServerPlayer sp, ServerLevel overworld, double x, double y, double z, float yaw, float pitch) {
        if (sp != null && overworld != null) {
            sp.teleportTo(overworld, x, y, z, yaw, pitch);
            sp.connection.teleport(x, y, z, yaw, pitch);
        }
        if (mc.player != null) {
            mc.player.moveTo(x, y, z, yaw, pitch);
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
            mc.player.yHeadRot = yaw;
            mc.player.yHeadRotO = yaw;
            mc.player.yBodyRot = yaw;
            mc.player.yBodyRotO = yaw;
            mc.player.setDeltaMovement(0, 0, 0);
        }
    }

    public static void saveScreenshot(String filename) {
        try {
            Minecraft mc = Minecraft.getInstance();
            NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            String customDir = System.getProperty("vn.test.dir");
            File dir = customDir != null ? new File(customDir) : new File(mc.gameDirectory, "screenshots");
            if (!dir.exists()) dir.mkdirs();
            File target = new File(dir, filename);
            image.writeToFile(target);
            image.close();
            System.out.println("[VillagerNews E2E] Captured: " + target.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
