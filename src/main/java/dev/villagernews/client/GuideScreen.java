package dev.villagernews.client;

import dev.villagernews.data.OriginalData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Manual text recovered from the addon. This is not the original Bedrock form. */
public final class GuideScreen extends Screen {
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int scroll;
    private int contentHeight = 1;

    public GuideScreen() {
        super(Component.literal("Villager News"));
    }

    @Override
    protected void init() {
        lines.clear();
        boolean isPt = net.minecraft.client.Minecraft.getInstance().options.languageCode.toLowerCase().startsWith("pt");
        com.google.gson.JsonObject guide = (isPt && OriginalData.GUIDE_PT != null) ? OriginalData.GUIDE_PT : OriginalData.GUIDE;
        for (var section : guide.entrySet()) {
            if (!section.getValue().isJsonArray()) continue;
            for (var entry : section.getValue().getAsJsonArray()) {
                if (!entry.isJsonObject()) continue;
                var page = entry.getAsJsonObject();
                if (page.has("header")) lines.addAll(font.split(Component.literal(page.get("header").getAsString()), width - 40));
                if (page.has("body")) lines.addAll(font.split(Component.literal(page.get("body").getAsString()), width - 40));
                lines.add(FormattedCharSequence.EMPTY);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int y = 32 - scroll;
        int bottom = height - 16;
        for (FormattedCharSequence line : lines) {
            if (y > 28 && y < bottom) graphics.drawString(font, line, 20, y, 0xFFFFFF);
            y += font.lineHeight + 2;
        }
        contentHeight = Math.max(1, y + scroll - 32);
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, contentHeight - (height - 48));
        scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 16, 0, max);
        return true;
    }
}
