package me.carmel.hotkeys.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Unique
    private static final int HEAD_SIZE = 16;
    @Unique
    private static final int SLOT_COUNT = 9;

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void hotkeys$renderPlayerHeads(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getListedPlayerListEntries();
        if (entries.isEmpty()) {
            return;
        }

        int scaledWidth = context.getScaledWindowWidth();
        int scaledHeight = context.getScaledWindowHeight();

        // Match the default hotbar anchor so heads are aligned to the 9 slots.
        int hotbarLeftX = scaledWidth / 2 - 91;
        int headsY = scaledHeight - 42;

        int drawn = 0;
        for (PlayerListEntry entry : entries) {
            if (drawn >= SLOT_COUNT) {
                break;
            }

            int x = hotbarLeftX + 3 + (drawn * 20);
            drawPlayerHead(context, entry, x, headsY);
            drawn++;
        }
    }

    @Unique
    private void drawPlayerHead(DrawContext context, PlayerListEntry playerEntry, int x, int y) {
        PlayerSkinDrawer.draw(context, playerEntry.getSkinTextures(), x, y, HEAD_SIZE);
    }
}
