package net.alex.guzhenren.datagen.curios;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

/**
 * Writes the Curios slot data. There is no runtime Curios code in this mod at all.
 *
 * <p>Extends {@link top.theillusivec4.curios.api.CuriosDataProvider}. Creates the {@code hands} slot
 * (size 2) and attaches the player entity to the standard Curios slots. Curios is an optional
 * dependency; the full jar stays on the runtime classpath so {@code runData} can load this provider.
 *
 * <p>⚠ Curios carries equipment only, and no Gu ever goes into a Curios slot. That decision is why
 * the whole Curios footprint here is datagen.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModCuriosProvider extends CuriosDataProvider {

    public ModCuriosProvider(PackOutput output, ExistingFileHelper fileHelper,
                             CompletableFuture<HolderLookup.Provider> registries) {
        super(Guzhenren.MOD_ID, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        createSlot("hands").size(2);
        createEntities("player").addPlayer().addSlots("hands", "back", "body", "head");
    }
}
