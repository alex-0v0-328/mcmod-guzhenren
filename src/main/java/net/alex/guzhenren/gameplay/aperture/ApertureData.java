package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The Aperture [空窍] attachment: a mortal has none, and awakening [开窍] is what puts one here. Immutable
 * record attachment keyed {@code aperture_data}, synced {@code OWNER_ONLY}, written only by {@link
 * ApertureService}; holds up to {@code MAX_APERTURES}
 * (2) {@link Aperture} entries -- an empty list IS the "no aperture at all" state.
 *
 * <p>List order is the awakening order, and the FIRST aperture is whatever is not flagged
 * {@code second}: a Second Aperture Gu may open before Hope Gu ever does, so the only reader of "the
 * primary" is {@link #primary}, which falls back to a lone second aperture. {@code isAwakened} means
 * "has the FIRST aperture" -- Hope Gu, the refinement gate and the command gates all read it.
 *
 * <p>⚠ {@code with(index, aperture)} REFUSES to grow the list -- only {@code opened}/{@code insertFirst}
 * append -- the mirror of {@link ApertureStorage#with}, which GROWS to reach its index; deliberately
 * opposite. ⚠ {@code MAX_APERTURES} caps the {@code STREAM_CODEC} list too: longer is silently
 * truncated on sync. ⚠ Decoding heals old saves: any entry at list position 1 is marked {@code second},
 * because pre-flag saves implied it by position alone.
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
            .xmap(ApertureData::healed, ApertureData::apertures);
    public static final StreamCodec<ByteBuf, ApertureData> STREAM_CODEC =
            Aperture.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_APERTURES))
                    .map(ApertureData::new, ApertureData::apertures);

    public ApertureData {
        apertures = apertures.size() <= MAX_APERTURES
                ? List.copyOf(apertures)
                : List.copyOf(apertures.subList(0, MAX_APERTURES));
    }

    public Aperture get(int i) { return i >= 0 && i < apertures.size() ? apertures.get(i) : Aperture.NONE; }

    public Aperture primary() { return get(PRIMARY); }

    public int count() { return apertures.size(); }

    public boolean hasAperture() { return !apertures.isEmpty(); }

    public boolean isAwakened() { return firstIndex() >= 0; }

    public boolean isFull() { return apertures.size() >= MAX_APERTURES; }

    public int firstIndex() {
        for (int i = 0; i < apertures.size(); i++) if (!apertures.get(i).second()) return i;
        return -1;
    }

    public int secondIndex() {
        for (int i = 0; i < apertures.size(); i++) if (apertures.get(i).second()) return i;
        return -1;
    }

    public ApertureData opened(Aperture aperture) {
        if (isFull()) return this;

        List<Aperture> next = new ArrayList<>(apertures);
        next.add(aperture);
        return new ApertureData(next);
    }

    public ApertureData insertFirst(Aperture aperture) {
        if (isFull() || firstIndex() >= 0) return opened(aperture);

        List<Aperture> next = new ArrayList<>(apertures);
        next.addFirst(aperture);
        return new ApertureData(next);
    }

    public ApertureData with(int index, Aperture aperture) {
        if (index < 0 || index >= apertures.size()) return this;

        List<Aperture> next = new ArrayList<>(apertures);
        next.set(index, aperture);
        return new ApertureData(next);
    }

    private static ApertureData healed(List<Aperture> list) {
        List<Aperture> fixed = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Aperture aperture = list.get(i);
            fixed.add(i >= 1 && !aperture.second() ? aperture.withSecond(true) : aperture);
        }
        return new ApertureData(fixed);
    }
}
