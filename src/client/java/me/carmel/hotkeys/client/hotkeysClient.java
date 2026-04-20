package me.carmel.hotkeys.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class hotkeysClient implements ClientModInitializer {
    private static KeyBinding SpearLunge;
    private static KeyBinding MaceAttack;
    private static final int ATTACK_DELAY_TICKS = 2;

    private long currentTick = 0;
    private boolean attackQueued = false;
    private final List<ScheduledAction> scheduledActions = new ArrayList<>();

    private static class ScheduledAction {
        private final long runAtTick;
        private final Runnable action;

        private ScheduledAction(long runAtTick, Runnable action) {
            this.runAtTick = runAtTick;
            this.action = action;
        }
    }

    @Override
    public void onInitializeClient() {
        SpearLunge = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("spear dash", GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MOVEMENT)
        );
        MaceAttack = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("Mace atttack", GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MOVEMENT)
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTick);
    }

    private void handleClientTick(MinecraftClient client) {
        currentTick++;
        runScheduledActions();

        if (client.player == null) {
            return;
        }

        if (attackQueued || shouldSkipAttack(client)) {
            return;
        }

        if (SpearLunge.isPressed()) {
            PlayerInventory inventory = client.player.getInventory();
            Map<Map<String, Object>, Integer> eligibleSpears = new HashMap<>();
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                ItemStack stack = inventory.getStack(i);
                Map<String, Object> spearData = isEligibleSpear(stack);
                if (spearData == null) {
                    continue;
                }
                eligibleSpears.put(spearData, i);
            }
            if (eligibleSpears.isEmpty()){
                return;
            }
            int i = eligibleSpears. // max by lunge level
                    entrySet()
                    .stream()
                    .max(Comparator.comparingInt(e -> (int) e.getKey().get("level")))
                    .map(Map.Entry::getValue)
                    .orElse(-1);

            queueAttack(client, inventory, i);

        } else if (MaceAttack.wasPressed()) {
            PlayerInventory inventory = client.player.getInventory();
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!isEligibleMace(stack)) {
                    continue;
                }

                queueAttack(client, inventory, i);
                break;
            }

        }

    }

    private void scheduleActionAfterTicks(int delayTicks, Runnable action) {
        long runAt = currentTick + Math.max(0, delayTicks);
        scheduledActions.add(new ScheduledAction(runAt, action));
    }

    private void runScheduledActions() {
        List<Runnable> dueActions = new ArrayList<>();
        Iterator<ScheduledAction> iterator = scheduledActions.iterator();
        while (iterator.hasNext()) {
            ScheduledAction scheduledAction = iterator.next();
            if (scheduledAction.runAtTick > currentTick) {
                continue;
            }

            dueActions.add(scheduledAction.action);
            iterator.remove();
        }

        for (Runnable action : dueActions) {
            action.run();
        }
    }

    private void queueAttack(MinecraftClient client, PlayerInventory inventory, int slot) {
        if (slot < 0 || attackQueued) {
            return;
        }

        int originalSlot = inventory.getSelectedSlot();
        attackQueued = true;
        inventory.setSelectedSlot(slot);
        scheduleActionAfterTicks(ATTACK_DELAY_TICKS, () -> executeDelayedAttack(client, originalSlot, 0));
    }

    private void executeDelayedAttack(MinecraftClient client, int originalSlot, int retries) {
        if (client.player == null) {
            attackQueued = false;
            return;
        }

        if (shouldSkipAttack(client) && retries < 20) {
            scheduleActionAfterTicks(1, () -> executeDelayedAttack(client, originalSlot, retries + 1));
            return;
        }

        triggerMainHandAttack(client);
        scheduleActionAfterTicks(1, () -> restoreSelectedSlot(client, originalSlot));
    }

    private void restoreSelectedSlot(MinecraftClient client, int previousSlot) {
        if (client.player != null && previousSlot != -1) {
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
        attackQueued = false;
    }

    private boolean shouldSkipAttack(MinecraftClient client) {
        return client.attackCooldown > 0;
    }

    private Map<String, Object> isEligibleSpear(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String itemName = stack.getItemName().getString().toLowerCase();
        String enchantments = stack.getEnchantments().toString();
        if (!itemName.contains("spear") || !enchantments.contains("minecraft:lunge")) {
            return null;
        }

        Matcher levelMatcher = Pattern.compile("minecraft:lunge[^0-9]*(\\d+)").matcher(enchantments);
        if (!levelMatcher.find()) {
            return null;
        }
        int lungeLevel = Integer.parseInt(levelMatcher.group(1));

        Map<String, Object> spearData = new HashMap<>();
        spearData.put("enchant", "lunge");
        spearData.put("level", lungeLevel);
        System.out.println("Found eligible spear: " + itemName + " with lunge level " + lungeLevel);
        return spearData;
    }

    private boolean isEligibleMace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getItemName().getString().toLowerCase().contains("mace");
    }

    private void triggerMainHandAttack(MinecraftClient client) {
        assert client.player != null;
        if (client.interactionManager == null) {
            return;
        }

        KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), true);
        KeyBinding.onKeyPressed(client.options.attackKey.getDefaultKey());
        KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), false);
    }
}
