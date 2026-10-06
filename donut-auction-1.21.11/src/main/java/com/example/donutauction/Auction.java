package com.example.donutauction;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Auction {
    private static boolean running = false;
    private static int ticksLeft = 0;
    private static int finishedTicks = 0;
    private static String topBidder = null;
    private static double topAmount = 0;
    private static Pattern pattern;
    public static boolean preview = false;

    public static void reloadPattern() {
        try {
            pattern = Pattern.compile(AuctionConfig.get().paymentRegex);
        } catch (PatternSyntaxException e) {
            pattern = null;
            say("Bad paymentRegex in config: " + e.getDescription());
        }
    }

    public static boolean isRunning() { return running; }

    public static void start(int seconds) {
        running = true;
        ticksLeft = seconds * 20;
        finishedTicks = 0;
        topBidder = null;
        topAmount = 0;
        AuctionConfig c = AuctionConfig.get();
        say("Auction started for " + seconds + "s. Minimum bid: $" + format(c.minimumBid) + ".");
    }

    public static void stop() {
        if (!running) return;
        running = false;
        AuctionConfig c = AuctionConfig.get();
        finishedTicks = Math.max(1, c.resultSeconds) * 20;
        String result = fill(topBidder == null ? c.noWinnerMessage : c.winnerMessage);
        say(result);
        Minecraft mc = Minecraft.getInstance();
        if (c.announceWinnerInChat && mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat(result);
        }
    }

    public static void addTime(int seconds) {
        if (running) ticksLeft = Math.max(20, ticksLeft + seconds * 20);
    }

    public static void tick() {
        if (running) {
            if (--ticksLeft <= 0) stop();
        } else if (finishedTicks > 0) {
            finishedTicks--;
        }
    }

    /** Called for every incoming system/game chat message. */
    public static void onChat(String message) {
        if (!running || pattern == null) return;
        Matcher m = pattern.matcher(message);
        if (!m.find()) return;
        try {
            AuctionConfig c = AuctionConfig.get();
            String name = m.group(1);
            double amount = parseAmount(m.group(2), m.group(3));
            if (amount < c.minimumBid) return;          // below minimum: ignored completely
            if (amount > topAmount) {
                topAmount = amount;
                topBidder = name;
                int snipe = c.antiSnipeSeconds * 20;
                if (snipe > 0 && ticksLeft < snipe) ticksLeft = snipe;
            }
        } catch (RuntimeException ignored) {
        }
    }

    // ---------- HUD ----------

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        AuctionConfig c = AuctionConfig.get();
        if (mc.options.hideGui) return;
        if (!(running || finishedTicks > 0 || c.showWhenIdle || preview)) return;

        Font font = mc.font;
        int secs = (ticksLeft + 19) / 20;
        List<String> texts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int maxW = 0;
        for (AuctionConfig.Line l : c.lines) {
            String t = fill(l.text);
            int col = AuctionConfig.parseColor(l.color, 0xFFFFFFFF);
            if (l.text.contains("{timer}")) {
                if (!running && finishedTicks > 0) col = AuctionConfig.parseColor(c.timerEndedColor, col);
                else if (running && secs <= c.warningSeconds) col = AuctionConfig.parseColor(c.timerWarningColor, col);
            }
            texts.add(t);
            colors.add(col);
            maxW = Math.max(maxW, font.width(t));
        }
        if (texts.isEmpty()) return;

        int lineH = font.lineHeight + c.lineSpacing;
        int w = maxW + c.padding * 2;
        int h = texts.size() * lineH - c.lineSpacing + c.padding * 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) c.hudX, (float) c.hudY);
        graphics.pose().scale(c.scale, c.scale);
        if (c.showBackground) {
            graphics.fill(0, 0, w, h, AuctionConfig.parseColor(c.backgroundColor, 0x99000000));
        }
        for (int i = 0; i < texts.size(); i++) {
            graphics.drawString(font, texts.get(i), c.padding, c.padding + i * lineH, colors.get(i), c.textShadow);
        }
        graphics.pose().popMatrix();
    }

    /** Replaces {placeholders} in a template. */
    static String fill(String template) {
        AuctionConfig c = AuctionConfig.get();
        String seller = c.sellerName == null || c.sellerName.isBlank()
                ? Minecraft.getInstance().getUser().getName() : c.sellerName;
        String item = c.itemName == null || c.itemName.isBlank() ? "Not set" : c.itemName;
        String timer;
        if (running) timer = timerText((ticksLeft + 19) / 20);
        else if (finishedTicks > 0) timer = "ENDED";
        else timer = timerText(c.durationSeconds);
        return template
                .replace("{seller}", seller)
                .replace("{item}", item)
                .replace("{bidder}", topBidder == null ? c.noBidText : topBidder)
                .replace("{bid}", topBidder == null ? "0" : format(topAmount))
                .replace("{min}", format(c.minimumBid))
                .replace("{timer}", timer);
    }

    static String timerText(int secs) {
        return secs / 60 + ":" + String.format("%02d", secs % 60);
    }

    // ---------- number helpers ----------

    static double parseAmount(String number, String suffix) {
        double value = Double.parseDouble(number.replace(",", ""));
        switch (suffix == null ? "" : suffix.toLowerCase()) {
            case "k": return value * 1_000;
            case "m": return value * 1_000_000;
            case "b": return value * 1_000_000_000;
            default: return value;
        }
    }

    /** Parses things like "500", "1,500", "1.5k", "2m". Returns NaN if unreadable. */
    public static double parseLoose(String s) {
        Matcher m = Pattern.compile("^\\s*\\$?([\\d,]*\\.?\\d+)\\s*([kKmMbB]?)\\s*$").matcher(s);
        if (!m.matches()) return Double.NaN;
        try {
            return parseAmount(m.group(1), m.group(2));
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    public static String format(double v) {
        if (AuctionConfig.get().compactNumbers) {
            if (v >= 1_000_000_000) return trim(v / 1_000_000_000) + "B";
            if (v >= 1_000_000) return trim(v / 1_000_000) + "M";
            if (v >= 1_000) return trim(v / 1_000) + "K";
        }
        return trim(v);
    }

    private static String trim(double v) {
        String s = String.format("%,.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void say(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Auction] " + msg));
    }
}
