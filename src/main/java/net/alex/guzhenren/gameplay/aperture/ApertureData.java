package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍[Aperture]附件：凡人没有，开窍[Awakening]后才有。
 *
 * <ul>
 *   <li>键 {@code aperture_data}，同步 {@code OWNER_ONLY}，仅 {@code ApertureService} 写；空列表即无空窍。</li>
 *   <li>列表顺序即开窍顺序，第一空窍是未标 {@code second} 者：第二空窍蛊[Second Aperture Gu]可能先于希望
 *       蛊[Hope Gu]开，故「主空窍」只经 {@link #getPrimary} 读，仅剩孤立第二空窍时退回它。</li>
 *   <li>{@code isAwakened} 指有第一空窍，希望蛊、炼化门槛、命令门槛读它；{@code hasAperture} 指有任一空窍（含孤立第二空
 *       窍），HUD、信息面板、元石[Primeval Stone]、温养[Nourish]读它。</li>
 *   <li>希望蛊在孤立第二空窍前插入第一空窍时位置会变，种类（{@code PRIMARY} 第一、{@code SECOND} 第二）不变；本命蛊绑定
 *       等跨插入存续者按种类指认，经 {@link #indexOf} / {@link #getKindAt} 换算。</li>
 *   <li>⚠ {@code with(index, aperture)} 不扩长列表，只有 {@code withOpened} / {@code insertFirst} 追加，与会扩长的
 *       {@link net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage#with} 刻意相反。</li>
 *   <li>⚠ 至多 {@code MAX_APERTURES}（2）个：构造时静默截断超长部分，{@code encode} / {@code decode} 超长抛异常。</li>
 *   <li>⚠ 解码修复旧存档：位置 1 的条目一律标 {@code second}，因加标记前的存档只靠位置表示。</li>
 * </ul>
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
        next.addFirst(aperture);
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
