package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家已掌握与激活的杀招[Killing Move]状态附件（键 {@code killing_move_data}，仅同步玩家本人供扇形选择器展示）。
 * <ul>
 *   <li>仅存储杀招字符串 ID（具体杀招表位于上层 {@code item} 包，{@code gameplay} 包不直接引用），紧凑构造器自动去重并
 *       排序；未开启 {@code keepInventory} 死亡时随继承附件一并清空。</li>
 *   <li>{@link #storeBacked} 记录配合蛊已炼化且存于空窍存储中的杀招 ID：因空窍存储不同步给客户端，由服务端
 *       {@code KillingMoveService.refreshStoreBacked} 重算后随本附件同步，客户端选择器再结合可见背包判断可用性；该列表
 *       仅同步不落盘，读档或重生后为空直至首次重算。</li>
 *   <li>{@link #active} 记录当前开启的持续型杀招 ID（未开启时为 {@link #NONE}，开启期间主蛊每次挥击均算一次杀招施展）：
 *       仅同步不落盘，重登或死亡克隆时自动关闭。</li>
 * </ul>
 */

public record KillingMoveData(List<String> learned, List<String> storeBacked, String active) {

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

    public KillingMoveData withActive(@NotNull String id) { return new KillingMoveData(learned, storeBacked, id); }
}
