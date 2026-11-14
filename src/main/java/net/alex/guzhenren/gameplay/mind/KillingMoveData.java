package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 已学会的杀招[Killing Move]，与 {@link MindData} 并列存于脑海。
 *
 * <ul>
 *   <li>附件 {@code killing_move_data}，同步本人供扇形选择器列招；不开保留物品栏死即全忘（随继承附件重置）。</li>
 *   <li>存招 ID：招表在物品侧 {@code KillingMove}，gameplay 包不引用，已删的不再问；构造器两表去重排序，同招相等。</li>
 *   <li>{@link #storeBacked}：配合蛊已炼化、存于空窍[Aperture]的招，由 {@code KillingMoveService.refreshStoreBacked}
 *       在服务端重算（空窍存储不同步），选择器结合所见物品栏读；只同步不存档，读档或死亡后空到重算。</li>
 *   <li>{@link #active}：已启动的招，关掉前主蛊每道刃都算一次施展；同步，选择器显示为可停止；不存档，
 *       重登或死亡（{@code PlayerDataService.onClone}）即关。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record KillingMoveData(List<String> learned, List<String> storeBacked, String active) {

    /** 未启动任何招时的 {@link #active}。 */
    public static final String NONE = "";
    public static final KillingMoveData DEFAULT = new KillingMoveData(List.of(), List.of(), NONE);
    public static final Codec<KillingMoveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(KillingMoveData::learned)
    ).apply(instance, learned -> new KillingMoveData(learned, List.of(), NONE)));

    public KillingMoveData {
        learned = learned.stream().distinct().sorted().toList();
        storeBacked = storeBacked.stream().distinct().sorted().toList();
    }

    public static @NotNull KillingMoveData decode(@NotNull FriendlyByteBuf buf) {
        return new KillingMoveData(buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf),
                buf.readUtf());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull KillingMoveData value) {
        buf.writeCollection(value.learned(), FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.storeBacked(), FriendlyByteBuf::writeUtf);
        buf.writeUtf(value.active());
    }

    public boolean knows(@NotNull String id) { return learned.contains(id); }

    public boolean isStoreBacked(@NotNull String id) { return storeBacked.contains(id); }

    public boolean isActive(@NotNull String id) { return !id.equals(NONE) && active.equals(id); }

    public KillingMoveData withLearned(@NotNull String id) {
        return new KillingMoveData(Stream.concat(learned.stream(), Stream.of(id)).toList(), storeBacked, active);
    }

    public KillingMoveData withStoreBacked(@NotNull List<String> ids) {
        return new KillingMoveData(learned, ids, active);
    }

    /** 启动 {@code id} 后的记录；传 {@link #NONE} 则关掉所有招。 */
    public KillingMoveData withActive(@NotNull String id) { return new KillingMoveData(learned, storeBacked, id); }
}
