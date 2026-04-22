package me.carmel.hotkeys.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Unique
    private static final int BASE_HEAD_SIZE = 10;
    @Unique
    private static final int MIN_HEAD_SIZE = 4;
    @Unique
    private static final int MAX_HEAD_SIZE = 12;
    @Unique
    private static final int HOTBAR_WIDTH = 182;
    @Unique
    private static final double MAX_SCALE_DISTANCE = 64.0;
    @Unique
    private static final float LOCATOR_FOV_DEGREES = 90.0f;
    @Unique
    private final Map<UUID, double[]> hotkeys$cachedTargetPositions = new HashMap<>();

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void hotkeys$renderPlayerHeads(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
            return;
        }
        PlayerEntity localPlayer = client.player;

        Collection<PlayerListEntry> entries = client.getNetworkHandler().getListedPlayerListEntries();
        if (entries.isEmpty()) {
            return;
        }

        int scaledWidth = context.getScaledWindowWidth();
        int scaledHeight = context.getScaledWindowHeight();

        int hotbarLeftX = scaledWidth / 2 - HOTBAR_WIDTH / 2;
        int hotbarRightX = hotbarLeftX + HOTBAR_WIDTH;
        int headsY = scaledHeight - 30;
        UUID localPlayerId = localPlayer.getUuid();
        Set<UUID> listedPlayers = new HashSet<>();

        for (PlayerListEntry entry : entries) {
            UUID entryId = entry.getProfile().id();
            if (entryId == null || entryId.equals(localPlayerId)) {
                continue;
            }
            listedPlayers.add(entryId);

            PlayerEntity target = client.world.getPlayerByUuid(entryId);
            double targetX;
            double targetZ;
            int size;

            if (target != null) {
                targetX = target.getX();
                targetZ = target.getZ();
                hotkeys$cachedTargetPositions.put(entryId, new double[]{targetX, targetZ});
                size = getHeadSizeFromDistance(localPlayer, targetX, targetZ);
            } else {
                double[] cachedPos = hotkeys$cachedTargetPositions.get(entryId);
                if (cachedPos == null) {
                    continue;
                }

                targetX = cachedPos[0];
                targetZ = cachedPos[1];
                size = MIN_HEAD_SIZE;
            }

            float yawDiff = getYawDifference(localPlayer, targetX, targetZ);
            if (!isWithinFov(yawDiff)) {
                continue;
            }

            int centerX = getHeadCenterXFromDirection(yawDiff, scaledWidth);
            int x = MathHelper.clamp(centerX - (size / 2), hotbarLeftX, hotbarRightX - size);
            int y = headsY + (BASE_HEAD_SIZE - size) / 2;
            drawPlayerHead(context, entry, x, y, size);
        }

        hotkeys$cachedTargetPositions.keySet().retainAll(listedPlayers);
    }

    @Unique
    private int getHeadCenterXFromDirection(float yawDiff, int scaledWidth) {
        double halfFov = LOCATOR_FOV_DEGREES / 2.0;
        double normalized = MathHelper.clamp(yawDiff / halfFov, -1.0, 1.0);
        return scaledWidth / 2 + (int) Math.round(normalized * (HOTBAR_WIDTH / 2.0));
    }

    @Unique
    private boolean isWithinFov(float yawDiff) {
        return Math.abs(yawDiff) <= LOCATOR_FOV_DEGREES / 2.0f;
    }

    @Unique
    private float getYawDifference(PlayerEntity source, double targetX, double targetZ) {
        double dx = targetX - source.getX();
        double dz = targetZ - source.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return (float) MathHelper.wrapDegrees(targetYaw - source.getYaw());
    }

    @Unique
    private int getHeadSizeFromDistance(PlayerEntity source, double targetX, double targetZ) {
        double dx = targetX - source.getX();
        double dz = targetZ - source.getZ();
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance >= MAX_SCALE_DISTANCE) {
            return MIN_HEAD_SIZE;
        }

        double normalized = MathHelper.clamp(distance / MAX_SCALE_DISTANCE, 0.0, 1.0);
        double scaled = MAX_HEAD_SIZE - (normalized * (MAX_HEAD_SIZE - MIN_HEAD_SIZE));
        return MathHelper.clamp((int) Math.round(scaled), MIN_HEAD_SIZE, MAX_HEAD_SIZE);
    }

    @Unique
    private void drawPlayerHead(DrawContext context, PlayerListEntry playerEntry, int x, int y, int size) {
        PlayerSkinDrawer.draw(context, playerEntry.getSkinTextures(), x, y, size);
    }
}
