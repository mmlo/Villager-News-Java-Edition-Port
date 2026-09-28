package dev.villagernews.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.villagernews.data.OriginalData;
import dev.villagernews.runtime.DialogueSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

public final class NewsCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("vn")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("speak")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.argument("groupId", StringArgumentType.word())
                        .executes(ctx -> {
                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                            String gid = StringArgumentType.getString(ctx, "groupId");
                            int count = 0;
                            for (Entity e : targets) {
                                if (e instanceof LivingEntity living) {
                                    if (DialogueSystem.speak(living, gid, true)) count++;
                                }
                            }
                            final int c = count;
                            ctx.getSource().sendSuccess(() -> Component.literal("Villager News: Disparado diálogo '" + gid + "' em " + c + " entidades."), true);
                            return count;
                        })
                    )
                )
            )
            .then(Commands.literal("random")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .executes(ctx -> {
                        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                        int count = 0;
                        for (Entity e : targets) {
                            if (e instanceof LivingEntity living) {
                                if (DialogueSystem.triggerRandom(living, DialogueSystem.GREETINGS)) count++;
                            }
                        }
                        final int c = count;
                        ctx.getSource().sendSuccess(() -> Component.literal("Villager News: Disparado diálogo aleatório em " + c + " entidades."), true);
                        return count;
                    })
                )
            )
            .then(Commands.literal("info")
                .executes(ctx -> {
                    int groups = OriginalData.DIALOGUES.size();
                    int sounds = OriginalData.SOUNDS.size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Villager News Port 1.0 (Forge 1.20.1): " + groups + " grupos de diálogo, " + sounds + " sons registrados."), false);
                    return 1;
                })
            )
        );
    }
}
