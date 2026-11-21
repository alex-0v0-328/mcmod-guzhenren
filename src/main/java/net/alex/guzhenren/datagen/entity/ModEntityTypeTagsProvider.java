package net.alex.guzhenren.datagen.entity;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组实体类型标签数据生成器：{@link ModEntityTypeTags#PREDATOR_PREY} 收录供捕食野兽主动猎杀的原版家畜与小动物，
 * {@link ModEntityTypeTags#WILD_GU} 收录全部可捕捉的野生蛊实体，{@link ModEntityTypeTags#SOULSUNDER_IMMUNE} 收录盔甲架。
 */

public class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {

    public ModEntityTypeTagsProvider(PackOutput output,
                                     CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(ModEntityTypeTags.PREDATOR_PREY).add(
                EntityType.PIG,
                EntityType.COW,
                EntityType.SHEEP,
                EntityType.CHICKEN,
                EntityType.RABBIT);
        tag(ModEntityTypeTags.WILD_GU).add(
                ModEntityTypes.HOPE_GU_ENTITY.get(),
                ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(),
                ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(),
                ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(),
                ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(),
                ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(),
                ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get());
        tag(ModEntityTypeTags.SOULSUNDER_IMMUNE).add(EntityType.ARMOR_STAND);
    }
}
