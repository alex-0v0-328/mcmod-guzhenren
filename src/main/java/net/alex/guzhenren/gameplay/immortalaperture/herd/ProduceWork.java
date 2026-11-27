package net.alex.guzhenren.gameplay.immortalaperture.herd;

import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 牧场产物入库[Pasture Produce]：托管轮里替主人剪栏内的羊、捡栏内地上的掉落物（鸡下的蛋、羽毛、剪下的羊毛等），都直进
 * 库藏；不设开关，有主就跑（同元泉收集），不耗剪刀。
 * <ul>
 *   <li>{@link #collect}：先剪羊（可支使、成年、长好了毛的 {@link Sheep}，掉落按 NeoForge 剪毛接口取，不落地），再捡
 *       牧场范围内（{@link PastureWork.Pasture#box}）所有物品实体；库藏不收的（胆识蛊）留在原地。只管已加载的牧场。</li>
 *   <li>只剪羊：哞菇也能剪，但剪了会变回牛（Alex 只要剪羊毛）。</li>
 *   <li>⚠ 物品实体一律捡走，玩家丢在栏里的也算；牧场里本就不该堆东西。</li>
 * </ul>
 */

public final class ProduceWork {

    private ProduceWork() {}

    public record Produce(int sheared, int picked) {}

    public static int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        int handled = 0;
        for (PastureWork.Pasture pasture : PastureWork.pastures(data)) {
            Produce done = collect(level, data, pasture);
            handled += done.sheared() + done.picked();
        }
        return handled;
    }

    public static @NotNull Produce collect(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                           @NotNull PastureWork.Pasture pasture) {
        if (!level.isLoaded(pasture.center().atY(level.getMinBuildHeight()))) return new Produce(0, 0);
        BlissfulStorage storage = BlissfulStorage.of(data);
        ItemStack shears = new ItemStack(Items.SHEARS);
        int sheared = 0;
        for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, pasture.box(level))) {
            if (!pasture.contains(sheep) || !LandRules.isManaged(sheep) || !sheep.readyForShearing()) continue;
            for (ItemStack drop : sheep.onSheared(null, shears, level, sheep.blockPosition())) storage.insert(drop);
            sheared++;
        }
        int picked = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, pasture.box(level))) {
            if (!item.isAlive() || !pasture.contains(item) || !BlissfulStorage.accepts(item.getItem())) continue;
            if (!storage.insert(item.getItem().copy())) continue;
            item.discard();
            picked++;
        }
        return new Produce(sheared, picked);
    }
}
