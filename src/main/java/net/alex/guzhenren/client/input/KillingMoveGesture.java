package net.alex.guzhenren.client.input;

import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import org.jetbrains.annotations.NotNull;

/**
 * What one hold of a moon blade Gu's [月刃] use key sends, kept free of game state so plain JUnit covers it: the
 * radial menu's calls in, the payload's move id out.
 *
 * <p>A release before the ring opens (a tap) sends {@link KillingMoveService#PLAIN}, the plain blade. A hold the menu
 * found no usable killing move [杀招] for ({@link #empty}) sends nothing, although the menu reports its release
 * through the same {@link #releaseUnopened}. A pick sends the id {@link #list} put at that sector.
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveInput
 * @since 1.0.0
 */

public final class KillingMoveGesture {

    private final Consumer<String> send;
    private List<String> listed = List.of();
    private boolean emptied;

    public KillingMoveGesture(@NotNull Consumer<String> send) {
        this.send = send;
    }

    public void list(@NotNull List<String> ids) { listed = List.copyOf(ids); }

    public void empty() { emptied = true; }

    public void pick(int index) { send.accept(listed.get(index)); }

    public void releaseUnopened() {
        if (!emptied) send.accept(KillingMoveService.PLAIN);
    }
}
