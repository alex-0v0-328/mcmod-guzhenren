package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家空窍列表附件（键 {@code aperture_data}，同步策略 {@code OWNER_ONLY}）：凡人为空列表，开窍[Awakening]或开启第
 * 二空窍后存入条目。
 * <ul>
 *   <li>运行时仅由 {@link ApertureService} 写入。</li>
 *   <li>列表按当前排位存放，未标 {@code second} 的条目为第一空窍：因第二空窍蛊[Second Aperture Gu]可先于
 *       希望蛊[Hope Gu]使用，「主空窍」一律经 {@link #getPrimary} 读取，仅有一个孤立第二空窍时退回该窍。</li>
 *   <li>{@link #isAwakened} 表示拥有第一空窍（希望蛊、炼化门槛与命令门槛检查此项）；{@link #hasAperture} 表示拥有任一
 *       空窍（含孤立第二空窍，HUD、信息面板、元石[Primeval Stone]与温养[Nourish]检查此项）。</li>
 *   <li>希望蛊在孤立第二空窍前插入第一空窍时，列表下标会后移，但空窍种类（{@link #PRIMARY}、{@link #SECOND}）不变；本命
 *       蛊绑定等跨插入存续的数据按种类标识，经 {@link #indexOf} 与 {@link #getKindAt} 换算下标。</li>
 *   <li>⚠ {@link #with(int, Aperture)} 不扩容列表，仅 {@link #withOpened} 与 {@link #insertFirst} 会新增条目（与自动
 *       扩容的 {@link net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage#with} 相反）。</li>
 *   <li>⚠ 最多容纳 {@link #MAX_APERTURES}（2）个空窍：构造器静默截断超长部分，{@link #encode} 与 {@link #decode} 遇到
 *       超长列表直接抛异常。</li>
 *   <li>⚠ {@code heal} 在反序列化时迁移旧存档：将下标 {@code 1} 的条目强制标为 {@code second}（旧存档仅靠位置区
 *       分）。</li>
 * </ul>
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
