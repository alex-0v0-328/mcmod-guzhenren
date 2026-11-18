package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家脑海中已学会的蛊方（键 {@code gu_recipe_knowledge_data}，仅同步玩家本人供炼蛊台选择器筛选）。
 * <ul>
 *   <li>只存蛊方配方 ID 字符串（{@code gameplay} 包不引用配方类型），紧凑构造器去重并排序；未开 {@code keepInventory}
 *       死亡时随继承附件一并清空。</li>
 *   <li>数据包删掉的蛊方 ID 仍留在列表里，炼蛊台按服务器现有配方取交集，不主动清理。</li>
 * </ul>
 */

public record GuRecipeKnowledgeData(List<String> learned) {

    public static final GuRecipeKnowledgeData DEFAULT = new GuRecipeKnowledgeData(List.of());
    public static final Codec<GuRecipeKnowledgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(GuRecipeKnowledgeData::learned)
    ).apply(instance, GuRecipeKnowledgeData::new));

    public GuRecipeKnowledgeData {
        learned = learned.stream().distinct().sorted().toList();
    }

    public static @NotNull GuRecipeKnowledgeData decode(@NotNull FriendlyByteBuf buf) {
        return new GuRecipeKnowledgeData(buf.readList(FriendlyByteBuf::readUtf));
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull GuRecipeKnowledgeData value) {
        buf.writeCollection(value.learned(), FriendlyByteBuf::writeUtf);
    }

    public boolean knows(@NotNull String id) { return learned.contains(id); }

    public GuRecipeKnowledgeData withLearned(@NotNull String id) {
        return new GuRecipeKnowledgeData(Stream.concat(learned.stream(), Stream.of(id)).toList());
    }
}
