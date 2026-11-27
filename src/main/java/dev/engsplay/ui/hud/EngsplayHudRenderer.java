package dev.engsplay.ui.hud;

import dev.engsplay.EngsplayClient;
import dev.engsplay.config.ClientConfig;
import dev.engsplay.module.ModuleManager;
import dev.engsplay.stats.SessionStatsTracker;
import dev.engsplay.ui.render.GlassRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class EngsplayHudRenderer {
    public static class HudBox {
        public final String id;
        public int x;
        public int y;
        public int w;
        public int h;

        public HudBox(String id, int x, int y, int w, int h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final Map<String, HudBox> lastBoxes = new LinkedHashMap<>();

    public Map<String, HudBox> getLastBoxes() {
        return lastBoxes;
    }

    public void render(DrawContext ctx, float tickDelta, boolean editorMode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        EngsplayClient client = EngsplayClient.getInstance();
        ClientConfig cfg = client.getConfig();
        ModuleManager mm = client.getModuleManager();
        SessionStatsTracker stats = client.getStatsTracker();
        TextRenderer tr = mc.textRenderer;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int accent = cfg.accentColor.getArgb();

        lastBoxes.clear();

        if (editorMode || mm.isEnabled("fps")) {
            int fps = Math.max(1, mc.getCurrentFps());
            String text = fps + " FPS";
            int pw = tr.getWidth(text) + 14;
            int ph = 18;
            int[] pos = resolvePos(cfg, "fps", sw, sh, pw, ph);
            drawPill(ctx, tr, pos[0], pos[1], pw, ph, text, accent, editorMode);
            lastBoxes.put("fps", new HudBox("fps", pos[0], pos[1], pw, ph));
        }

        if (editorMode || mm.isEnabled("ping")) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            String text = ping + " ms";
            int pw = tr.getWidth(text) + 14;
            int ph = 18;
            int[] pos = resolvePos(cfg, "ping", sw, sh, pw, ph);
            drawPill(ctx, tr, pos[0], pos[1], pw, ph, text, accent, editorMode);
            lastBoxes.put("ping", new HudBox("ping", pos[0], pos[1], pw, ph));
        }

        if (editorMode || mm.isEnabled("cps")) {
            String text = stats.getLeftCps() + " | " + stats.getRightCps() + " CPS";
            int pw = tr.getWidth(text) + 14;
            int ph = 18;
            int[] pos = resolvePos(cfg, "cps", sw, sh, pw, ph);
            drawPill(ctx, tr, pos[0], pos[1], pw, ph, text, accent, editorMode);
            lastBoxes.put("cps", new HudBox("cps", pos[0], pos[1], pw, ph));
        }

        if (editorMode || mm.isEnabled("coordinates")) {
            int x = (int) Math.floor(mc.player.getX());
            int y = (int) Math.floor(mc.player.getY());
            int z = (int) Math.floor(mc.player.getZ());
            Direction dir = mc.player.getHorizontalFacing();
            String dirLetter = switch (dir) {
                case NORTH -> "N";
                case SOUTH -> "S";
                case EAST -> "E";
                case WEST -> "W";
                default -> "";
            };
            String text = "X: " + x + " Y: " + y + " Z: " + z + " " + dirLetter;
            int pw = tr.getWidth(text) + 14;
            int ph = 18;
            int[] pos = resolvePos(cfg, "coords", sw, sh, pw, ph);
            drawPill(ctx, tr, pos[0], pos[1], pw, ph, text, accent, editorMode);
            lastBoxes.put("coords", new HudBox("coords", pos[0], pos[1], pw, ph));
        }

        if (editorMode || mm.isEnabled("clock")) {
            String text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            int pw = tr.getWidth(text) + 14;
            int ph = 18;
            int[] pos = resolvePos(cfg, "clock", sw, sh, pw, ph);
            drawPill(ctx, tr, pos[0], pos[1], pw, ph, text, accent, editorMode);
            lastBoxes.put("clock", new HudBox("clock", pos[0], pos[1], pw, ph));
        }

        if (editorMode || mm.isEnabled("keystrokes")) {
            int[] pos = resolvePos(cfg, "keystrokes", sw, sh, 82, 58);
            int kx = pos[0];
            int ky = pos[1];
            long handle = mc.getWindow().getHandle();

            boolean wDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
            boolean aDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
            boolean sDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
            boolean dDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
            boolean lmbDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmbDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            drawKeyBox(ctx, tr, kx + 29, ky, 22, 17, "W", wDown, accent);
            drawKeyBox(ctx, tr, kx + 5, ky + 19, 22, 17, "A", aDown, accent);
            drawKeyBox(ctx, tr, kx + 29, ky + 19, 22, 17, "S", sDown, accent);
            drawKeyBox(ctx, tr, kx + 53, ky + 19, 22, 17, "D", dDown, accent);
            drawKeyBox(ctx, tr, kx + 5, ky + 38, 34, 16, "LMB", lmbDown, accent);
            drawKeyBox(ctx, tr, kx + 41, ky + 38, 34, 16, "RMB", rmbDown, accent);

            if (editorMode) {
                GlassRenderer.drawRoundedOutline(ctx, kx, ky, 80, 56, 8, accent);
            }
            lastBoxes.put("keystrokes", new HudBox("keystrokes", kx, ky, 80, 56));
        }
    }

    private void drawPill(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String text, int accent, boolean editorMode) {
        GlassRenderer.drawGlassCard(ctx, x, y, w, h, 9, GlassRenderer.PILL_BG,
                editorMode ? accent : GlassRenderer.BORDER_SUBTLE);
        ctx.drawTextWithShadow(tr, text, x + 7, y + (h - tr.fontHeight) / 2 + 1, GlassRenderer.TEXT_PRIMARY);
    }

    private void drawKeyBox(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean pressed, int accent) {
        int bg = pressed ? accent : GlassRenderer.PILL_BG;
        GlassRenderer.drawGlassCard(ctx, x, y, w, h, 5, bg, GlassRenderer.BORDER_SUBTLE);
        int lw = tr.getWidth(label);
        ctx.drawTextWithShadow(tr, label, x + (w - lw) / 2, y + (h - tr.fontHeight) / 2 + 1,
                pressed ? 0xFFFFFFFF : GlassRenderer.TEXT_PRIMARY);
    }

    private int[] resolvePos(ClientConfig cfg, String id, int sw, int sh, int w, int h) {
        float[] norm = cfg.hudPositions.getOrDefault(id, new float[]{0.02f, 0.02f});
        int x = Math.max(2, Math.min(sw - w - 2, Math.round(norm[0] * sw)));
        int y = Math.max(2, Math.min(sh - h - 2, Math.round(norm[1] * sh)));
        return new int[]{x, y};
    }
}
