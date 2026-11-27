package dev.engsplay.ui.screen;

import dev.engsplay.EngsplayClient;
import dev.engsplay.config.AccentColor;
import dev.engsplay.config.ClientConfig;
import dev.engsplay.config.UiSize;
import dev.engsplay.module.Category;
import dev.engsplay.module.Module;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.module.Setting;
import dev.engsplay.stats.SessionStatsTracker;
import dev.engsplay.ui.anim.SpringValue;
import dev.engsplay.ui.render.GlassRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class EngsplayClickGuiScreen extends Screen {
    public enum NavTab {
        HOME("Home", null),
        COMBAT("Combat", Category.COMBAT),
        MOVEMENT("Movement", Category.MOVEMENT),
        VISUALS("Visuals", Category.VISUALS),
        PLAYER("Player", Category.PLAYER),
        WORLD("World", Category.WORLD),
        HUD("HUD", Category.HUD),
        SKIN("Skin", Category.SKIN),
        FRIENDS("Friends", null),
        SETTINGS("Settings", null),
        PROFILE("Profile", null);

        public final String label;
        public final Category category;

        NavTab(String label, Category category) {
            this.label = label;
            this.category = category;
        }
    }

    private NavTab activeTab = NavTab.HOME;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private float scrollOffset = 0f;
    private float targetScroll = 0f;
    private long lastFrameNs = System.nanoTime();

    private Module bindingModule = null;
    private boolean bindingGuiKey = false;
    private Setting.SliderSetting draggingSlider = null;
    private int draggingSliderX = 0;
    private int draggingSliderW = 1;
    private boolean draggingVolume = false;
    private int volSliderX = 0;
    private int volSliderW = 140;

    private final SpringValue openAnim = new SpringValue(0f, 260f, 22f);

    public EngsplayClickGuiScreen() {
        super(Text.literal("engsplay"));
        openAnim.setTarget(1f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientConfig cfg = EngsplayClient.getInstance().getConfig();
        if (cfg.backgroundBlur) {
            this.applyBlur();
        }
        context.fill(0, 0, this.width, this.height, 0x6609080A);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrameNs) / 1_000_000_000f);
        lastFrameNs = now;

        float scaleAnim = openAnim.update(dt);
        scrollOffset += (targetScroll - scrollOffset) * Math.min(1f, dt * 16f);

        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();
        ModuleManager mm = client.getModuleManager();

        for (Module m : mm.getModules()) {
            m.getToggleAnim().update(dt);
            m.getExpandAnim().update(dt);
        }

        if (draggingSlider != null) {
            float norm = (float) (mouseX - draggingSliderX) / (float) draggingSliderW;
            draggingSlider.setNormalized(norm);
        }
        if (draggingVolume) {
            cfg.uiSoundVolume = Math.max(0f, Math.min(1f, (float) (mouseX - volSliderX) / (float) volSliderW));
        }

        renderBackground(ctx, mouseX, mouseY, delta);

        int winW = Math.min(880, this.width - 40);
        int winH = Math.min(540, this.height - 36);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        ctx.getMatrices().push();
        float s = (0.94f + 0.06f * scaleAnim) * cfg.uiSize.getScale();
        ctx.getMatrices().translate(winX + winW / 2f, winY + winH / 2f, 0f);
        ctx.getMatrices().scale(s, s, 1f);
        ctx.getMatrices().translate(-(winX + winW / 2f), -(winY + winH / 2f), 0f);

        GlassRenderer.drawGlassCard(ctx, winX, winY, winW, winH, 16, GlassRenderer.PANEL_BG, GlassRenderer.BORDER_STRONG);

        int sidebarW = 176;
        GlassRenderer.fillRoundedRect(ctx, winX + 1, winY + 1, sidebarW, winH - 2, 15, GlassRenderer.SIDEBAR_BG);
        ctx.fill(winX + sidebarW, winY + 10, winX + sidebarW + 1, winY + winH - 10, GlassRenderer.BORDER_SUBTLE);

        renderSidebar(ctx, winX, winY, sidebarW, winH, mouseX, mouseY);

        int contentX = winX + sidebarW + 14;
        int contentW = winW - sidebarW - 28;
        int topBarH = 44;

        renderTopBar(ctx, contentX, winY + 10, contentW, topBarH, mouseX, mouseY);

        int bodyY = winY + topBarH + 16;
        int bodyH = winH - topBarH - 26;

        ctx.enableScissor(contentX - 2, bodyY, contentX + contentW + 2, bodyY + bodyH);
        if (!searchQuery.isBlank()) {
            renderModuleList(ctx, contentX, bodyY - (int) scrollOffset, contentW, mm.search(searchQuery), mouseX, mouseY, "Search Results");
        } else if (activeTab == NavTab.HOME) {
            renderHomeDashboard(ctx, contentX, bodyY - (int) scrollOffset, contentW, mouseX, mouseY);
        } else if (activeTab.category != null) {
            renderModuleList(ctx, contentX, bodyY - (int) scrollOffset, contentW, mm.getByCategory(activeTab.category), mouseX, mouseY, activeTab.label);
        } else if (activeTab == NavTab.PROFILE) {
            renderProfileView(ctx, contentX, bodyY - (int) scrollOffset, contentW, mouseX, mouseY);
        } else if (activeTab == NavTab.FRIENDS) {
            renderFriendsView(ctx, contentX, bodyY - (int) scrollOffset, contentW);
        } else if (activeTab == NavTab.SETTINGS) {
            renderSettingsView(ctx, contentX, bodyY - (int) scrollOffset, contentW, mouseX, mouseY);
        }
        ctx.disableScissor();

        ctx.getMatrices().pop();
    }

    private void renderSidebar(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        ClientConfig cfg = EngsplayClient.getInstance().getConfig();
        ModuleManager mm = EngsplayClient.getInstance().getModuleManager();
        int accent = cfg.accentColor.getArgb();

        GlassRenderer.drawEmblem(ctx, this.textRenderer, x + 14, y + 14, 26, accent);
        ctx.drawTextWithShadow(this.textRenderer, "engsplay", x + 46, y + 15, GlassRenderer.TEXT_PRIMARY);
        ctx.drawText(this.textRenderer, "1.21.11 • Fabric", x + 46, y + 27, GlassRenderer.TEXT_MUTED, false);

        int navY = y + 54;
        NavTab[] mainTabs = {
                NavTab.HOME, NavTab.COMBAT, NavTab.MOVEMENT, NavTab.VISUALS,
                NavTab.PLAYER, NavTab.WORLD, NavTab.HUD, NavTab.SKIN
        };

        for (NavTab tab : mainTabs) {
            boolean selected = searchQuery.isBlank() && activeTab == tab;
            boolean hovered = GlassRenderer.isHovered(mx, my, x + 10, navY, w - 20, 24);
            if (selected) {
                GlassRenderer.fillRoundedRect(ctx, x + 10, navY, w - 20, 24, 8, cfg.accentColor.withAlpha(45));
                GlassRenderer.drawRoundedOutline(ctx, x + 10, navY, w - 20, 24, 8, cfg.accentColor.withAlpha(110));
            } else if (hovered) {
                GlassRenderer.fillRoundedRect(ctx, x + 10, navY, w - 20, 24, 8, 0x18FFFFFF);
            }

            int textColor = selected ? GlassRenderer.TEXT_PRIMARY : GlassRenderer.TEXT_SECONDARY;
            ctx.drawText(this.textRenderer, tab.label, x + 20, navY + 8, textColor, false);

            if (tab.category != null) {
                int count = (tab == NavTab.HUD) ? mm.getTotalCount(tab.category) : mm.getActiveCount(tab.category);
                if (count > 0) {
                    String badge = String.valueOf(count);
                    int bw = Math.max(16, this.textRenderer.getWidth(badge) + 8);
                    int bx = x + w - 16 - bw;
                    int badgeBg = selected ? accent : 0x26FFFFFF;
                    GlassRenderer.fillRoundedRect(ctx, bx, navY + 5, bw, 14, 7, badgeBg);
                    ctx.drawText(this.textRenderer, badge, bx + (bw - this.textRenderer.getWidth(badge)) / 2, navY + 8,
                            selected ? 0xFFFFFFFF : GlassRenderer.TEXT_SECONDARY, false);
                }
            }
            navY += 27;
        }

        int clientSecY = y + h - 122;
        ctx.drawText(this.textRenderer, "CLIENT", x + 18, clientSecY, GlassRenderer.TEXT_MUTED, false);
        clientSecY += 14;

        NavTab[] bottomTabs = {NavTab.FRIENDS, NavTab.SETTINGS};
        for (NavTab tab : bottomTabs) {
            boolean selected = searchQuery.isBlank() && activeTab == tab;
            boolean hovered = GlassRenderer.isHovered(mx, my, x + 10, clientSecY, w - 20, 24);
            if (selected) {
                GlassRenderer.fillRoundedRect(ctx, x + 10, clientSecY, w - 20, 24, 8, cfg.accentColor.withAlpha(45));
                GlassRenderer.drawRoundedOutline(ctx, x + 10, clientSecY, w - 20, 24, 8, cfg.accentColor.withAlpha(110));
            } else if (hovered) {
                GlassRenderer.fillRoundedRect(ctx, x + 10, clientSecY, w - 20, 24, 8, 0x18FFFFFF);
            }
            ctx.drawText(this.textRenderer, tab.label, x + 20, clientSecY + 8,
                    selected ? GlassRenderer.TEXT_PRIMARY : GlassRenderer.TEXT_SECONDARY, false);
            clientSecY += 27;
        }

        int chipY = y + h - 48;
        boolean chipHover = GlassRenderer.isHovered(mx, my, x + 10, chipY, w - 20, 38);
        GlassRenderer.drawGlassCard(ctx, x + 10, chipY, w - 20, 38, 10,
                chipHover ? GlassRenderer.CARD_HOVER_BG : GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);

        Identifier skin = GlassRenderer.getLocalPlayerSkin();
        GlassRenderer.drawPlayerHead(ctx, skin, x + 17, chipY + 7, 24);

        String username = getPlayerName();
        ctx.drawText(this.textRenderer, username, x + 47, chipY + 9, GlassRenderer.TEXT_PRIMARY, false);
        ctx.drawText(this.textRenderer, "View profile", x + 47, chipY + 21, accent, false);
    }

    private void renderTopBar(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        ClientConfig cfg = EngsplayClient.getInstance().getConfig();
        int accent = cfg.accentColor.getArgb();

        String greeting = getTimeGreeting() + ", " + getPlayerName();
        ctx.drawTextWithShadow(this.textRenderer, greeting, x, y + 4, GlassRenderer.TEXT_PRIMARY);
        ctx.drawText(this.textRenderer, "Everything at a glance", x, y + 17, GlassRenderer.TEXT_MUTED, false);

        int searchW = 210;
        int searchH = 26;
        int searchX = x + (w - searchW) / 2 + 24;
        int searchY = y + 6;
        int searchBorder = searchFocused ? accent : GlassRenderer.BORDER_SUBTLE;
        GlassRenderer.drawGlassCard(ctx, searchX, searchY, searchW, searchH, 10, GlassRenderer.CARD_BG, searchBorder);

        String displayQuery = searchQuery.isEmpty() && !searchFocused
                ? "Search for a module..."
                : searchQuery + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        int qColor = searchQuery.isEmpty() && !searchFocused ? GlassRenderer.TEXT_MUTED : GlassRenderer.TEXT_PRIMARY;
        ctx.drawText(this.textRenderer, displayQuery, searchX + 10, searchY + 9, qColor, false);

        int langW = 34;
        int langH = 26;
        int langX = x + w - langW;
        int langY = y + 6;
        boolean langHover = GlassRenderer.isHovered(mx, my, langX, langY, langW, langH);
        GlassRenderer.drawGlassCard(ctx, langX, langY, langW, langH, 10,
                langHover ? GlassRenderer.CARD_HOVER_BG : GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        int lw = this.textRenderer.getWidth(cfg.language);
        ctx.drawText(this.textRenderer, cfg.language, langX + (langW - lw) / 2, langY + 9, GlassRenderer.TEXT_PRIMARY, false);
    }

    private void renderHomeDashboard(DrawContext ctx, int x, int y, int w, int mx, int my) {
        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();
        ModuleManager mm = client.getModuleManager();
        SessionStatsTracker stats = client.getStatsTracker();
        int accent = cfg.accentColor.getArgb();

        int card1H = 96;
        GlassRenderer.drawGlassCard(ctx, x, y, w, card1H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);

        Identifier skin = GlassRenderer.getLocalPlayerSkin();
        GlassRenderer.drawPlayerHead(ctx, skin, x + 14, y + 14, 34);

        String username = getPlayerName();
        ctx.drawTextWithShadow(this.textRenderer, username, x + 56, y + 15, GlassRenderer.TEXT_PRIMARY);
        String worldStatus = (MinecraftClient.getInstance().isInSingleplayer() ? "Playing singleplayer" : "Playing multiplayer")
                + " • " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH));
        ctx.drawText(this.textRenderer, worldStatus, x + 56, y + 28, GlassRenderer.TEXT_SECONDARY, false);

        String sessionStr = "Session " + stats.formatSessionShort();
        String todayStr = "Today " + stats.formatDuration(600 + stats.getSessionElapsedSeconds());
        String streakStr = cfg.streakDays + (cfg.streakDays == 1 ? " day in a row" : " days in a row");
        drawMiniStatBadge(ctx, x + 14, y + 58, sessionStr, GlassRenderer.TEXT_PRIMARY);
        drawMiniStatBadge(ctx, x + 102, y + 58, todayStr, GlassRenderer.TEXT_PRIMARY);
        drawMiniStatBadge(ctx, x + 190, y + 58, streakStr, accent);

        int graphW = 210;
        int graphH = 68;
        int graphX = x + w - graphW - 14;
        int graphY = y + 14;
        GlassRenderer.drawGlassCard(ctx, graphX, graphY, graphW, graphH, 10, 0x80100E10, GlassRenderer.BORDER_SUBTLE);

        int currentFps = Math.max(1, MinecraftClient.getInstance().getCurrentFps());
        String fpsLabel = currentFps + " FPS";
        ctx.drawText(this.textRenderer, fpsLabel, graphX + graphW - this.textRenderer.getWidth(fpsLabel) - 8, graphY + 6, accent, false);
        ctx.drawText(this.textRenderer, "Framerate", graphX + 8, graphY + 6, GlassRenderer.TEXT_MUTED, false);

        float[] history = stats.getFpsHistoryOrdered();
        float maxFps = 60f;
        for (float v : history) if (v > maxFps) maxFps = v;
        int plotX = graphX + 8;
        int plotY = graphY + 20;
        int plotW = graphW - 16;
        int plotH = graphH - 26;

        for (int i = 1; i < history.length; i++) {
            int x1 = plotX + (int) ((i - 1) / (float) (history.length - 1) * plotW);
            int x2 = plotX + (int) (i / (float) (history.length - 1) * plotW);
            int y1 = plotY + plotH - (int) (Math.min(1f, history[i - 1] / maxFps) * plotH);
            int y2 = plotY + plotH - (int) (Math.min(1f, history[i] / maxFps) * plotH);
            drawThickLine(ctx, x1, y1, x2, y2, accent);
        }

        int row2Y = y + card1H + 10;
        int row2H = 112;
        int ringCardW = 178;
        GlassRenderer.drawGlassCard(ctx, x, row2Y, ringCardW, row2H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);

        int activeCount = mm.getActiveCount();
        int totalCount = mm.getTotalCount();
        float ratio = totalCount > 0 ? (float) activeCount / totalCount : 0f;
        drawProgressRing(ctx, x + ringCardW / 2, row2Y + 46, 26, 4, ratio, accent);

        String ratioText = activeCount + "/" + totalCount;
        ctx.drawText(this.textRenderer, ratioText, x + (ringCardW - this.textRenderer.getWidth(ratioText)) / 2, row2Y + 42,
                GlassRenderer.TEXT_PRIMARY, false);
        String subRing = activeCount + " of " + totalCount + " active";
        ctx.drawText(this.textRenderer, subRing, x + (ringCardW - this.textRenderer.getWidth(subRing)) / 2, row2Y + 86,
                GlassRenderer.TEXT_SECONDARY, false);

        int gridX = x + ringCardW + 10;
        int gridW = w - ringCardW - 10;
        int cols = 4;
        int cellGap = 6;
        int cellW = (gridW - cellGap * (cols - 1)) / cols;
        int cellH = (row2H - cellGap) / 2;

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = gridX + col * (cellW + cellGap);
            int cy = row2Y + row * (cellH + cellGap);
            boolean hover = GlassRenderer.isHovered(mx, my, cx, cy, cellW, cellH);
            GlassRenderer.drawGlassCard(ctx, cx, cy, cellW, cellH, 11,
                    hover ? GlassRenderer.CARD_HOVER_BG : GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
            ctx.drawText(this.textRenderer, cats[i].getTitle(), cx + 10, cy + 10, GlassRenderer.TEXT_PRIMARY, false);
            String cStat = mm.getActiveCount(cats[i]) + " / " + mm.getTotalCount(cats[i]);
            ctx.drawText(this.textRenderer, cStat, cx + 10, cy + 28,
                    mm.getActiveCount(cats[i]) > 0 ? accent : GlassRenderer.TEXT_MUTED, false);
        }

        int row3Y = row2Y + row2H + 10;
        int row3H = 88;
        GlassRenderer.drawGlassCard(ctx, x, row3Y, w, row3H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, "Quick Toggle", x + 14, row3Y + 10, GlassRenderer.TEXT_PRIMARY, false);

        String[] quickIds = {"clean_movement", "clouds", "fps", "ping", "cps", "coordinates"};
        int qCols = 3;
        int qGap = 8;
        int qCellW = (w - 28 - qGap * (qCols - 1)) / qCols;
        int qCellH = 24;
        for (int i = 0; i < quickIds.length; i++) {
            Module m = mm.getById(quickIds[i]);
            if (m == null) continue;
            int col = i % qCols;
            int row = i / qCols;
            int qx = x + 14 + col * (qCellW + qGap);
            int qy = row3Y + 26 + row * (qCellH + 6);
            GlassRenderer.fillRoundedRect(ctx, qx, qy, qCellW, qCellH, 8, 0x60100E10);
            ctx.drawText(this.textRenderer, m.getName(), qx + 8, qy + 8, GlassRenderer.TEXT_PRIMARY, false);
            GlassRenderer.drawPillSwitch(ctx, qx + qCellW - 38, qy + 4, m.getToggleAnim().get(), accent);
        }

        int row4Y = row3Y + row3H + 10;
        int row4H = 84;
        int bGap = 10;
        int bW = (w - bGap * 2) / 3;

        GlassRenderer.drawGlassCard(ctx, x, row4Y, bW, row4H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, "Config Profile", x + 12, row4Y + 10, GlassRenderer.TEXT_MUTED, false);
        ctx.drawTextWithShadow(this.textRenderer, cfg.activeProfile, x + 12, row4Y + 24, GlassRenderer.TEXT_PRIMARY);
        ctx.drawText(this.textRenderer, "1 profile saved automatically", x + 12, row4Y + 38, GlassRenderer.TEXT_SECONDARY, false);
        drawActionButton(ctx, x + 12, row4Y + 55, 68, 20, "Manage", accent, mx, my);

        int cardBX = x + bW + bGap;
        GlassRenderer.drawGlassCard(ctx, cardBX, row4Y, bW, row4H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, "Friends", cardBX + 12, row4Y + 10, GlassRenderer.TEXT_MUTED, false);
        ctx.drawTextWithShadow(this.textRenderer, "3 friends • 0 online", cardBX + 12, row4Y + 24, GlassRenderer.TEXT_PRIMARY);
        for (int i = 0; i < 3; i++) {
            GlassRenderer.drawPlayerHead(ctx, skin, cardBX + 12 + i * 14, row4Y + 38, 13);
        }
        drawActionButton(ctx, cardBX + 12, row4Y + 55, 56, 20, "View", accent, mx, my);

        int cardCX = x + (bW + bGap) * 2;
        GlassRenderer.drawGlassCard(ctx, cardCX, row4Y, bW, row4H, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, "HUD", cardCX + 12, row4Y + 10, GlassRenderer.TEXT_MUTED, false);
        int hudCount = mm.getTotalCount(Category.HUD);
        ctx.drawTextWithShadow(this.textRenderer, hudCount + " elements", cardCX + 12, row4Y + 24, GlassRenderer.TEXT_PRIMARY);
        ctx.drawText(this.textRenderer, "Drag with T", cardCX + 12, row4Y + 38, GlassRenderer.TEXT_SECONDARY, false);
        drawActionButton(ctx, cardCX + 12, row4Y + 55, 76, 20, "Customize", accent, mx, my);
    }

    private void renderModuleList(DrawContext ctx, int x, int y, int w, List<Module> modules, int mx, int my, String headerTitle) {
        ClientConfig cfg = EngsplayClient.getInstance().getConfig();
        int accent = cfg.accentColor.getArgb();

        ctx.drawTextWithShadow(this.textRenderer, headerTitle + " (" + modules.size() + ")", x, y + 2, GlassRenderer.TEXT_PRIMARY);
        int curY = y + 20;

        for (Module m : modules) {
            float exp = m.getExpandAnim().get();
            int settingsRows = m.getSettings().size() + 1;
            int expandedExtraH = Math.round(exp * (settingsRows * 26 + 10));
            int cardH = 48 + expandedExtraH;

            boolean hover = GlassRenderer.isHovered(mx, my, x, curY, w, 48);
            GlassRenderer.drawGlassCard(ctx, x, curY, w, cardH, 12,
                    hover ? GlassRenderer.CARD_HOVER_BG : GlassRenderer.CARD_BG,
                    m.isEnabled() ? cfg.accentColor.withAlpha(80) : GlassRenderer.BORDER_SUBTLE);

            ctx.drawTextWithShadow(this.textRenderer, m.getName(), x + 14, curY + 11, GlassRenderer.TEXT_PRIMARY);
            ctx.drawText(this.textRenderer, m.getDescription(), x + 14, curY + 25, GlassRenderer.TEXT_SECONDARY, false);

            GlassRenderer.drawPillSwitch(ctx, x + w - 62, curY + 16, m.getToggleAnim().get(), accent);
            String chevron = m.isExpanded() ? "^" : "v";
            ctx.drawText(this.textRenderer, chevron, x + w - 20, curY + 20, GlassRenderer.TEXT_SECONDARY, false);

            if (exp > 0.05f) {
                int setY = curY + 52;
                ctx.fill(x + 14, setY - 4, x + w - 14, setY - 3, GlassRenderer.BORDER_SUBTLE);

                for (Setting<?> s : m.getSettings()) {
                    if (setY > curY + cardH - 16) break;
                    ctx.drawText(this.textRenderer, s.getName(), x + 16, setY + 6, GlassRenderer.TEXT_SECONDARY, false);

                    if (s instanceof Setting.ModeSetting ms) {
                        String val = "< " + ms.get() + " >";
                        int vw = this.textRenderer.getWidth(val) + 14;
                        int vx = x + w - 16 - vw;
                        GlassRenderer.fillRoundedRect(ctx, vx, setY + 2, vw, 18, 7, 0x55100E10);
                        ctx.drawText(this.textRenderer, val, vx + 7, setY + 7, GlassRenderer.TEXT_PRIMARY, false);
                    } else if (s instanceof Setting.SliderSetting ss) {
                        int barW = 140;
                        int barX = x + w - 16 - barW - 44;
                        int barY = setY + 9;
                        GlassRenderer.fillRoundedRect(ctx, barX, barY, barW, 4, 2, 0xFF2E2A2E);
                        int fillW = Math.round(barW * ss.getNormalized());
                        GlassRenderer.fillRoundedRect(ctx, barX, barY, fillW, 4, 2, accent);
                        GlassRenderer.fillRoundedRect(ctx, barX + fillW - 4, barY - 3, 8, 10, 4, 0xFFFFFFFF);

                        String valStr = ss.formatValue();
                        ctx.drawText(this.textRenderer, valStr, x + w - 16 - this.textRenderer.getWidth(valStr), setY + 6,
                                GlassRenderer.TEXT_PRIMARY, false);
                    } else if (s instanceof Setting.BoolSetting bs) {
                        GlassRenderer.drawPillSwitch(ctx, x + w - 48, setY + 3, bs.get() ? 1f : 0f, accent);
                    } else if (s instanceof Setting.ColorSetting cs) {
                        int[] presets = cs.getPresets();
                        int startX = x + w - 16 - presets.length * 18;
                        for (int i = 0; i < presets.length; i++) {
                            int sx = startX + i * 18;
                            GlassRenderer.fillRoundedRect(ctx, sx, setY + 4, 14, 14, 7, presets[i]);
                            if (cs.get() == presets[i]) {
                                GlassRenderer.drawRoundedOutline(ctx, sx - 2, setY + 2, 18, 18, 9, 0xFFFFFFFF);
                            }
                        }
                    }
                    setY += 26;
                }

                if (setY <= curY + cardH - 12) {
                    ctx.drawText(this.textRenderer, "Keybind", x + 16, setY + 6, GlassRenderer.TEXT_SECONDARY, false);
                    String bindLabel = (bindingModule == m) ? "Press key..." : getKeyName(m.getKeybind());
                    int bw = Math.max(54, this.textRenderer.getWidth(bindLabel) + 14);
                    int bx = x + w - 16 - bw;
                    GlassRenderer.fillRoundedRect(ctx, bx, setY + 2, bw, 18, 7, 0x66100E10);
                    ctx.drawText(this.textRenderer, bindLabel, bx + (bw - this.textRenderer.getWidth(bindLabel)) / 2,
                            setY + 7, bindingModule == m ? accent : GlassRenderer.TEXT_PRIMARY, false);
                }
            }

            curY += cardH + 8;
        }
    }

    private void renderProfileView(DrawContext ctx, int x, int y, int w, int mx, int my) {
        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();
        SessionStatsTracker stats = client.getStatsTracker();
        int accent = cfg.accentColor.getArgb();

        int leftW = 190;
        int totalH = 390;
        GlassRenderer.drawGlassCard(ctx, x, y, leftW, totalH, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);

        if (MinecraftClient.getInstance().player != null) {
            InventoryScreen.drawEntity(ctx, x + 35, y + 40, x + leftW - 35, y + 260, 68, 0.0625f, mx, my,
                    MinecraftClient.getInstance().player);
        }

        String name = getPlayerName();
        ctx.drawTextWithShadow(this.textRenderer, name, x + (leftW - this.textRenderer.getWidth(name)) / 2, y + 285,
                GlassRenderer.TEXT_PRIMARY);
        String status = "Online • Fabric 1.21.11";
        ctx.drawText(this.textRenderer, status, x + (leftW - this.textRenderer.getWidth(status)) / 2, y + 300,
                accent, false);

        int rightX = x + leftW + 12;
        int rightW = w - leftW - 12;
        int statW = (rightW - 16) / 3;
        int statH = 68;

        drawProfileStatCard(ctx, rightX, y, statW, statH, "Today",
                stats.formatDuration(720 + stats.getSessionElapsedSeconds()), accent);
        drawProfileStatCard(ctx, rightX + statW + 8, y, statW, statH, "Total",
                stats.formatDuration(cfg.totalPlaytimeSeconds + stats.getSessionElapsedSeconds()), GlassRenderer.TEXT_PRIMARY);
        drawProfileStatCard(ctx, rightX + (statW + 8) * 2, y, statW, statH, "Sessions",
                String.valueOf(cfg.totalSessions), GlassRenderer.TEXT_PRIMARY);

        int chartY = y + statH + 10;
        int chartH = 135;
        GlassRenderer.drawGlassCard(ctx, rightX, chartY, rightW, chartH, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawTextWithShadow(this.textRenderer, "2h 10m", rightX + 14, chartY + 12, GlassRenderer.TEXT_PRIMARY);
        ctx.drawText(this.textRenderer, "44m/day on average", rightX + 14, chartY + 25, GlassRenderer.TEXT_SECONDARY, false);

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int barAreaY = chartY + 46;
        int barMaxH = 64;
        int slotW = (rightW - 28) / 7;
        for (int i = 0; i < 7; i++) {
            int mins = cfg.weeklyMinutes[i];
            int bh = Math.max(8, Math.round((mins / 60f) * barMaxH));
            int bx = rightX + 14 + i * slotW + (slotW - 18) / 2;
            int by = barAreaY + (barMaxH - bh);
            GlassRenderer.fillRoundedRect(ctx, bx, by, 18, bh, 5, i == 3 ? accent : cfg.accentColor.withAlpha(140));
            ctx.drawText(this.textRenderer, days[i], bx, barAreaY + barMaxH + 6, GlassRenderer.TEXT_MUTED, false);
        }

        int bottomY = chartY + chartH + 10;
        int bottomH = totalH - statH - chartH - 20;
        int gridCardW = (int) (rightW * 0.62f);
        GlassRenderer.drawGlassCard(ctx, rightX, bottomY, gridCardW, bottomH, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, "Activity", rightX + 14, bottomY + 12, GlassRenderer.TEXT_PRIMARY, false);

        for (int col = 0; col < 14; col++) {
            for (int row = 0; row < 5; row++) {
                int level = (col * 3 + row * 7) % 4;
                int cellColor = switch (level) {
                    case 0 -> 0x22FFFFFF;
                    case 1 -> cfg.accentColor.withAlpha(70);
                    case 2 -> cfg.accentColor.withAlpha(145);
                    default -> accent;
                };
                GlassRenderer.fillRoundedRect(ctx, rightX + 14 + col * 14, bottomY + 32 + row * 14, 10, 10, 3, cellColor);
            }
        }

        int mostX = rightX + gridCardW + 10;
        int mostW = rightW - gridCardW - 10;
        GlassRenderer.drawGlassCard(ctx, mostX, bottomY, mostW, bottomH, 14, GlassRenderer.CARD_BG, cfg.accentColor.withAlpha(90));
        ctx.drawText(this.textRenderer, "Most Used", mostX + 14, bottomY + 12, GlassRenderer.TEXT_MUTED, false);
        ctx.drawTextWithShadow(this.textRenderer, "Zoom", mostX + 14, bottomY + 34, accent);
        ctx.drawText(this.textRenderer, "Player • Key C", mostX + 14, bottomY + 50, GlassRenderer.TEXT_SECONDARY, false);
    }

    private void renderFriendsView(DrawContext ctx, int x, int y, int w) {
        GlassRenderer.drawGlassCard(ctx, x, y, w, 160, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawTextWithShadow(this.textRenderer, "Friends (3 friends • 0 online)", x + 16, y + 14, GlassRenderer.TEXT_PRIMARY);
        String[] sampleFriends = {"KiroDev • Offline", "Vexel • Offline", "NoraMC • Offline"};
        Identifier skin = GlassRenderer.getLocalPlayerSkin();
        for (int i = 0; i < sampleFriends.length; i++) {
            int ry = y + 42 + i * 34;
            GlassRenderer.fillRoundedRect(ctx, x + 16, ry, w - 32, 28, 8, 0x55100E10);
            GlassRenderer.drawPlayerHead(ctx, skin, x + 22, ry + 5, 18);
            ctx.drawText(this.textRenderer, sampleFriends[i], x + 48, ry + 10, GlassRenderer.TEXT_SECONDARY, false);
        }
    }

    private void renderSettingsView(DrawContext ctx, int x, int y, int w, int mx, int my) {
        ClientConfig cfg = EngsplayClient.getInstance().getConfig();
        int accent = cfg.accentColor.getArgb();

        int cardH = 230;
        GlassRenderer.drawGlassCard(ctx, x, y, w, cardH, 14, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawTextWithShadow(this.textRenderer, "Client Settings", x + 16, y + 14, GlassRenderer.TEXT_PRIMARY);

        ctx.drawText(this.textRenderer, "Accent Color", x + 16, y + 40, GlassRenderer.TEXT_SECONDARY, false);
        AccentColor[] colors = AccentColor.values();
        int swatchStartX = x + w - 16 - colors.length * 22;
        for (int i = 0; i < colors.length; i++) {
            int sx = swatchStartX + i * 22;
            GlassRenderer.fillRoundedRect(ctx, sx, y + 36, 16, 16, 8, colors[i].getArgb());
            if (cfg.accentColor == colors[i]) {
                GlassRenderer.drawRoundedOutline(ctx, sx - 2, y + 34, 20, 20, 10, 0xFFFFFFFF);
            }
        }

        ctx.drawText(this.textRenderer, "UI Size", x + 16, y + 72, GlassRenderer.TEXT_SECONDARY, false);
        String sizeLabel = "< " + cfg.uiSize.getLabel() + " >";
        int sw = this.textRenderer.getWidth(sizeLabel) + 16;
        GlassRenderer.fillRoundedRect(ctx, x + w - 16 - sw, y + 68, sw, 18, 7, 0x66100E10);
        ctx.drawText(this.textRenderer, sizeLabel, x + w - 8 - sw, y + 73, GlassRenderer.TEXT_PRIMARY, false);

        ctx.drawText(this.textRenderer, "Background Blur", x + 16, y + 104, GlassRenderer.TEXT_SECONDARY, false);
        GlassRenderer.drawPillSwitch(ctx, x + w - 48, y + 100, cfg.backgroundBlur ? 1f : 0f, accent);

        ctx.drawText(this.textRenderer, "UI Sounds", x + 16, y + 136, GlassRenderer.TEXT_SECONDARY, false);
        GlassRenderer.drawPillSwitch(ctx, x + w - 48, y + 132, cfg.uiSounds ? 1f : 0f, accent);

        ctx.drawText(this.textRenderer, "Sound Volume", x + 16, y + 166, GlassRenderer.TEXT_SECONDARY, false);
        int volBarW = 140;
        int volBarX = x + w - 16 - volBarW - 40;
        GlassRenderer.fillRoundedRect(ctx, volBarX, y + 169, volBarW, 4, 2, 0xFF2E2A2E);
        int vFill = Math.round(volBarW * cfg.uiSoundVolume);
        GlassRenderer.fillRoundedRect(ctx, volBarX, y + 169, vFill, 4, 2, accent);
        String volStr = Math.round(cfg.uiSoundVolume * 100) + "%";
        ctx.drawText(this.textRenderer, volStr, x + w - 16 - this.textRenderer.getWidth(volStr), y + 166,
                GlassRenderer.TEXT_PRIMARY, false);

        ctx.drawText(this.textRenderer, "Open and close the GUI", x + 16, y + 198, GlassRenderer.TEXT_SECONDARY, false);
        String guiKeyStr = bindingGuiKey ? "Press key..." : getKeyName(cfg.guiKeybind);
        int kw = Math.max(74, this.textRenderer.getWidth(guiKeyStr) + 14);
        GlassRenderer.fillRoundedRect(ctx, x + w - 16 - kw, y + 194, kw, 18, 7, 0x66100E10);
        ctx.drawText(this.textRenderer, guiKeyStr, x + w - 16 - kw + 7, y + 199,
                bindingGuiKey ? accent : GlassRenderer.TEXT_PRIMARY, false);

        int outroY = y + cardH + 12;
        int outroH = 136;
        GlassRenderer.drawGlassCard(ctx, x, outroY, w, outroH, 16, GlassRenderer.CARD_BG, cfg.accentColor.withAlpha(110));

        int logoSize = 34;
        GlassRenderer.drawEmblem(ctx, this.textRenderer, x + (w - logoSize) / 2, outroY + 16, logoSize, accent);
        String title = "engsplay";
        ctx.drawTextWithShadow(this.textRenderer, title, x + (w - this.textRenderer.getWidth(title)) / 2, outroY + 58,
                GlassRenderer.TEXT_PRIMARY);
        String sub = "a Minecraft 1.21.11 Fabric client";
        ctx.drawText(this.textRenderer, sub, x + (w - this.textRenderer.getWidth(sub)) / 2, outroY + 72,
                GlassRenderer.TEXT_SECONDARY, false);

        int dotsTotalW = colors.length * 20 - 6;
        int dotsX = x + (w - dotsTotalW) / 2;
        int dotsY = outroY + 98;
        for (int i = 0; i < colors.length; i++) {
            int dx = dotsX + i * 20;
            boolean active = cfg.accentColor == colors[i];
            int rSize = active ? 14 : 11;
            int offset = (14 - rSize) / 2;
            GlassRenderer.fillRoundedRect(ctx, dx + offset, dotsY + offset, rSize, rSize, rSize / 2, colors[i].getArgb());
            if (active) {
                GlassRenderer.drawRoundedOutline(ctx, dx - 2, dotsY - 2, 18, 18, 9, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();
        ModuleManager mm = client.getModuleManager();

        int winW = Math.min(880, this.width - 40);
        int winH = Math.min(540, this.height - 36);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;
        int sidebarW = 176;

        int navY = winY + 54;
        NavTab[] mainTabs = {
                NavTab.HOME, NavTab.COMBAT, NavTab.MOVEMENT, NavTab.VISUALS,
                NavTab.PLAYER, NavTab.WORLD, NavTab.HUD, NavTab.SKIN
        };
        for (NavTab tab : mainTabs) {
            if (GlassRenderer.isHovered(mouseX, mouseY, winX + 10, navY, sidebarW - 20, 24)) {
                this.activeTab = tab;
                this.searchQuery = "";
                this.targetScroll = 0f;
                client.playUiClick(1.05f);
                return true;
            }
            navY += 27;
        }

        int clientSecY = winY + winH - 108;
        for (NavTab tab : new NavTab[]{NavTab.FRIENDS, NavTab.SETTINGS}) {
            if (GlassRenderer.isHovered(mouseX, mouseY, winX + 10, clientSecY, sidebarW - 20, 24)) {
                this.activeTab = tab;
                this.searchQuery = "";
                this.targetScroll = 0f;
                client.playUiClick(1.05f);
                return true;
            }
            clientSecY += 27;
        }

        if (GlassRenderer.isHovered(mouseX, mouseY, winX + 10, winY + winH - 48, sidebarW - 20, 38)) {
            this.activeTab = NavTab.PROFILE;
            this.searchQuery = "";
            this.targetScroll = 0f;
            client.playUiClick(1.1f);
            return true;
        }

        int contentX = winX + sidebarW + 14;
        int contentW = winW - sidebarW - 28;
        int searchW = 210;
        int searchX = contentX + (contentW - searchW) / 2 + 24;
        int searchY = winY + 16;
        this.searchFocused = GlassRenderer.isHovered(mouseX, mouseY, searchX, searchY, searchW, 26);

        int langX = contentX + contentW - 34;
        if (GlassRenderer.isHovered(mouseX, mouseY, langX, searchY, 34, 26)) {
            cfg.language = cfg.language.equals("EN") ? "NL" : "EN";
            client.playUiClick(1.0f);
            client.saveConfig();
            return true;
        }

        int bodyY = winY + 60 - (int) scrollOffset;

        if (searchQuery.isBlank() && activeTab == NavTab.HOME) {
            int row2Y = bodyY + 106;
            int ringCardW = 178;
            int gridX = contentX + ringCardW + 10;
            int gridW = contentW - ringCardW - 10;
            int cellW = (gridW - 18) / 4;
            int cellH = 53;
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                int cx = gridX + (i % 4) * (cellW + 6);
                int cy = row2Y + (i / 4) * (cellH + 6);
                if (GlassRenderer.isHovered(mouseX, mouseY, cx, cy, cellW, cellH)) {
                    this.activeTab = NavTab.valueOf(cats[i].name());
                    this.targetScroll = 0f;
                    client.playUiClick(1.05f);
                    return true;
                }
            }

            int row3Y = row2Y + 122;
            String[] quickIds = {"clean_movement", "clouds", "fps", "ping", "cps", "coordinates"};
            int qCellW = (contentW - 44) / 3;
            for (int i = 0; i < quickIds.length; i++) {
                Module m = mm.getById(quickIds[i]);
                if (m == null) continue;
                int qx = contentX + 14 + (i % 3) * (qCellW + 8);
                int qy = row3Y + 26 + (i / 3) * 30;
                if (GlassRenderer.isHovered(mouseX, mouseY, qx, qy, qCellW, 24)) {
                    m.toggle();
                    return true;
                }
            }

            int row4Y = row3Y + 98;
            int bW = (contentW - 20) / 3;
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + 12, row4Y + 55, 68, 20)) {
                client.saveConfig();
                client.playUiClick(1.1f);
                return true;
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + bW + 22, row4Y + 55, 56, 20)) {
                this.activeTab = NavTab.FRIENDS;
                client.playUiClick(1.05f);
                return true;
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + (bW + 10) * 2 + 12, row4Y + 55, 76, 20)) {
                MinecraftClient.getInstance().setScreen(new HudEditorScreen(this));
                client.playUiClick(1.15f);
                return true;
            }
        }

        if (!searchQuery.isBlank() || activeTab.category != null) {
            List<Module> list = !searchQuery.isBlank() ? mm.search(searchQuery) : mm.getByCategory(activeTab.category);
            int curY = bodyY + 20;
            for (Module m : list) {
                float exp = m.getExpandAnim().get();
                int settingsRows = m.getSettings().size() + 1;
                int cardH = 48 + Math.round(exp * (settingsRows * 26 + 10));

                if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 68, curY + 10, 42, 28)) {
                    m.toggle();
                    return true;
                }
                if (GlassRenderer.isHovered(mouseX, mouseY, contentX, curY, contentW, 48)) {
                    m.setExpanded(!m.isExpanded());
                    client.playUiClick(1.0f);
                    return true;
                }

                if (m.isExpanded()) {
                    int setY = curY + 52;
                    for (Setting<?> s : m.getSettings()) {
                        if (s instanceof Setting.ModeSetting ms) {
                            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 140, setY, 124, 22)) {
                                ms.cycle(button == 1 ? -1 : 1);
                                client.playUiClick(1.05f);
                                client.saveConfig();
                                return true;
                            }
                        } else if (s instanceof Setting.SliderSetting ss) {
                            int barW = 140;
                            int barX = contentX + contentW - 16 - barW - 44;
                            if (GlassRenderer.isHovered(mouseX, mouseY, barX - 6, setY, barW + 12, 22)) {
                                this.draggingSlider = ss;
                                this.draggingSliderX = barX;
                                this.draggingSliderW = barW;
                                ss.setNormalized((float) (mouseX - barX) / (float) barW);
                                client.saveConfig();
                                return true;
                            }
                        } else if (s instanceof Setting.BoolSetting bs) {
                            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 56, setY, 40, 22)) {
                                bs.set(!bs.get());
                                client.playUiClick(1.05f);
                                client.saveConfig();
                                return true;
                            }
                        } else if (s instanceof Setting.ColorSetting cs) {
                            int[] presets = cs.getPresets();
                            int startX = contentX + contentW - 16 - presets.length * 18;
                            for (int i = 0; i < presets.length; i++) {
                                if (GlassRenderer.isHovered(mouseX, mouseY, startX + i * 18, setY + 2, 16, 18)) {
                                    cs.set(presets[i]);
                                    client.playUiClick(1.1f);
                                    client.saveConfig();
                                    return true;
                                }
                            }
                        }
                        setY += 26;
                    }
                    if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 90, setY, 74, 22)) {
                        this.bindingModule = m;
                        client.playUiClick(1.0f);
                        return true;
                    }
                }
                curY += cardH + 8;
            }
        }

        if (searchQuery.isBlank() && activeTab == NavTab.SETTINGS) {
            AccentColor[] colors = AccentColor.values();
            int swatchStartX = contentX + contentW - 16 - colors.length * 22;
            for (int i = 0; i < colors.length; i++) {
                if (GlassRenderer.isHovered(mouseX, mouseY, swatchStartX + i * 22, bodyY + 34, 18, 20)) {
                    cfg.accentColor = colors[i];
                    client.playUiClick(1.15f);
                    client.saveConfig();
                    return true;
                }
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 120, bodyY + 66, 104, 22)) {
                UiSize[] sizes = UiSize.values();
                cfg.uiSize = sizes[(cfg.uiSize.ordinal() + 1) % sizes.length];
                client.playUiClick(1.05f);
                client.saveConfig();
                return true;
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 52, bodyY + 98, 36, 20)) {
                cfg.backgroundBlur = !cfg.backgroundBlur;
                client.playUiClick(1.0f);
                client.saveConfig();
                return true;
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 52, bodyY + 130, 36, 20)) {
                cfg.uiSounds = !cfg.uiSounds;
                client.playUiClick(1.0f);
                client.saveConfig();
                return true;
            }
            int volBarW = 140;
            int volBarX = contentX + contentW - 16 - volBarW - 40;
            if (GlassRenderer.isHovered(mouseX, mouseY, volBarX - 4, bodyY + 162, volBarW + 8, 18)) {
                this.draggingVolume = true;
                this.volSliderX = volBarX;
                this.volSliderW = volBarW;
                cfg.uiSoundVolume = Math.max(0f, Math.min(1f, (float) (mouseX - volBarX) / volBarW));
                client.playUiClick(1.0f);
                client.saveConfig();
                return true;
            }
            if (GlassRenderer.isHovered(mouseX, mouseY, contentX + contentW - 96, bodyY + 192, 80, 22)) {
                this.bindingGuiKey = true;
                client.playUiClick(1.0f);
                return true;
            }

            int outroY = bodyY + 242;
            int dotsTotalW = colors.length * 20 - 6;
            int dotsX = contentX + (contentW - dotsTotalW) / 2;
            int dotsY = outroY + 98;
            for (int i = 0; i < colors.length; i++) {
                if (GlassRenderer.isHovered(mouseX, mouseY, dotsX + i * 20 - 2, dotsY - 2, 18, 18)) {
                    cfg.accentColor = colors[i];
                    client.playUiClick(1.2f);
                    client.saveConfig();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingSlider != null) {
            this.draggingSlider = null;
            EngsplayClient.getInstance().saveConfig();
        }
        if (this.draggingVolume) {
            this.draggingVolume = false;
            EngsplayClient.getInstance().saveConfig();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.targetScroll = Math.max(0f, this.targetScroll - (float) (verticalAmount * 24.0));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();

        if (bindingGuiKey) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                cfg.guiKeybind = keyCode;
                client.saveConfig();
            }
            bindingGuiKey = false;
            return true;
        }

        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingModule.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                bindingModule.setKeybind(keyCode);
            }
            client.saveConfig();
            bindingModule = null;
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                targetScroll = 0f;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == cfg.guiKeybind) {
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && chr >= 32 && chr != 127) {
            searchQuery += chr;
            targetScroll = 0f;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void drawMiniStatBadge(DrawContext ctx, int x, int y, String text, int textColor) {
        int w = this.textRenderer.getWidth(text) + 12;
        GlassRenderer.fillRoundedRect(ctx, x, y, w, 16, 7, 0x66100E10);
        ctx.drawText(this.textRenderer, text, x + 6, y + 4, textColor, false);
    }

    private void drawProfileStatCard(DrawContext ctx, int x, int y, int w, int h, String label, String value, int valColor) {
        GlassRenderer.drawGlassCard(ctx, x, y, w, h, 12, GlassRenderer.CARD_BG, GlassRenderer.BORDER_SUBTLE);
        ctx.drawText(this.textRenderer, label, x + 12, y + 12, GlassRenderer.TEXT_MUTED, false);
        ctx.drawTextWithShadow(this.textRenderer, value, x + 12, y + 32, valColor);
    }

    private void drawActionButton(DrawContext ctx, int x, int y, int w, int h, String label, int accent, int mx, int my) {
        boolean hover = GlassRenderer.isHovered(mx, my, x, y, w, h);
        GlassRenderer.fillRoundedRect(ctx, x, y, w, h, 7, hover ? accent : GlassRenderer.lerpColor(0xFF242024, accent, 0.25f));
        GlassRenderer.drawRoundedOutline(ctx, x, y, w, h, 7, hover ? 0x55FFFFFF : GlassRenderer.BORDER_SUBTLE);
        int lw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, x + (w - lw) / 2, y + (h - this.textRenderer.fontHeight) / 2 + 1, 0xFFFFFFFF, false);
    }

    private void drawProgressRing(DrawContext ctx, int cx, int cy, int radius, int thickness, float progress, int accent) {
        int steps = 48;
        for (int i = 0; i < steps; i++) {
            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i) / steps;
            int px = cx + (int) Math.round(Math.cos(angle) * radius);
            int py = cy + (int) Math.round(Math.sin(angle) * radius);
            boolean active = (i / (float) steps) <= progress;
            ctx.fill(px - thickness / 2, py - thickness / 2, px + thickness / 2 + 1, py + thickness / 2 + 1,
                    active ? accent : 0x26FFFFFF);
        }
    }

    private void drawThickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) + 1;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = Math.round(x1 + (x2 - x1) * t);
            int py = Math.round(y1 + (y2 - y1) * t);
            ctx.fill(px, py, px + 2, py + 2, color);
        }
    }

    private String getPlayerName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) return mc.player.getName().getString();
        return mc.getSession().getUsername();
    }

    private String getTimeGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }

    private String getKeyName(int key) {
        if (key <= 0 || key == GLFW.GLFW_KEY_UNKNOWN) return "None";
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "Right Shift";
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "Left Shift";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase(Locale.ROOT);
        return "Key " + key;
    }
}
