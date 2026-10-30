package net.alex.guzhenren.gameplay.body;

/**
 * The invulnerability frames a shield [护盾] last met: the largest hit it saw inside them and the tick they close.
 * Scratch attachment keyed {@code shield_window}, neither saved nor synced; {@code ShieldService} is the only writer.
 *
 * <p>Vanilla's own {@code lastHurt} cannot stand in: a hit the shield takes whole is cancelled and leaves it behind,
 * and a hit that breaks the shield leaves only what passed. Frames this record did not see read {@code lastHurt}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public record ShieldWindow(float lastHit, long endTick) {

    public static final ShieldWindow CLOSED = new ShieldWindow(0.0F, BodyData.UNTRACKED);

    public boolean isOpen(long now) { return now < endTick; }
}
