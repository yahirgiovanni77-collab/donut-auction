package com.example.donutauction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Everything is customizable here. File: config/donutauction.json (or use the in-game menu / commands). */
public class AuctionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donutauction.json");
    private static AuctionConfig instance = new AuctionConfig();

    /** One line of the HUD. text supports: {seller} {item} {bidder} {bid} {min} {timer} */
    public static class Line {
        public String text = "";
        public String color = "#FFFFFF";
        public Line() {}
        public Line(String text, String color) { this.text = text; this.color = color; }
    }

    // ---- Auction settings ----
    public String sellerName = "";            // blank = your own Minecraft name
    public String itemName = "";              // what you're auctioning
    public double minimumBid = 0;             // payments below this are ignored and don't count
    public int durationSeconds = 60;          // default timer length
    public int antiSnipeSeconds = 0;          // late bid resets timer to this many seconds (0 = off)
    public int warningSeconds = 10;           // timer changes color at/below this
    public int resultSeconds = 10;            // how long the HUD stays after the auction ends
    public boolean announceWinnerInChat = false;
    public String winnerMessage = "{seller}'s auction for {item} was won by {bidder} with ${bid}!";
    public String noWinnerMessage = "{seller}'s auction for {item} ended with no bids.";
    public String noBidText = "No bids yet";
    public boolean compactNumbers = true;     // 1.5M instead of 1,500,000
    /** Group 1 = player name, group 2 = number, group 3 = K/M/B suffix. Edit to match DonutSMP's real message. */
    public String paymentRegex = "(\\w+) paid you \\$?([\\d,.]+)([kKmMbB]?)";

    // ---- HUD look ----
    public boolean showWhenIdle = false;      // keep HUD visible when no auction is running
    public int hudX = 6;
    public int hudY = 6;
    public float scale = 1.0f;
    public int padding = 4;
    public int lineSpacing = 2;
    public boolean showBackground = true;
    public String backgroundColor = "#99000000"; // #AARRGGBB or #RRGGBB
    public boolean textShadow = true;
    public String timerWarningColor = "#FF5555";
    public String timerEndedColor = "#55FF55";

    public List<Line> lines = defaultLines();

    public static List<Line> defaultLines() {
        List<Line> l = new ArrayList<>();
        l.add(new Line("AUCTION by {seller}", "#FFAA00"));
        l.add(new Line("Item: {item}", "#FFFFFF"));
        l.add(new Line("Highest bidder: {bidder}", "#55FFFF"));
        l.add(new Line("Current bid: ${bid}", "#55FF55"));
        l.add(new Line("Minimum bid: ${min}", "#AAAAAA"));
        l.add(new Line("Time left: {timer}", "#FFFF55"));
        return l;
    }

    public static AuctionConfig get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                AuctionConfig loaded = GSON.fromJson(Files.readString(FILE), AuctionConfig.class);
                if (loaded != null) instance = loaded;
            }
            if (instance.lines == null || instance.lines.isEmpty()) instance.lines = defaultLines();
            save();
        } catch (IOException | RuntimeException e) {
            System.err.println("[DonutAuction] Could not load config: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[DonutAuction] Could not save config: " + e.getMessage());
        }
    }

    public static int parseColor(String s, int fallback) {
        try {
            String h = s.trim();
            if (h.startsWith("#")) h = h.substring(1);
            long v = Long.parseLong(h, 16);
            if (h.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Names of simple options usable with /auctionhud set. */
    public static List<String> optionNames() {
        List<String> names = new ArrayList<>();
        for (Field f : AuctionConfig.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            Class<?> t = f.getType();
            if (t == int.class || t == float.class || t == double.class || t == boolean.class || t == String.class) {
                names.add(f.getName());
            }
        }
        return names;
    }

    /** Sets any simple option by name. Returns a message to show the player. */
    public static String set(String name, String value) {
        try {
            Field f = AuctionConfig.class.getField(name);
            Class<?> t = f.getType();
            if (t == int.class) f.setInt(instance, Integer.parseInt(value.trim()));
            else if (t == float.class) f.setFloat(instance, Float.parseFloat(value.trim()));
            else if (t == double.class) {
                double d = Auction.parseLoose(value);
                if (Double.isNaN(d)) return "Not a number: " + value;
                f.setDouble(instance, d);
            } else if (t == boolean.class) f.setBoolean(instance, Boolean.parseBoolean(value.trim()));
            else if (t == String.class) f.set(instance, value);
            else return "Use /auctionhud line ... to edit the HUD lines.";
            save();
            Auction.reloadPattern();
            return "Set " + name + " = " + value;
        } catch (NoSuchFieldException e) {
            return "Unknown option: " + name;
        } catch (NumberFormatException e) {
            return "Invalid value: " + value;
        } catch (IllegalAccessException e) {
            return "Cannot set " + name;
        }
    }
}
