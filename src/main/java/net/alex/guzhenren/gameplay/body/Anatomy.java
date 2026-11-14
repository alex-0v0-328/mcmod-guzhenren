package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenren.core.ModByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 肉身的部位状态，{@link BodyData} 的一格：六肢体与七窍的伤势、长出的附加部位。
 * <ul>
 *   <li>筋骨皮肉的改造档仍是全身一档，存在 {@link BodyData} 自己的四格，不按肢体分。</li>
 *   <li>两张伤势表只存受伤的条目，读不到即 {@link Injury#NONE}；构造器丢掉 {@code NONE}，相等比较不受写法影响。</li>
 *   <li>⚠ 网络按枚举顺序逐个写全部肢体和七窍，{@code encode}/{@code decode} 顺序须一致。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BodyPart
 * @see Organ
 * @since 1.0.0
 */

public record Anatomy(Map<BodyPart, Injury> limbInjuries, Map<Organ, Injury> organInjuries,
                      Set<Appendage> appendages) {

    public static final Anatomy DEFAULT = new Anatomy(Map.of(), Map.of(), Set.of());
    public static final Codec<Anatomy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(BodyPart.CODEC, Injury.CODEC).optionalFieldOf("limb_injuries", Map.of())
                    .forGetter(Anatomy::limbInjuries),
            Codec.unboundedMap(Organ.CODEC, Injury.CODEC).optionalFieldOf("organ_injuries", Map.of())
                    .forGetter(Anatomy::organInjuries),
            Appendage.CODEC.listOf().<Set<Appendage>>xmap(Set::copyOf, ArrayList::new)
                    .optionalFieldOf("appendages", Set.of()).forGetter(Anatomy::appendages)
    ).apply(instance, Anatomy::new));

    public Anatomy {
        limbInjuries = getInjured(BodyPart.class, limbInjuries);
        organInjuries = getInjured(Organ.class, organInjuries);
        appendages = Collections.unmodifiableSet(toEnumSet(appendages));
    }

    private static <K extends Enum<K>> Map<K, Injury> getInjured(Class<K> type, Map<K, Injury> injuries) {
        EnumMap<K, Injury> injured = new EnumMap<>(type);
        injuries.forEach((key, injury) -> {
            if (injury.isInjured()) injured.put(key, injury);
        });
        return Collections.unmodifiableMap(injured);
    }

    private static EnumSet<Appendage> toEnumSet(Collection<Appendage> appendages) {
        EnumSet<Appendage> set = EnumSet.noneOf(Appendage.class);
        set.addAll(appendages);
        return set;
    }

    public static @NotNull Anatomy decode(@NotNull FriendlyByteBuf buf) {
        Map<BodyPart, Injury> limbs = new EnumMap<>(BodyPart.class);
        for (BodyPart part : BodyPart.values()) limbs.put(part, ModByteBufs.readEnum(buf, Injury.class));
        Map<Organ, Injury> organs = new EnumMap<>(Organ.class);
        for (Organ organ : Organ.values()) organs.put(organ, ModByteBufs.readEnum(buf, Injury.class));
        return new Anatomy(limbs, organs, ModByteBufs.readEnumSet(buf, Appendage.class));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull Anatomy value) {
        for (BodyPart part : BodyPart.values()) ModByteBufs.writeEnum(buf, value.getInjury(part));
        for (Organ organ : Organ.values()) ModByteBufs.writeEnum(buf, value.getInjury(organ));
        ModByteBufs.writeEnumSet(buf, value.appendages());
    }

    public @NotNull Injury getInjury(@NotNull BodyPart part) { return limbInjuries.getOrDefault(part, Injury.NONE); }

    public @NotNull Injury getInjury(@NotNull Organ organ) { return organInjuries.getOrDefault(organ, Injury.NONE); }

    public boolean hasAppendage(@NotNull Appendage appendage) { return appendages.contains(appendage); }

    public boolean isUninjured() { return limbInjuries.isEmpty() && organInjuries.isEmpty(); }

    public @NotNull Anatomy withInjury(@NotNull BodyPart part, @NotNull Injury injury) {
        Map<BodyPart, Injury> limbs = new EnumMap<>(BodyPart.class);
        limbs.putAll(limbInjuries);
        limbs.put(part, injury);
        return new Anatomy(limbs, organInjuries, appendages);
    }

    public @NotNull Anatomy withInjury(@NotNull Organ organ, @NotNull Injury injury) {
        Map<Organ, Injury> organs = new EnumMap<>(Organ.class);
        organs.putAll(organInjuries);
        organs.put(organ, injury);
        return new Anatomy(limbInjuries, organs, appendages);
    }

    public @NotNull Anatomy withAppendage(@NotNull Appendage appendage, boolean present) {
        EnumSet<Appendage> next = toEnumSet(appendages);
        if (present) next.add(appendage);
        else next.remove(appendage);
        return new Anatomy(limbInjuries, organInjuries, next);
    }

    public @NotNull Anatomy withoutInjuries() { return new Anatomy(Map.of(), Map.of(), appendages); }
}
