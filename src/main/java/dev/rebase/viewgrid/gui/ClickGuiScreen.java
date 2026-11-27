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
    private static final int COL_BACKDROP = 0x99080B12;
    private static final int COL_SIDEBAR = 0xE60E1420;
    private static final int COL_PANEL = 0xCC121927;
    private static final int COL_CARD = 0xE6182234;
    private static final int COL_CARD_ACTIVE = 0xE61D2B42;
    private static final int COL_BORDER = 0x44526B94;
    private static final int COL_ACCENT = 0xFF38BDF8;
    private static final int COL_TEXT_PRIMARY = 0xFFF1F5F9;
    private static final int COL_TEXT_MUTED = 0xFF94A3B8;

    private final ModuleManager moduleManager;
    private final ConfigManager configManager;

    private Category activeCategory;
    private Module expandedModule = null;
    private EditBox searchBox;
    private EditBox hexInputBox;
    private Setting<ColorRgba> activeColorSetting = null;
    private Module listeningKeybindModule = null;

    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int panelX, panelY, panelW, panelH;
    private int barX, barY, barW, barH;

    public ClickGuiScreen(ModuleManager moduleManager, ConfigManager configManager) {
        super(Component.literal("ViewGrid GUI"));
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        this.activeCategory = parseCategory(configManager.getLastCategory());
    }

    private Category parseCategory(String name) {
        if (name == null) return Category.VISUALS;
        for (Category c : Category.values()) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return Category.VISUALS;
    }

    @Override
    protected void init() {
        int margin = 18;
        int bottomBarHeight = 36;
        int totalH = Math.max(220, this.height - margin * 2 - bottomBarHeight - 10);
        int totalW = Math.max(460, this.width - margin * 2);

        this.sidebarW = Math.min(188, Math.max(145, totalW / 4));
        this.sidebarX = margin;
        this.sidebarY = margin;
        this.sidebarH = totalH;

        this.panelX = this.sidebarX + this.sidebarW + 12;
        this.panelY = margin;
        this.panelW = Math.max(220, totalW - this.sidebarW - 12);
        this.panelH = totalH;

        this.barW = Math.min(540, totalW);
        this.barH = bottomBarHeight;
        this.barX = (this.width - this.barW) / 2;
        this.barY = this.panelY + this.panelH + 10;

        int searchW = Math.max(150, this.barW - 118);
        this.searchBox = new EditBox(this.font, this.barX + 10, this.barY + 8, searchW, 20, Component.literal("Search modules..."));
        this.searchBox.setHint(Component.literal("Search modules..."));
        this.searchBox.setMaxLength(64);
        addRenderableWidget(this.searchBox);

        this.hexInputBox = new EditBox(this.font, 0, 0, 96, 16, Component.literal("#RRGGBBAA"));
        this.hexInputBox.setMaxLength(9);
        this.hexInputBox.setVisible(false);
        addRenderableWidget(this.hexInputBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, COL_BACKDROP);

        renderLeftSidebar(graphics, mouseX, mouseY);
        renderMainPanel(graphics, mouseX, mouseY);
        renderBottomBar(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.expandedModule != null) {
            renderModuleSettingsDrawer(graphics, mouseX, mouseY, this.expandedModule);
        }
    }

    private void renderLeftSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiDrawHelper.drawPanel(graphics, sidebarX, sidebarY, sidebarW, sidebarH, COL_SIDEBAR, COL_BORDER);

        // Custom geometric emblem (no external branding or copied artwork)
        int embX = sidebarX + 12;
        int embY = sidebarY + 12;
        graphics.fill(embX, embY, embX + 18, embY + 18, COL_ACCENT);
        graphics.fill(embX + 4, embY + 4, embX + 14, embY + 14, COL_SIDEBAR);
        graphics.fill(embX + 7, embY + 7, embX + 11, embY + 11, COL_ACCENT);

        graphics.drawString(this.font, "VIEWGRID", embX + 24, embY + 2, COL_TEXT_PRIMARY, false);
        graphics.drawString(this.font, "1.21.11 Client", embX + 24, embY + 12, COL_TEXT_MUTED, false);

        int navY = sidebarY + 42;
        int navItemH = 26;
        for (Category cat : Category.values()) {
            boolean selected = (cat == this.activeCategory);
            boolean hovered = GuiDrawHelper.isInside(mouseX, mouseY, sidebarX + 8, navY, sidebarW - 16, navItemH);
            int rowFill = selected ? 0x3338BDF8 : (hovered ? 0x1AFFFFFF : 0x00000000);
            graphics.fill(sidebarX + 8, navY, sidebarX + sidebarW - 8, navY + navItemH, rowFill);

            if (selected) {
                graphics.fill(sidebarX + 8, navY + 4, sidebarX + 11, navY + navItemH - 4, COL_ACCENT);
            }

            int count = moduleManager.getByCategoryAndFilter(cat, "").size();
            graphics.drawString(this.font, cat.getLabel(), sidebarX + 18, navY + 9, selected ? COL_TEXT_PRIMARY : COL_TEXT_MUTED, false);
            String countStr = String.valueOf(count);
            graphics.drawString(this.font, countStr, sidebarX + sidebarW - 16 - this.font.width(countStr), navY + 9, COL_TEXT_MUTED, false);

            navY += navItemH + 4;
        }

        // Compact footer card: session / account status and active module count
        int footH = 44;
        int footX = sidebarX + 8;
        int footY = sidebarY + sidebarH - footH - 8;
        int footW = sidebarW - 16;
        GuiDrawHelper.drawPanel(graphics, footX, footY, footW, footH, 0xCC0B101A, COL_BORDER);

        String userName = (Minecraft.getInstance().getUser() != null)
            ? Minecraft.getInstance().getUser().getName()
            : "Offline";
        graphics.drawString(this.font, "Session: " + userName, footX + 8, footY + 8, COL_TEXT_PRIMARY, false);
        graphics.drawString(this.font, "Active: " + moduleManager.getActiveCount() + " / " + moduleManager.getModules().size(), footX + 8, footY + 22, COL_ACCENT, false);
    }

    private void renderMainPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiDrawHelper.drawPanel(graphics, panelX, panelY, panelW, panelH, COL_PANEL, COL_BORDER);

        String query = this.searchBox != null ? this.searchBox.getValue() : "";
        List<Module> list = moduleManager.getByCategoryAndFilter(
            query.isBlank() ? this.activeCategory : null,
            query
        );

        long activeInCat = list.stream().filter(Module::isEnabled).count();
        String title = query.isBlank() ? this.activeCategory.getLabel() : "Search Results";
        graphics.drawString(this.font, title, panelX + 14, panelY + 12, COL_TEXT_PRIMARY, false);
        String meta = list.size() + " modules  |  " + activeInCat + " active";
        graphics.drawString(this.font, meta, panelX + panelW - 14 - this.font.width(meta), panelY + 12, COL_TEXT_MUTED, false);

        int gridTop = panelY + 30;
        int gridLeft = panelX + 12;
        int gridW = panelW - 24;
        int cols = 2;
        int gap = 10;
        int cardW = Math.max(100, (gridW - gap * (cols - 1)) / cols);
        int cardH = Boolean.TRUE.equals(configManager.isCompactCards()) ? 44 : 54;

        for (int i = 0; i < list.size(); i++) {
            Module mod = list.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = gridLeft + col * (cardW + gap);
            int cy = gridTop + row * (cardH + gap);

            if (cy + cardH > panelY + panelH - 10) {
                break;
            }

            int bg = mod.isEnabled() ? COL_CARD_ACTIVE : COL_CARD;
            int border = mod.isEnabled() ? COL_ACCENT : COL_BORDER;
            GuiDrawHelper.drawPanel(graphics, cx, cy, cardW, cardH, bg, border);

            graphics.drawString(this.font, mod.getName(), cx + 10, cy + 8, COL_TEXT_PRIMARY, false);
            if (!Boolean.TRUE.equals(configManager.isCompactCards())) {
                String trimmed = this.font.plainSubstrByWidth(mod.getSummary(), Math.max(40, cardW - 68));
                graphics.drawString(this.font, trimmed, cx + 10, cy + 22, COL_TEXT_MUTED, false);
            }

            // Settings gear / expand button
            int gearX = cx + cardW - 48;
            int gearY = cy + (cardH - 16) / 2;
            GuiDrawHelper.drawPanel(graphics, gearX, gearY, 16, 16, 0x8824324A, COL_BORDER);
            graphics.drawString(this.font, "*", gearX + 5, gearY + 4, COL_TEXT_PRIMARY, false);

            // Toggle switch pill
            int swX = cx + cardW - 28;
            int swY = cy + (cardH - 14) / 2;
            GuiDrawHelper.drawPill(graphics, swX, swY, 20, 14, mod.isEnabled() ? 0xFF0EA5E9 : 0xFF334155, COL_BORDER);
            int knobX = mod.isEnabled() ? swX + 8 : swX + 2;
            graphics.fill(knobX, swY + 2, knobX + 10, swY + 12, COL_TEXT_PRIMARY);
        }
    }

    private void renderModuleSettingsDrawer(GuiGraphics graphics, int mouseX, int mouseY, Module mod) {
        int dw = Math.min(260, this.width - 40);
        int dh = Math.min(260, this.height - 80);
        int dx = (this.width - dw) / 2;
        int dy = (this.height - dh) / 2;

        GuiDrawHelper.drawPanel(graphics, dx, dy, dw, dh, 0xF50B101B, COL_ACCENT);
        graphics.drawString(this.font, mod.getName() + " Settings", dx + 12, dy + 10, COL_TEXT_PRIMARY, false);
        graphics.drawString(this.font, "[X]", dx + dw - 24, dy + 10, 0xFFFF6B6B, false);

        int sy = dy + 28;
        // Keybind row
        String keyLabel = (listeningKeybindModule == mod)
            ? "Press any key..."
            : "Bind: " + (mod.getKeybind() == GLFW.GLFW_KEY_UNKNOWN ? "None" : GLFW.glfwGetKeyName(mod.getKeybind(), 0));
        GuiDrawHelper.drawPanel(graphics, dx + 12, sy, dw - 24, 18, 0x991E293B, COL_BORDER);
        graphics.drawString(this.font, keyLabel, dx + 18, sy + 5, COL_ACCENT, false);
        sy += 22;

        for (Setting<?> setting : mod.getSettings()) {
            if (sy + 20 > dy + dh - 10) break;
            switch (setting.getType()) {
                case BOOLEAN -> {
                    @SuppressWarnings("unchecked")
                    Setting<Boolean> bs = (Setting<Boolean>) setting;
                    String stateStr = Boolean.TRUE.equals(bs.getValue()) ? "[ON]" : "[OFF]";
                    graphics.drawString(this.font, bs.getLabel() + ": " + stateStr, dx + 12, sy + 5, COL_TEXT_PRIMARY, false);
                }
                case DOUBLE -> {
                    @SuppressWarnings("unchecked")
                    Setting<Double> ds = (Setting<Double>) setting;
                    double frac = (ds.getValue() - ds.getMinDouble()) / Math.max(1e-6, ds.getMaxDouble() - ds.getMinDouble());
                    int barW = dw - 24;
                    graphics.fill(dx + 12, sy + 12, dx + 12 + barW, sy + 16, 0xFF1E293B);
                    graphics.fill(dx + 12, sy + 12, dx + 12 + (int) (barW * frac), sy + 16, COL_ACCENT);
                    graphics.drawString(this.font, String.format("%s: %.2f", ds.getLabel(), ds.getValue()), dx + 12, sy + 2, COL_TEXT_PRIMARY, false);
                }
                case INT -> {
                    @SuppressWarnings("unchecked")
                    Setting<Integer> is = (Setting<Integer>) setting;
                    double frac = (double) (is.getValue() - is.getMinInt()) / Math.max(1, is.getMaxInt() - is.getMinInt());
                    int barW = dw - 24;
                    graphics.fill(dx + 12, sy + 12, dx + 12 + barW, sy + 16, 0xFF1E293B);
                    graphics.fill(dx + 12, sy + 12, dx + 12 + (int) (barW * frac), sy + 16, COL_ACCENT);
                    graphics.drawString(this.font, is.getLabel() + ": " + is.getValue(), dx + 12, sy + 2, COL_TEXT_PRIMARY, false);
                }
                case ENUM -> {
                    @SuppressWarnings("unchecked")
                    Setting<Enum<?>> es = (Setting<Enum<?>>) setting;
                    graphics.drawString(this.font, es.getLabel() + ": < " + es.getValue().name() + " >", dx + 12, sy + 5, COL_TEXT_PRIMARY, false);
                }
                case COLOR -> {
                    @SuppressWarnings("unchecked")
                    Setting<ColorRgba> cs = (Setting<ColorRgba>) setting;
                    graphics.drawString(this.font, cs.getLabel(), dx + 12, sy + 5, COL_TEXT_PRIMARY, false);
                    GuiDrawHelper.drawSwatch(graphics, dx + dw - 30, sy + 2, 14, cs.getValue());
                }
                case BLOCK_LIST -> {
                    graphics.drawString(this.font, setting.getLabel() + " (configured)", dx + 12, sy + 5, COL_TEXT_MUTED, false);
                }
            }
            sy += 22;
        }
    }

    private void renderBottomBar(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiDrawHelper.drawPill(graphics, barX, barY, barW, barH, COL_SIDEBAR, COL_BORDER);

        int bx = barX + barW - 96;
        int by = barY + 8;

        // Quick action 1: Audio feedback toggle
        GuiDrawHelper.drawPanel(graphics, bx, by, 24, 20, configManager.isAudioFeedback() ? 0x4438BDF8 : 0x441E293B, COL_BORDER);
        graphics.drawString(this.font, "S", bx + 9, by + 6, COL_TEXT_PRIMARY, false);

        // Quick action 2: Compact card layout toggle
        GuiDrawHelper.drawPanel(graphics, bx + 30, by, 24, 20, configManager.isCompactCards() ? 0x4438BDF8 : 0x441E293B, COL_BORDER);
        graphics.drawString(this.font, "L", bx + 39, by + 6, COL_TEXT_PRIMARY, false);

        // Quick action 3: Save config button
        GuiDrawHelper.drawPanel(graphics, bx + 60, by, 26, 20, 0x4410B981, COL_BORDER);
        graphics.drawString(this.font, "CFG", bx + 64, by + 6, COL_TEXT_PRIMARY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.expandedModule != null) {
            int dw = Math.min(260, this.width - 40);
            int dh = Math.min(260, this.height - 80);
            int dx = (this.width - dw) / 2;
            int dy = (this.height - dh) / 2;

            if (GuiDrawHelper.isInside(mouseX, mouseY, dx + dw - 28, dy + 6, 22, 16)
                || !GuiDrawHelper.isInside(mouseX, mouseY, dx, dy, dw, dh)) {
                this.expandedModule = null;
                this.listeningKeybindModule = null;
                this.activeColorSetting = null;
                this.hexInputBox.setVisible(false);
                this.configManager.save();
                return true;
            }

            int sy = dy + 28;
            if (GuiDrawHelper.isInside(mouseX, mouseY, dx + 12, sy, dw - 24, 18)) {
                this.listeningKeybindModule = this.expandedModule;
                return true;
            }
            sy += 22;

            for (Setting<?> setting : this.expandedModule.getSettings()) {
                if (sy + 20 > dy + dh - 10) break;
                if (GuiDrawHelper.isInside(mouseX, mouseY, dx + 12, sy, dw - 24, 20)) {
                    handleSettingClick(setting, mouseX, dx + 12, dw - 24, button);
                    this.configManager.save();
                    return true;
                }
                sy += 22;
            }
            return true;
        }

        // Sidebar categories
        int navY = sidebarY + 42;
        int navItemH = 26;
        for (Category cat : Category.values()) {
            if (GuiDrawHelper.isInside(mouseX, mouseY, sidebarX + 8, navY, sidebarW - 16, navItemH)) {
                this.activeCategory = cat;
                this.configManager.setLastCategory(cat.name());
                this.configManager.save();
                return true;
            }
            navY += navItemH + 4;
        }

        // Module cards
        String query = this.searchBox != null ? this.searchBox.getValue() : "";
        List<Module> list = moduleManager.getByCategoryAndFilter(
            query.isBlank() ? this.activeCategory : null,
            query
        );
        int gridTop = panelY + 30;
        int gridLeft = panelX + 12;
        int gridW = panelW - 24;
        int cols = 2;
        int gap = 10;
        int cardW = Math.max(100, (gridW - gap * (cols - 1)) / cols);
        int cardH = Boolean.TRUE.equals(configManager.isCompactCards()) ? 44 : 54;

        for (int i = 0; i < list.size(); i++) {
            Module mod = list.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = gridLeft + col * (cardW + gap);
            int cy = gridTop + row * (cardH + gap);
            if (cy + cardH > panelY + panelH - 10) break;

            int gearX = cx + cardW - 48;
            int gearY = cy + (cardH - 16) / 2;
            if (GuiDrawHelper.isInside(mouseX, mouseY, gearX, gearY, 16, 16) || button == 1) {
                if (GuiDrawHelper.isInside(mouseX, mouseY, cx, cy, cardW, cardH)) {
                    this.expandedModule = mod;
                    this.listeningKeybindModule = null;
                    return true;
                }
            }

            if (GuiDrawHelper.isInside(mouseX, mouseY, cx, cy, cardW, cardH) && button == 0) {
                mod.toggle();
                this.configManager.save();
                return true;
            }
        }

        // Bottom quick-action buttons
        int bx = barX + barW - 96;
        int by = barY + 8;
        if (GuiDrawHelper.isInside(mouseX, mouseY, bx, by, 24, 20)) {
            configManager.setAudioFeedback(!configManager.isAudioFeedback());
            configManager.save();
            return true;
        }
        if (GuiDrawHelper.isInside(mouseX, mouseY, bx + 30, by, 24, 20)) {
            configManager.setCompactCards(!configManager.isCompactCards());
            configManager.save();
            return true;
        }
        if (GuiDrawHelper.isInside(mouseX, mouseY, bx + 60, by, 26, 20)) {
            configManager.save();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @SuppressWarnings("unchecked")
    private void handleSettingClick(Setting<?> setting, double mouseX, int rowX, int rowW, int button) {
        switch (setting.getType()) {
            case BOOLEAN -> {
                Setting<Boolean> bs = (Setting<Boolean>) setting;
                bs.setValue(!Boolean.TRUE.equals(bs.getValue()));
            }
            case DOUBLE -> {
                Setting<Double> ds = (Setting<Double>) setting;
                double frac = Math.max(0.0, Math.min(1.0, (mouseX - rowX) / Math.max(1.0, rowW)));
                ds.setValue(ds.getMinDouble() + frac * (ds.getMaxDouble() - ds.getMinDouble()));
            }
            case INT -> {
                Setting<Integer> is = (Setting<Integer>) setting;
                double frac = Math.max(0.0, Math.min(1.0, (mouseX - rowX) / Math.max(1.0, rowW)));
                int val = (int) Math.round(is.getMinInt() + frac * (is.getMaxInt() - is.getMinInt()));
                is.setValue(val);
            }
            case ENUM -> {
                Setting<Enum<?>> es = (Setting<Enum<?>>) setting;
                Enum<?>[] vals = es.getEnumConstants();
                if (vals != null && vals.length > 0) {
                    int idx = es.getValue().ordinal();
                    int next = (button == 1) ? (idx - 1 + vals.length) % vals.length : (idx + 1) % vals.length;
                    es.setValue(vals[next]);
                }
            }
            case COLOR -> {
                Setting<ColorRgba> cs = (Setting<ColorRgba>) setting;
                ColorRgba cur = cs.getValue();
                // Cycle quick presets on click; hex field can also be used when expanded.
                ColorRgba next = new ColorRgba((cur.getR() + 64) % 256, (cur.getG() + 48) % 256, (cur.getB() + 96) % 256, cur.getA());
                cs.setValue(next);
                this.activeColorSetting = cs;
                this.hexInputBox.setValue(next.toHex8());
            }
            case BLOCK_LIST -> {
                // Editable via config/viewgrid/config.json or extended text drawer.
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.listeningKeybindModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                this.listeningKeybindModule.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                this.listeningKeybindModule.setKeybind(keyCode);
            }
            this.listeningKeybindModule = null;
            this.configManager.save();
            return true;
        }

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_F && this.searchBox != null) {
            setFocused(this.searchBox);
            this.searchBox.setFocused(true);
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
