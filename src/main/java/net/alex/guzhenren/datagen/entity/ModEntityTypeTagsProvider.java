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
 * 实体类型标签：{@link ModEntityTypeTags#PREDATOR_PREY} 为狩猎野兽主动攻击的原版家畜、小动物；
 * {@link ModEntityTypeTags#WILD_GU} 为全部野生蛊实体。
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
    }
}
