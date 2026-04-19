package me.carmel.sprint.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SprintClient implements ClientModInitializer {
    private static KeyBinding SpearLunge;
    private int lastSelectedSlot = -1;
    private boolean isattacking = false;
    private static KeyBinding MaceAttack;

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
        if (client.player == null) {
            return;
        }

        if (restoreSlotIfNeeded(client) || shouldSkipAttack(client)) {
            return;
        }

        if (SpearLunge.wasPressed()) {
            PlayerInventory inventory = client.player.getInventory();
            Map<Map<String, Object>, Integer> eligibleSpears = new HashMap<>();
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (isEligibleSpear(stack) == null) {
                    continue;
                }
                eligibleSpears.put(isEligibleSpear(stack), i);
            }
            int i = eligibleSpears. // max by lunge level
                    entrySet()
                    .stream()
                    .max(Comparator.comparingInt(e -> (int) e.getKey().get("level")))
                    .map(Map.Entry::getValue)
                    .orElse(-1);
            if (!isattacking) {
                lastSelectedSlot = inventory.getSelectedSlot();
            }
            isattacking = true;
            System.out.println("moved from" + inventory.getSelectedSlot());

            inventory.setSelectedSlot(i);
            triggerMainHandAttack(client);

        } else if (MaceAttack.wasPressed()) {
            PlayerInventory inventory = client.player.getInventory();
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!isEligibleMace(stack)) {
                    continue;
                }

                if (!isattacking) {
                    lastSelectedSlot = inventory.getSelectedSlot();
                }
                isattacking = true;
                System.out.println("moved from" + inventory.getSelectedSlot());

                inventory.setSelectedSlot(i);
                triggerMainHandAttack(client);
            }

        }

    }

    private boolean restoreSlotIfNeeded(MinecraftClient client) {
        assert client.player != null;
        if (!isattacking) {
            return false;
        }

        System.out.println("returning to " + lastSelectedSlot);
        isattacking = false;
        if (lastSelectedSlot != -1) {
            client.player.getInventory().setSelectedSlot(lastSelectedSlot);
        }
        return true;
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

        client.player.swingHand(Hand.MAIN_HAND);
        KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), true);
        KeyBinding.onKeyPressed(client.options.attackKey.getDefaultKey());
        client.player.swingHand(Hand.MAIN_HAND);
        KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), false);
    }
}
