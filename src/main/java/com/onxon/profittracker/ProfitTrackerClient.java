package com.onxon.profittracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfitTrackerClient implements ClientModInitializer {
    public static final String MOD_ID = "donut_profit_tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        Settings.load();
        Ledger.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category." + MOD_ID));

        // P opens the menu from anywhere in the game
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ProfitMenuScreen(null));
                }
            }
        });

        // Every chat / system line goes through the parser; matches become ledger entries
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            PaymentParser.parse(message.getString()).ifPresent(Ledger.get()::record);
        });

        // The in-game overlay, drawn just after the chat layer
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "tracker_hud"),
                (context, tickCounter) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (!client.options.hudHidden) {
                        HudWidgets.renderAll(context);
                    }
                });

        LOGGER.info("Donut Profit Tracker loaded. Press P to open the menu.");
    }
}
