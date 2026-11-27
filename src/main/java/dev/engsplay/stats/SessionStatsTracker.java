package dev.engsplay.stats;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

public class SessionStatsTracker {
    private final long sessionStartMs = System.currentTimeMillis();
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private final float[] fpsHistory = new float[48];
    private int fpsIndex = 0;
    private long lastSampleMs = 0;

    public SessionStatsTracker() {
        for (int i = 0; i < fpsHistory.length; i++) {
            fpsHistory[i] = 60f;
        }
    }

    public void recordLeftClick() {
        leftClicks.addLast(System.currentTimeMillis());
    }

    public void recordRightClick() {
        rightClicks.addLast(System.currentTimeMillis());
    }

    public int getLeftCps() {
        prune(leftClicks);
        return leftClicks.size();
    }

    public int getRightCps() {
        prune(rightClicks);
        return rightClicks.size();
    }

    private void prune(Deque<Long> q) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!q.isEmpty() && q.peekFirst() < cutoff) {
            q.removeFirst();
        }
    }

    public void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastSampleMs >= 250L) {
            lastSampleMs = now;
            int fps = client.getCurrentFps();
            fpsHistory[fpsIndex] = fps > 0 ? fps : 60f;
            fpsIndex = (fpsIndex + 1) % fpsHistory.length;
        }
    }

    public float[] getFpsHistoryOrdered() {
        float[] ordered = new float[fpsHistory.length];
        for (int i = 0; i < fpsHistory.length; i++) {
            ordered[i] = fpsHistory[(fpsIndex + i) % fpsHistory.length];
        }
        return ordered;
    }

    public long getSessionElapsedSeconds() {
        return Math.max(1L, (System.currentTimeMillis() - sessionStartMs) / 1000L);
    }

    public String formatSessionShort() {
        long s = getSessionElapsedSeconds();
        if (s < 60) return s + "s";
        long m = s / 60;
        if (m < 60) return m + "m";
        return (m / 60) + "h " + (m % 60) + "m";
    }

    public String formatDuration(long totalSeconds) {
        long m = totalSeconds / 60;
        if (m < 60) return m + "m";
        return (m / 60) + "h " + (m % 60) + "m";
    }
}
