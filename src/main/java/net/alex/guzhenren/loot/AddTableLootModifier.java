package net.alex.guzhenren.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Rolls one more loot table into a drop whose conditions hold, as {@code guzhenren:add_table}.
 *
 * <p>⚠ Port-only: the NeoForge 1.21.1 mainline uses NeoForge's own {@code neoforge:add_table}, which Forge 1.20.1
 * lacks. The extra table rolls on the same loot context and its stacks are split to their stack size.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class AddTableLootModifier extends LootModifier {

    public static final Codec<AddTableLootModifier> CODEC = RecordCodecBuilder.create(i -> codecStart(i).and(
            ResourceLocation.CODEC.fieldOf("table").forGetter(modifier -> modifier.table)
    ).apply(i, AddTableLootModifier::new));

    private final ResourceLocation table;

    public AddTableLootModifier(LootItemCondition[] conditions, ResourceLocation table) {
        super(conditions);
        this.table = table;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot,
                                                          @NotNull LootContext context) {
        LootTable extra = context.getResolver().getLootTable(table);
        extra.getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));
        return generatedLoot;
    }

    @Override
    public @NotNull Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
