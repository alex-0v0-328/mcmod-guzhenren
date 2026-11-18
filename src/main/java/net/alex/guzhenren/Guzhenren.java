package net.alex.guzhenren;

import com.mojang.logging.LogUtils;
import net.alex.guzhenren.compat.EpicFightIntegration;
import net.alex.guzhenren.compat.FtbUltimineIntegration;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.StonePurse;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorageService;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.stack.StoredGuItems;
import net.alex.guzhenren.registry.advancement.ModCriteriaTriggers;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.alex.guzhenren.registry.fluid.ModFluidTypes;
import net.alex.guzhenren.registry.fluid.ModFluids;
import net.alex.guzhenren.registry.item.ModCreativeTabs;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.alex.guzhenren.registry.item.ModItems;
import net.alex.guzhenren.registry.loot.ModLootModifiers;
import net.alex.guzhenren.registry.menu.ModMenus;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.alex.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 主模组（{@code guzhenren}）入口类。
 * <ul>
 *   <li>提供全仓共用的 {@link #MOD_ID} 与资源路径构造函数 {@link #id}，并在构造函数中按依赖顺序将 14 个
 *       {@code DeferredRegister} 挂载到模组事件总线；世界生成交由配套模组 Gu World 负责，由其引用并放置本模组注
 *       册的元泉[Spirit Spring]与野生知心草方块。</li>
 *   <li>在构造期通过回调桥接玩法层与物品层：将物品侧 {@link PrimevalStoneSupply} 包装为 {@link StonePurse} 注入
 *       空窍温养[Nourish]服务（{@link ApertureNourishService}），并将基于蛊虫物品的 {@link StoredGuItems} 注入空窍存储
 *       服务（{@link ApertureStorageService}），使 {@code gameplay} 包不反向依赖 {@code item} 包。</li>
 *   <li>注册 Epic Fight 动画与事件联动（{@link EpicFightIntegration}）；对可选模组 FTB 连锁[FTB Ultimine]则仅在检测到其
 *       已加载时调用 {@link FtbUltimineIntegration#initialize}（未安装时仅读取字符串常量
 *       {@link FtbUltimineIntegration#MOD_ID}）。</li>
 * </ul>
 */

@Mod(Guzhenren.MOD_ID)
public class Guzhenren {

    public static final String MOD_ID = "guzhenren";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    public Guzhenren(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModParticles.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCriteriaTriggers.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ApertureNourishService.setStonePurse(new StonePurse(
                PrimevalStoneSupply::canAfford, PrimevalStoneSupply::topUp, PrimevalStoneSupply::trySpend));
        ApertureStorageService.setStoredGu(new StoredGuItems());
        modEventBus.addListener(EpicFightIntegration::onAnimationRegistry);
        EpicFightIntegration.initialize();
        if (ModList.get().isLoaded(FtbUltimineIntegration.MOD_ID)) FtbUltimineIntegration.initialize();
    }
}
