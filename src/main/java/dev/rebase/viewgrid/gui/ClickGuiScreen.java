package dev.rebase.viewgrid.gui;

import dev.rebase.viewgrid.config.ColorRgba;
import dev.rebase.viewgrid.config.ConfigManager;
import dev.rebase.viewgrid.module.Category;
import dev.rebase.viewgrid.module.Module;
import dev.rebase.viewgrid.module.ModuleManager;
import dev.rebase.viewgrid.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ClickGuiScreen extends Screen {
    private static final int BACKDROP = 0x99070A10;
    private static final int SIDEBAR_BG = 0xE60C121D;
    private static final int PANEL_BG = 0xCC111826;
    private static final int CARD_BG = 0xE6172133;
    private static final int CARD_ACTIVE_BG = 0xE61C2A40;
    private static final int BORDER_COL = 0x44526B94;
    private static final int ACCENT_COL = 0xFF38BDF8;
    private static final int TEXT_HI = 0xFFF1F5F9;
    private static final int TEXT_LO = 0xFF94A3B8;

    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    private Category currentCategory;
    private Module openDrawerModule = null;
    private Module bindingModule = null;
    private EditBox searchField;

    private int railX, railY, railW, railH;
    private int bodyX, bodyY, bodyW, bodyH;
    private int dockX, dockY, dockW, dockH;

    public ClickGuiScreen(ModuleManager moduleManager, ConfigManager configManager) {
        super(Component.literal("ViewGrid"));
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        this.currentCategory = resolveCategory(configManager.getSelectedCategory());
    }

    private Category resolveCategory(String raw) {
        if (raw == null) return Category.VISUALS;
        for (Category c : Category.values()) {
            if (c.name().equalsIgnoreCase(raw)) return c;
        }
        return Category.VISUALS;
    }

    @Override
    protected void init() {
        int pad = 18;
        int dockHeight = 36;
        int usableH = Math.max(220, this.height - pad * 2 - dockHeight - 10);
        int usableW = Math.max(460, this.width - pad * 2);

        this.railW = Math.min(188, Math.max(145, usableW / 4));
        this.railX = pad;
        this.railY = pad;
        this.railH = usableH;

        this.bodyX = this.railX + this.railW + 12;
        this.bodyY = pad;
        this.bodyW = Math.max(220, usableW - this.railW - 12);
        this.bodyH = usableH;

        this.dockW = Math.min(540, usableW);
        this.dockH = dockHeight;
        this.dockX = (this.width - this.dockW) / 2;
        this.dockY = this.bodyY + this.bodyH + 10;

        int searchWidth = Math.max(150, this.dockW - 118);
        this.searchField = new EditBox(this.font, this.dockX + 10, this.dockY + 8, searchWidth, 20, Component.literal("Search modules..."));
        this.searchField.setHint(Component.literal("Search modules..."));
        this.searchField.setMaxLength(64);
        addRenderableWidget(this.searchField);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, BACKDROP);
        drawRail(g, mouseX, mouseY);
        drawModuleGrid(g, mouseX, mouseY);
        drawDock(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
        if (this.openDrawerModule != null) {
            drawSettingsDrawer(g, mouseX, mouseY, this.openDrawerModule);
        }
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        GuiDrawHelper.drawBox(g, railX, railY, railW, railH, SIDEBAR_BG, BORDER_COL);

        int bx = railX + 12;
        int by = railY + 12;
        g.fill(bx, by, bx + 18, by + 18, ACCENT_COL);
        g.fill(bx + 4, by + 4, bx + 14, by + 14, SIDEBAR_BG);
        g.fill(bx + 7, by + 7, bx + 11, by + 11, ACCENT_COL);

        g.drawString(this.font, "VIEWGRID", bx + 24, by + 2, TEXT_HI, false);
        g.drawString(this.font, "1.21.11 Fabric", bx + 24, by + 12, TEXT_LO, false);

        int y = railY + 42;
        int rowH = 26;
        for (Category cat : Category.values()) {
            boolean active = (cat == this.currentCategory);
            boolean hover = GuiDrawHelper.contains(mouseX, mouseY, railX + 8, y, railW - 16, rowH);
            int bg = active ? 0x3338BDF8 : (hover ? 0x1AFFFFFF : 0x00000000);
            g.fill(railX + 8, y, railX + railW - 8, y + rowH, bg);
            if (active) {
                g.fill(railX + 8, y + 4, railX + 11, y + rowH - 4, ACCENT_COL);
            }
            int cnt = moduleManager.filter(cat, "").size();
            g.drawString(this.font, cat.getTitle(), railX + 18, y + 9, active ? TEXT_HI : TEXT_LO, false);
            String num = String.valueOf(cnt);
            g.drawString(this.font, num, railX + railW - 16 - this.font.width(num), y + 9, TEXT_LO, false);
            y += rowH + 4;
        }

        int fx = railX + 8;
        int fy = railY + railH - 52;
        int fw = railW - 16;
        GuiDrawHelper.drawBox(g, fx, fy, fw, 44, 0xCC0A0F18, BORDER_COL);
        String user = Minecraft.getInstance().getUser() != null ? Minecraft.getInstance().getUser().getName() : "Offline";
        g.drawString(this.font, "User: " + user, fx + 8, fy + 8, TEXT_HI, false);
        g.drawString(this.font, "Enabled: " + moduleManager.countActive() + "/" + moduleManager.getAll().size(), fx + 8, fy + 22, ACCENT_COL, false);
    }

    private void drawModuleGrid(GuiGraphics g, int mouseX, int mouseY) {
        GuiDrawHelper.drawBox(g, bodyX, bodyY, bodyW, bodyH, PANEL_BG, BORDER_COL);

        String q = this.searchField != null ? this.searchField.getValue() : "";
        List<Module> items = moduleManager.filter(q.isBlank() ? this.currentCategory : null, q);

        long enabledCount = items.stream().filter(Module::isEnabled).count();
        String heading = q.isBlank() ? this.currentCategory.getTitle() : "Search";
        g.drawString(this.font, heading, bodyX + 14, bodyY + 12, TEXT_HI, false);
        String rightMeta = items.size() + " total  |  " + enabledCount + " on";
        g.drawString(this.font, rightMeta, bodyX + bodyW - 14 - this.font.width(rightMeta), bodyY + 12, TEXT_LO, false);

        int top = bodyY + 30;
        int left = bodyX + 12;
        int availW = bodyW - 24;
        int cols = 2;
        int gap = 10;
        int cardW = Math.max(100, (availW - gap) / cols);
        int cardH = Boolean.TRUE.equals(configManager.isCompactLayout()) ? 44 : 54;

        for (int i = 0; i < items.size(); i++) {
            Module mod = items.get(i);
            int c = i % cols;
            int r = i / cols;
            int cx = left + c * (cardW + gap);
            int cy = top + r * (cardH + gap);
            if (cy + cardH > bodyY + bodyH - 10) break;

            int fill = mod.isEnabled() ? CARD_ACTIVE_BG : CARD_BG;
            int border = mod.isEnabled() ? ACCENT_COL : BORDER_COL;
            GuiDrawHelper.drawBox(g, cx, cy, cardW, cardH, fill, border);

            g.drawString(this.font, mod.getTitle(), cx + 10, cy + 8, TEXT_HI, false);
            if (!Boolean.TRUE.equals(configManager.isCompactLayout())) {
                String desc = this.font.plainSubstrByWidth(mod.getDescription(), Math.max(40, cardW - 68));
                g.drawString(this.font, desc, cx + 10, cy + 22, TEXT_LO, false);
            }

            int gearX = cx + cardW - 48;
            int gearY = cy + (cardH - 16) / 2;
            GuiDrawHelper.drawBox(g, gearX, gearY, 16, 16, 0x8824324A, BORDER_COL);
            g.drawString(this.font, "*", gearX + 5, gearY + 4, TEXT_HI, false);

            int swX = cx + cardW - 28;
            int swY = cy + (cardH - 14) / 2;
            GuiDrawHelper.drawCapsule(g, swX, swY, 20, 14, mod.isEnabled() ? 0xFF0EA5E9 : 0xFF334155, BORDER_COL);
            g.fill(mod.isEnabled() ? swX + 8 : swX + 2, swY + 2, (mod.isEnabled() ? swX + 8 : swX + 2) + 10, swY + 12, TEXT_HI);
        }
    }

    private void drawSettingsDrawer(GuiGraphics g, int mouseX, int mouseY, Module mod) {
        int dw = Math.min(260, this.width - 40);
        int dh = Math.min(260, this.height - 80);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;

        GuiDrawHelper.drawBox(g, dx, dy, dw, dh, 0xF50A0F1A, ACCENT_COL);
        g.drawString(this.font, mod.getTitle(), dx + 12, dy + 10, TEXT_HI, false);
        g.drawString(this.font, "[X]", dx + dw - 24, dy + 10, 0xFFFF6B6B, false);

        int sy = dy + 28;
        String bindText = (bindingModule == mod)
            ? "Press key..."
            : "Keybind: " + (mod.getKeybind() == GLFW.GLFW_KEY_UNKNOWN ? "None" : GLFW.glfwGetKeyName(mod.getKeybind(), 0));
        GuiDrawHelper.drawBox(g, dx + 12, sy, dw - 24, 18, 0x991E293B, BORDER_COL);
        g.drawString(this.font, bindText, dx + 18, sy + 5, ACCENT_COL, false);
        sy += 22;

        for (Setting<?> s : mod.getSettings()) {
            if (sy + 20 > dy + dh - 10) break;
            switch (s.getKind()) {
                case TOGGLE -> {
                    @SuppressWarnings("unchecked")
                    Setting<Boolean> ts = (Setting<Boolean>) s;
                    g.drawString(this.font, ts.getLabel() + ": " + (Boolean.TRUE.equals(ts.get()) ? "ON" : "OFF"), dx + 12, sy + 5, TEXT_HI, false);
                }
                case SLIDER_DOUBLE -> {
                    @SuppressWarnings("unchecked")
                    Setting<Double> ds = (Setting<Double>) s;
                    double p = (ds.get() - ds.getMinD()) / Math.max(1e-6, ds.getMaxD() - ds.getMinD());
                    int bw = dw - 24;
                    g.fill(dx + 12, sy + 12, dx + 12 + bw, sy + 16, 0xFF1E293B);
                    g.fill(dx + 12, sy + 12, dx + 12 + (int) (bw * p), sy + 16, ACCENT_COL);
                    g.drawString(this.font, String.format("%s: %.2f", ds.getLabel(), ds.get()), dx + 12, sy + 2, TEXT_HI, false);
                }
                case SLIDER_INT -> {
                    @SuppressWarnings("unchecked")
                    Setting<Integer> is = (Setting<Integer>) s;
                    double p = (double) (is.get() - is.getMinI()) / Math.max(1, is.getMaxI() - is.getMinI());
                    int bw = dw - 24;
                    g.fill(dx + 12, sy + 12, dx + 12 + bw, sy + 16, 0xFF1E293B);
                    g.fill(dx + 12, sy + 12, dx + 12 + (int) (bw * p), sy + 16, ACCENT_COL);
                    g.drawString(this.font, is.getLabel() + ": " + is.get(), dx + 12, sy + 2, TEXT_HI, false);
                }
                case DROPDOWN -> {
                    @SuppressWarnings("unchecked")
                    Setting<Enum<?>> es = (Setting<Enum<?>>) s;
                    g.drawString(this.font, es.getLabel() + ": " + es.get().name(), dx + 12, sy + 5, TEXT_HI, false);
                }
                case COLOR_PICKER -> {
                    @SuppressWarnings("unchecked")
                    Setting<ColorRgba> cs = (Setting<ColorRgba>) s;
                    g.drawString(this.font, cs.getLabel(), dx + 12, sy + 5, TEXT_HI, false);
                    GuiDrawHelper.drawSwatch(g, dx + dw - 30, sy + 2, 14, cs.get());
                }
                case BLOCK_FILTER -> g.drawString(this.font, s.getLabel() + " (in config JSON)", dx + 12, sy + 5, TEXT_LO, false);
            }
            sy += 22;
        }
    }

    private void drawDock(GuiGraphics g, int mouseX, int mouseY) {
        GuiDrawHelper.drawCapsule(g, dockX, dockY, dockW, dockH, SIDEBAR_BG, BORDER_COL);
        int bx = dockX + dockW - 96;
        int by = dockY + 8;
        GuiDrawHelper.drawBox(g, bx, by, 24, 20, configManager.isSoundFeedback() ? 0x4438BDF8 : 0x441E293B, BORDER_COL);
        g.drawString(this.font, "A", bx + 9, by + 6, TEXT_HI, false);

        GuiDrawHelper.drawBox(g, bx + 30, by, 24, 20, configManager.isCompactLayout() ? 0x4438BDF8 : 0x441E293B, BORDER_COL);
        g.drawString(this.font, "C", bx + 39, by + 6, TEXT_HI, false);

        GuiDrawHelper.drawBox(g, bx + 60, by, 26, 20, 0x4410B981, BORDER_COL);
        g.drawString(this.font, "SAVE", bx + 63, by + 6, TEXT_HI, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.openDrawerModule != null) {
            int dw = Math.min(260, this.width - 40);
            int dh = Math.min(260, this.height - 80);
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;

            if (GuiDrawHelper.contains(mouseX, mouseY, dx + dw - 28, dy + 6, 22, 16)
                || !GuiDrawHelper.contains(mouseX, mouseY, dx, dy, dw, dh)) {
                this.openDrawerModule = null;
                this.bindingModule = null;
                this.configManager.save();
                return true;
            }

            int sy = dy + 28;
            if (GuiDrawHelper.contains(mouseX, mouseY, dx + 12, sy, dw - 24, 18)) {
                this.bindingModule = this.openDrawerModule;
                return true;
            }
            sy += 22;

            for (Setting<?> s : this.openDrawerModule.getSettings()) {
                if (sy + 20 > dy + dh - 10) break;
                if (GuiDrawHelper.contains(mouseX, mouseY, dx + 12, sy, dw - 24, 20)) {
                    mutateSetting(s, mouseX, dx + 12, dw - 24, button);
                    this.configManager.save();
                    return true;
                }
                sy += 22;
            }
            return true;
        }

        int y = railY + 42;
        int rowH = 26;
        for (Category cat : Category.values()) {
            if (GuiDrawHelper.contains(mouseX, mouseY, railX + 8, y, railW - 16, rowH)) {
                this.currentCategory = cat;
                this.configManager.setSelectedCategory(cat.name());
                this.configManager.save();
                return true;
            }
            y += rowH + 4;
        }

        String q = this.searchField != null ? this.searchField.getValue() : "";
        List<Module> items = moduleManager.filter(q.isBlank() ? this.currentCategory : null, q);
        int top = bodyY + 30;
        int left = bodyX + 12;
        int availW = bodyW - 24;
        int cols = 2;
        int gap = 10;
        int cardW = Math.max(100, (availW - gap) / cols);
        int cardH = Boolean.TRUE.equals(configManager.isCompactLayout()) ? 44 : 54;

        for (int i = 0; i < items.size(); i++) {
            Module mod = items.get(i);
            int c = i % cols;
            int r = i / cols;
            int cx = left + c * (cardW + gap);
            int cy = top + r * (cardH + gap);
            if (cy + cardH > bodyY + bodyH - 10) break;

            int gearX = cx + cardW - 48;
            int gearY = cy + (cardH - 16) / 2;
            if (GuiDrawHelper.contains(mouseX, mouseY, cx, cy, cardW, cardH)) {
                if (GuiDrawHelper.contains(mouseX, mouseY, gearX, gearY, 16, 16) || button == 1) {
                    this.openDrawerModule = mod;
                    this.bindingModule = null;
                    return true;
                }
                if (button == 0) {
                    mod.toggle();
                    this.configManager.save();
                    return true;
                }
            }
        }

        int bx = dockX + dockW - 96;
        int by = dockY + 8;
        if (GuiDrawHelper.contains(mouseX, mouseY, bx, by, 24, 20)) {
            configManager.setSoundFeedback(!configManager.isSoundFeedback());
            configManager.save();
            return true;
        }
        if (GuiDrawHelper.contains(mouseX, mouseY, bx + 30, by, 24, 20)) {
            configManager.setCompactLayout(!configManager.isCompactLayout());
            configManager.save();
            return true;
        }
        if (GuiDrawHelper.contains(mouseX, mouseY, bx + 60, by, 26, 20)) {
            configManager.save();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @SuppressWarnings("unchecked")
    private void mutateSetting(Setting<?> s, double mouseX, int rx, int rw, int button) {
        switch (s.getKind()) {
            case TOGGLE -> {
                Setting<Boolean> ts = (Setting<Boolean>) s;
                ts.set(!Boolean.TRUE.equals(ts.get()));
            }
            case SLIDER_DOUBLE -> {
                Setting<Double> ds = (Setting<Double>) s;
                double frac = Math.max(0.0, Math.min(1.0, (mouseX - rx) / Math.max(1.0, rw)));
                ds.set(ds.getMinD() + frac * (ds.getMaxD() - ds.getMinD()));
            }
            case SLIDER_INT -> {
                Setting<Integer> is = (Setting<Integer>) s;
                double frac = Math.max(0.0, Math.min(1.0, (mouseX - rx) / Math.max(1.0, rw)));
                is.set((int) Math.round(is.getMinI() + frac * (is.getMaxI() - is.getMinI())));
            }
            case DROPDOWN -> {
                Setting<Enum<?>> es = (Setting<Enum<?>>) s;
                Enum<?>[] vals = es.getChoices();
                if (vals != null && vals.length > 0) {
                    int idx = es.get().ordinal();
                    es.set(vals[(button == 1 ? idx - 1 + vals.length : idx + 1) % vals.length]);
                }
            }
            case COLOR_PICKER -> {
                Setting<ColorRgba> cs = (Setting<ColorRgba>) s;
                ColorRgba cur = cs.get();
                cs.set(new ColorRgba((cur.getR() + 64) % 256, (cur.getG() + 48) % 256, (cur.getB() + 96) % 256, cur.getA()));
            }
            case BLOCK_FILTER -> {
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                this.bindingModule.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                this.bindingModule.setKeybind(keyCode);
            }
            this.bindingModule = null;
            this.configManager.save();
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_F && this.searchField != null) {
            setFocused(this.searchField);
            this.searchField.setFocused(true);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.configManager.save();
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
