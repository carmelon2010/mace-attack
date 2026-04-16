package me.carmel.sprint.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class SprintClient implements ClientModInitializer {
    private static KeyBinding extraSprintKey;
    private int lastSelectedSlot = -1;
    private boolean isattacking = false;

    @Override
    public void onInitializeClient() {
        extraSprintKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("sprint v2", GLFW.GLFW_KEY_R, KeyBinding.Category.MOVEMENT)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            if (isattacking){
                if (!extraSprintKey.isPressed()) {
                    System.out.println("returning to " + lastSelectedSlot);
                    isattacking = false;
                    if (lastSelectedSlot != -1) {
                        client.player.getInventory().setSelectedSlot(lastSelectedSlot);
                    }
                }
                return;
            }

            if (extraSprintKey.wasPressed()) {
                PlayerInventory inventory = client.player.getInventory();

                for (int i = 0; i < inventory.getHotbarSize(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
//                    System.out.println("Slot " + i + ": " + stack.getName().getString() + " x" + stack.getCount());
                    if (!stack.getItemName().getString().toLowerCase().contains("spear")) {
                        continue;
                    }

                    isattacking = true;
                    lastSelectedSlot = inventory.getSelectedSlot();
                    System.out.println("moved from" + lastSelectedSlot);

                    inventory.setSelectedSlot(i);

                    // put jab attack here
                    if (client.interactionManager != null) {
                        client.player.swingHand(Hand.MAIN_HAND);

                        if (client.crosshairTarget != null && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
                            client.interactionManager.attackEntity(client.player, ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity());
                        } else {
                            client.interactionManager.attackBlock(client.player.getBlockPos(), Direction.EAST);
                        }
                    }


                }

            }
        });
    }
}
