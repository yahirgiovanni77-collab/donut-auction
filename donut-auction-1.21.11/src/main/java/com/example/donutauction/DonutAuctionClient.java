package com.example.donutauction;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DonutAuctionClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("donutauction", "main"));
    private static KeyMapping toggleKey;
    private static KeyMapping menuKey;
    private static int openMenuDelay = 0;

    @Override
    public void onInitializeClient() {
        AuctionConfig.load();
        Auction.reloadPattern();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donutauction.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donutauction.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                if (Auction.isRunning()) Auction.stop();
                else Auction.start(AuctionConfig.get().durationSeconds);
            }
            while (menuKey.consumeClick()) {
                client.setScreen(new AuctionMenuScreen());
            }
            // commands run while chat is open, so open the menu a couple ticks later
            if (openMenuDelay > 0 && --openMenuDelay == 0) {
                client.setScreen(new AuctionMenuScreen());
            }
            Auction.tick();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) Auction.onChat(message.getString());
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("donutauction", "auction_hud"), Auction::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("auctionhud");

            root.then(literal("start")
                    .executes(c -> {
                        Auction.start(AuctionConfig.get().durationSeconds);
                        return 1;
                    })
                    .then(argument("seconds", integer(5, 86400)).executes(c -> {
                        Auction.start(getInteger(c, "seconds"));
                        return 1;
                    })));
            root.then(literal("stop").executes(c -> {
                Auction.stop();
                return 1;
            }));
            root.then(literal("addtime").then(argument("seconds", integer(-3600, 3600)).executes(c -> {
                Auction.addTime(getInteger(c, "seconds"));
                return 1;
            })));
            root.then(literal("menu").executes(c -> {
                openMenuDelay = 2;
                return 1;
            }));
            root.then(literal("preview").executes(c -> {
                Auction.preview = !Auction.preview;
                reply(c.getSource(), "HUD preview " + (Auction.preview ? "on" : "off"));
                return 1;
            }));
            root.then(literal("reload").executes(c -> {
                AuctionConfig.load();
                Auction.reloadPattern();
                reply(c.getSource(), "Config reloaded.");
                return 1;
            }));

            // quick setters
            root.then(setCmd("seller", "sellerName"));
            root.then(setCmd("item", "itemName"));
            root.then(setCmd("min", "minimumBid"));
            root.then(setCmd("time", "durationSeconds"));

            // set ANY simple option: /auctionhud set <option> <value>
            root.then(literal("set").then(argument("option", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(AuctionConfig.optionNames(), b))
                    .then(argument("value", greedyString()).executes(c -> {
                        reply(c.getSource(), AuctionConfig.set(getString(c, "option"), getString(c, "value")));
                        return 1;
                    }))));

            // HUD line editing
            root.then(literal("line").then(argument("n", integer(1, 50))
                    .then(argument("text", greedyString()).executes(c -> {
                        int n = getInteger(c, "n");
                        List<AuctionConfig.Line> lines = AuctionConfig.get().lines;
                        if (n <= lines.size()) lines.get(n - 1).text = getString(c, "text");
                        else if (n == lines.size() + 1) lines.add(new AuctionConfig.Line(getString(c, "text"), "#FFFFFF"));
                        else {
                            reply(c.getSource(), "Line numbers go up to " + (lines.size() + 1));
                            return 0;
                        }
                        AuctionConfig.save();
                        reply(c.getSource(), "Line " + n + " updated.");
                        return 1;
                    }))));
            root.then(literal("linecolor").then(argument("n", integer(1, 50))
                    .then(argument("color", StringArgumentType.word()).executes(c -> {
                        int n = getInteger(c, "n");
                        List<AuctionConfig.Line> lines = AuctionConfig.get().lines;
                        if (n > lines.size()) {
                            reply(c.getSource(), "No line " + n);
                            return 0;
                        }
                        lines.get(n - 1).color = getString(c, "color");
                        AuctionConfig.save();
                        reply(c.getSource(), "Line " + n + " color set.");
                        return 1;
                    }))));
            root.then(literal("removeline").then(argument("n", integer(1, 50)).executes(c -> {
                int n = getInteger(c, "n");
                List<AuctionConfig.Line> lines = AuctionConfig.get().lines;
                if (n > lines.size()) {
                    reply(c.getSource(), "No line " + n);
                    return 0;
                }
                lines.remove(n - 1);
                AuctionConfig.save();
                reply(c.getSource(), "Line " + n + " removed.");
                return 1;
            })));
            root.then(literal("resetlines").executes(c -> {
                AuctionConfig.get().lines = AuctionConfig.defaultLines();
                AuctionConfig.save();
                reply(c.getSource(), "HUD lines reset to default.");
                return 1;
            }));

            dispatcher.register(root);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> setCmd(String name, String option) {
        return literal(name).then(argument("value", greedyString()).executes(c -> {
            reply(c.getSource(), AuctionConfig.set(option, getString(c, "value")));
            return 1;
        }));
    }

    private static void reply(FabricClientCommandSource source, String msg) {
        source.sendFeedback(Component.literal("[Auction] " + msg));
    }
}
