package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The Aperture [空窍] attachment: a mortal has none, and awakening [开窍] is what puts one here. Immutable
 * record attachment keyed {@code aperture_data}, synced {@code OWNER_ONLY}, written only by {@code
 * ApertureService}; holds up to {@code MAX_APERTURES}
 * (2) {@link Aperture} entries -- an empty list IS the "no aperture at all" state.
 *
 * <p>List order is the awakening order, and the FIRST aperture is whatever is not flagged
 * {@code second}: a Second Aperture Gu may open before Hope Gu ever does, so the only reader of "the
 * primary" is {@link #getPrimary}, which falls back to a lone second aperture. {@code isAwakened} means
 * "has the FIRST aperture" -- Hope Gu, the refinement gate and the command gates all read it.
 *
 * <p>A position moves when Hope Gu inserts the first aperture ahead of a lone second one; a kind
 * ({@code PRIMARY} the first, {@code SECOND} the second) never does. Anything that outlives the insert --
 * a Vital Gu's binding -- names its aperture by kind, and {@link #indexOf} / {@link #getKindAt} convert.
 *
 * <p>⚠ {@code with(index, aperture)} REFUSES to grow the list -- only {@code withOpened}/{@code insertFirst}
 * append -- the mirror of {@link net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage#with}, which GROWS
 * to reach its index; deliberately opposite. ⚠ {@code MAX_APERTURES} caps the {@code encode}/{@code decode}
 * list too: longer is silently truncated on sync. ⚠ Decoding heals old saves: any entry at list position 1 is
 * marked {@code second}, because pre-flag saves implied it by position alone.
 *
 * @author Alex
 * @version 1.0.0
 * @see Aperture
 * @see ApertureService
 * @since 1.0.0
 */

public record ApertureData(List<Aperture> apertures) {

    public static final int MAX_APERTURES = 2;
    public static final int PRIMARY = 0;
    public static final int SECOND = 1;
    public static final ApertureData DEFAULT = new ApertureData(List.of());
    public static final Codec<ApertureData> CODEC = Aperture.CODEC.listOf()
            .xmap(ApertureData::heal, ApertureData::apertures);

    public ApertureData {
        apertures = apertures.size() <= MAX_APERTURES
                ? List.copyOf(apertures)
                : List.copyOf(apertures.subList(0, MAX_APERTURES));
    }

    public static @NotNull ApertureData decode(@NotNull FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size > MAX_APERTURES) throw new DecoderException("Value too big (" + size + " > " + MAX_APERTURES + ")");

        List<Aperture> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(Aperture.decode(buf));
        return new ApertureData(list);
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull ApertureData value) {
        List<Aperture> list = value.apertures();
        if (list.size() > MAX_APERTURES) {
            throw new EncoderException("Value too big (" + list.size() + " > " + MAX_APERTURES + ")");
        }

        buf.writeVarInt(list.size());
        for (Aperture aperture : list) Aperture.encode(buf, aperture);
    }

    public Aperture get(int i) { return i >= 0 && i < apertures.size() ? apertures.get(i) : Aperture.NONE; }

    public Aperture getPrimary() { return get(PRIMARY); }

    public int count() { return apertures.size(); }

    public boolean hasAperture() { return !apertures.isEmpty(); }

    public boolean isAwakened() { return getFirstIndex() >= 0; }

    public boolean isFull() { return apertures.size() >= MAX_APERTURES; }

    public int getFirstIndex() {
        for (int i = 0; i < apertures.size(); i++) if (!apertures.get(i).second()) return i;
        return -1;
    }

    public int getSecondIndex() {
        for (int i = 0; i < apertures.size(); i++) if (apertures.get(i).second()) return i;
        return -1;
    }

    public int indexOf(int kind) { return kind == SECOND ? getSecondIndex() : getFirstIndex(); }

    public int getKindAt(int index) { return get(index).second() ? SECOND : PRIMARY; }

    public ApertureData withOpened(Aperture aperture) {
        if (isFull()) return this;

        List<Aperture> next = new ArrayList<>(apertures);
        next.add(aperture);
        return new ApertureData(next);
    }

    public ApertureData insertFirst(Aperture aperture) {
        if (isFull() || getFirstIndex() >= 0) return withOpened(aperture);

        List<Aperture> next = new ArrayList<>(apertures);
        next.add(0, aperture);
        return new ApertureData(next);
    }

    public ApertureData with(int index, Aperture aperture) {
        if (index < 0 || index >= apertures.size()) return this;

        List<Aperture> next = new ArrayList<>(apertures);
        next.set(index, aperture);
        return new ApertureData(next);
    }

    private static ApertureData heal(List<Aperture> list) {
        List<Aperture> fixed = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Aperture aperture = list.get(i);
            fixed.add(i >= 1 && !aperture.second() ? aperture.withSecond(true) : aperture);
        }
        return new ApertureData(fixed);
    }
}
