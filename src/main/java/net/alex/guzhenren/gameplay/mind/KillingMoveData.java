package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The killing moves [杀招] a player has learned [学会], held in the mind [脑海] beside {@link MindData}: immutable
 * record attachment keyed {@code killing_move_data}, synced to its owner so the radial menu lists only what they
 * know. A death without keepInventory resets it with every inherited attachment, so dying forgets them all and
 * keepInventory keeps them.
 *
 * <p>Each entry is a killing move's id as a string: the table is the item side's {@code KillingMove}, which gameplay
 * never imports, and an id no row names any more is simply never asked for. The compact constructor keeps the list
 * distinct and sorted, so two records that know the same moves compare equal.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record KillingMoveData(List<String> learned) {

    public static final KillingMoveData DEFAULT = new KillingMoveData(List.of());
    public static final Codec<KillingMoveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(KillingMoveData::learned)
    ).apply(instance, KillingMoveData::new));

    public KillingMoveData {
        learned = learned.stream().distinct().sorted().toList();
    }

    public static @NotNull KillingMoveData decode(@NotNull FriendlyByteBuf buf) {
        return new KillingMoveData(buf.readList(FriendlyByteBuf::readUtf));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull KillingMoveData value) {
        buf.writeCollection(value.learned(), FriendlyByteBuf::writeUtf);
    }

    public boolean knows(@NotNull String id) { return learned.contains(id); }

    public KillingMoveData withLearned(@NotNull String id) {
        return new KillingMoveData(Stream.concat(learned.stream(), Stream.of(id)).toList());
    }
}
