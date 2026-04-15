package me.carmel.sprint.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class SprintClient implements ClientModInitializer {
    private static KeyBinding extraSprintKey;

    @Override
    public void onInitializeClient() {
        extraSprintKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("sprint v2", GLFW.GLFW_KEY_R, KeyBinding.Category.MOVEMENT)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }


            if (extraSprintKey.wasPressed()) {
                PlayerInventory inventory = client.player.getInventory();
                for (int i = 0; i < inventory.getHotbarSize(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        System.out.println("Slot " + i + ": " + stack.getName().getString() + " x" + stack.getCount());
                    }
                }

            }
//            while (extraSprintKey.wasPressed()) {
//                client.player.setSprinting(true);
//            }
        });
    }
}
