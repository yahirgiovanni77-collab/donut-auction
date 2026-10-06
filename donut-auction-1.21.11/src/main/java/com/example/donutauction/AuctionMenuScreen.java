package com.example.donutauction;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** In-game settings menu for the main auction options. Style options use /auctionhud set. */
public class AuctionMenuScreen extends Screen {
    private final List<EditBox> fields = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private EditBox seller, item, min, time, hx, hy, scale, snipe;

    public AuctionMenuScreen() {
        super(Component.literal("Auction Settings"));
    }

    @Override
    protected void init() {
        fields.clear();
        labels.clear();
        AuctionConfig c = AuctionConfig.get();
        int colW = 150, gap = 20;
        int left = this.width / 2 - colW - gap / 2;
        int right = this.width / 2 + gap / 2;
        int top = Math.max(36, this.height / 2 - 85);

        seller = field(left, top, colW, "Your username (blank = you)", c.sellerName);
        item = field(left, top + 40, colW, "Item being auctioned", c.itemName);
        min = field(left, top + 80, colW, "Minimum bid (e.g. 500k, 1.5m)", Auction.format(c.minimumBid));
        time = field(left, top + 120, colW, "Timer (seconds)", Integer.toString(c.durationSeconds));
        hx = field(right, top, colW, "HUD X position", Integer.toString(c.hudX));
        hy = field(right, top + 40, colW, "HUD Y position", Integer.toString(c.hudY));
        scale = field(right, top + 80, colW, "HUD size (1.0 = normal)", Float.toString(c.scale));
        snipe = field(right, top + 120, colW, "Anti-snipe seconds (0 = off)", Integer.toString(c.antiSnipeSeconds));

        int by = top + 165;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(this.width / 2 - 155, by, 100, 20).build());
        addRenderableWidget(Button.builder(
                        Component.literal(Auction.isRunning() ? "Stop Auction" : "Save & Start"), b -> {
                            save();
                            if (Auction.isRunning()) Auction.stop();
                            else Auction.start(AuctionConfig.get().durationSeconds);
                            this.onClose();
                        })
                .bounds(this.width / 2 - 50, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 + 55, by, 100, 20).build());
    }

    private EditBox field(int x, int y, int w, String label, String value) {
        EditBox f = new EditBox(this.font, x, y, w, 18, Component.literal(label));
        f.setMaxLength(100);
        f.setValue(value == null ? "" : value);
        addRenderableWidget(f);
        fields.add(f);
        labels.add(label);
        return f;
    }

    private void save() {
        AuctionConfig c = AuctionConfig.get();
        c.sellerName = seller.getValue().trim();
        c.itemName = item.getValue().trim();
        double m = Auction.parseLoose(min.getValue());
        if (!Double.isNaN(m)) c.minimumBid = m;
        c.durationSeconds = Math.max(5, parseInt(time.getValue(), c.durationSeconds));
        c.hudX = parseInt(hx.getValue(), c.hudX);
        c.hudY = parseInt(hy.getValue(), c.hudY);
        c.antiSnipeSeconds = Math.max(0, parseInt(snipe.getValue(), c.antiSnipeSeconds));
        try {
            c.scale = Math.max(0.3f, Math.min(5f, Float.parseFloat(scale.getValue().trim())));
        } catch (NumberFormatException ignored) {
        }
        AuctionConfig.save();
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, "Auction Settings", this.width / 2, 14, 0xFFFFFFFF);
        for (int i = 0; i < fields.size(); i++) {
            EditBox f = fields.get(i);
            graphics.drawString(this.font, labels.get(i), f.getX(), f.getY() - 11, 0xFFAAAAAA, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
