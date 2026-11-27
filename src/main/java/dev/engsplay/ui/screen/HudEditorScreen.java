package dev.engsplay.ui.screen;

import dev.engsplay.EngsplayClient;
import dev.engsplay.config.ClientConfig;
import dev.engsplay.ui.hud.EngsplayHudRenderer;
import dev.engsplay.ui.render.GlassRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private String draggingId = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x4409080A);
        EngsplayClient client = EngsplayClient.getInstance();
        client.getHudRenderer().render(ctx, delta, true);

        String banner = "HUD Drag Mode • Drag elements • ESC to return";
        int bw = this.textRenderer.getWidth(banner) + 20;
        int bx = (this.width - bw) / 2;
        GlassRenderer.drawGlassCard(ctx, bx, 10, bw, 22, 10, GlassRenderer.PANEL_BG, client.getConfig().accentColor.getArgb());
        ctx.drawTextWithShadow(this.textRenderer, banner, bx + 10, 17, GlassRenderer.TEXT_PRIMARY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (EngsplayHudRenderer.HudBox box : EngsplayClient.getInstance().getHudRenderer().getLastBoxes().values()) {
                if (GlassRenderer.isHovered(mouseX, mouseY, box.x, box.y, box.w, box.h)) {
                    this.draggingId = box.id;
                    this.dragOffsetX = (int) mouseX - box.x;
                    this.dragOffsetY = (int) mouseY - box.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingId != null && button == 0) {
            ClientConfig cfg = EngsplayClient.getInstance().getConfig();
            int newX = Math.max(2, Math.min(this.width - 10, (int) mouseX - dragOffsetX));
            int newY = Math.max(2, Math.min(this.height - 10, (int) mouseY - dragOffsetY));
            cfg.hudPositions.put(this.draggingId, new float[]{
                    (float) newX / Math.max(1, this.width),
                    (float) newY / Math.max(1, this.height)
            });
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingId != null) {
            this.draggingId = null;
            EngsplayClient.getInstance().saveConfig();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
