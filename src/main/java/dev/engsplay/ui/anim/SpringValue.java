package dev.engsplay.ui.anim;

public class SpringValue {
    private float value;
    private float target;
    private float velocity;
    private final float stiffness;
    private final float damping;

    public SpringValue(float initial, float stiffness, float damping) {
        this.value = initial;
        this.target = initial;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void setTarget(float target) { this.target = target; }

    public void snap(float v) {
        this.value = v;
        this.target = v;
        this.velocity = 0f;
    }

    public float update(float dt) {
        float clampedDt = Math.min(dt, 0.05f);
        float force = (target - value) * stiffness;
        velocity = (velocity + force * clampedDt) * (float) Math.exp(-damping * clampedDt);
        value += velocity * clampedDt;
        if (Math.abs(target - value) < 0.0005f && Math.abs(velocity) < 0.0005f) {
            value = target;
            velocity = 0f;
        }
        return value;
    }

    public float get() { return value; }
    public float getTarget() { return target; }
}
