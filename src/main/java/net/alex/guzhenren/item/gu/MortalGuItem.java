package net.alex.guzhenren.item.gu;

import java.util.List;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 凡蛊：持有 {@link GuProperties}、{@link GuBehavior} 与真元[Essence] 门槛，无物品堆状态。
 * <ul>
 *   <li>介于 {@link GuItem} 与三种生命周期宿主间：{@link OneShotGuItem} 炼化即催动，无状态，可堆叠；
 *       {@link TendedGuItem} 炼化后补食、催动，带 {@link RefinedGuState}；及其 {@link ConsumedGuItem}；希望蛊[Hope Gu]
 *       开启空窍[Aperture]，三者皆非。构造器跑 {@link GuProperties#validate}，笔误启动即失败并点名；
 *       {@link #checkStageUpGate} 是升阶蛊唯一检查：空窍转数等于蛊转数，阶段低于巅峰。</li>
 *   <li>{@link #getUseEssence}：一次催动所付真元（持续照顾型蛊[Tended Gu] 单次代价、即时一次性蛊的炼化，灌注[Channel]
 *       为 0）；{@link #canBeAmplified}：功倍蛊[Amplify Effect Gu] 能否倍增它；类型行 {@link #formatType} 显示类型或
 *       「父 · 分支」（{@code 防御 · 反击}）。</li>
 *   <li>⚠ 至多一个行为可增幅：每个都从自身效果向功倍蛊结账，第二个由构造器点名拒绝；蛊的作为即其行为，按注册顺序：
 *       {@link #checkModules} 取首个拒绝，{@link #applyModules} 全执行，他处经 {@link #getBehavior} 询问。</li>
 *   <li>⚠ 每只蛊都响应右键，即使无事可做：炼化总是响应；不响应的叶类让蛊在手中像坏了。</li>
 *   <li>⚠ 真元门槛 {@link #checkEssenceGate} 经 {@code PrimevalStoneSupply.canAfford} 把携带元石[Primeval Stone] 连同真
 *       元池计数，两端一致；每笔代价都经 {@code PrimevalStoneSupply.pay} 付，放行即付得清。</li>
 *   <li>⚠ 蛊不查 {@code isAwakened}：真元门槛代之（元石有空窍才计），零代价放行；即时一次性蛊无炼化代价，无空窍凡人按设
 *       计可用生机叶蛊[Vitality Leaf Gu]、寿蛊[Lifespan Gu]、胆识蛊[Guts Gu]、第二空窍蛊[Second Aperture Gu]，舍利
 *       蛊[Relics Gu] 止于 {@link #checkStageUpGate}；仅希望蛊查 {@code isAwakened}：拒绝已有第一空窍者。</li>
 *   <li>生效催动都以 {@code spend} 结束，设冷却、扣蛊后发布 {@link GuUsedEvent}：任何蛊催动反应的唯一接缝，新蛊无需代
 *       码；蓄力按持有者与蛊的转数差定节奏（{@link #useChargeByGap}），不按阶段。</li>
 *   <li>⚠ 本命绑定存物品堆，只经 {@link VitalGuState} 读写，本类显示名字标记与附魔光效并留蛊在手；
 *       {@link #onDroppedByPlayer} 拒绝 Q 键丢弃时客户端已清空该格，即把服务端该格远端副本标过期，下次广播放回。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuProperties
 * @since 1.0.0
 */

public abstract class MortalGuItem extends GuItem {

    public static final int REFINE_DONE_COOLDOWN_TICKS = 2 * Ticks.SECOND;
    public static final int USE_FAST_TICKS = Ticks.SECOND / 4;
    public static final int USE_SAME_TICKS = Ticks.HALF_SECOND;
    public static final int USE_SLOW_TICKS = Ticks.SECOND;
    private static final String KIND_KEY = "guzhenren.display.gu";
    private static final String TYPE_KEY = "guzhenren.display.gu_type";
    private static final String TYPE_BRANCH_KEY = "guzhenren.display.gu_type_branch";
    private static final String FAILED_STAGE_PEAK = "guzhenren.item.failed.stage_peak";
    protected static final String CAPTION_USING_PLAIN = "guzhenren.hud.using_plain";
    private static final String CAPTION_REFINING_PLAIN = "guzhenren.hud.refining_plain";
    private static final String CAPTION_REFINING = "guzhenren.hud.refining";
    protected final GuProperties guProperties;
    private final List<GuBehavior> behaviors;

    protected MortalGuItem(Properties properties, GuProperties guProperties, GuBehavior... behaviors) {
        super(properties, guProperties.getRank(), guProperties.getPath());
        this.guProperties = guProperties;
        this.behaviors = List.of(behaviors);
        String id = getRank().name() + " " + getClass().getSimpleName()
                + this.behaviors.stream().map(behavior -> behavior.getClass().getSimpleName()).toList();
        guProperties.validate(id);
        long amplified = this.behaviors.stream().filter(GuBehavior::canBeAmplified).count();
        if (amplified > 1) {
            throw new IllegalStateException("Gu '%s' has %d amplified behaviors; each would bill the Amplify Effect Gu"
                    .formatted(id, amplified));
        }
    }

    @Override
    protected String getKindKey() { return KIND_KEY; }

    public GuProperties getGuProperties() { return guProperties; }

    public int getRefineCost() { return guProperties.getRefineCost(); }

    public long getUseEssence() { return guProperties.isChanneled() ? 0L : guProperties.getEssencePerRound(); }

    public boolean canBeAmplified() { return behaviors.stream().anyMatch(GuBehavior::canBeAmplified); }

    /** 本蛊所施全部流派效果的来源：其物品 ID。 */
    public final ResourceLocation getEffectSource() { return BuiltInRegistries.ITEM.getKey(this); }

    public final <P> PathEffect<P> createEffect(PathEffectType<P> type, long endTick, P payload) {
        return guProperties.createEffect(type, getEffectSource(), endTick, payload);
    }

    public final int getEffectCooldownLeft(Player player, ItemStack stack) {
        return GuCooldowns.getCooldownLeft(player, stack.get(ModDataComponents.USED_AT),
                guProperties.getEffectCooldownTicks());
    }

    //region behaviors [行为]
    public <M extends GuBehavior> @Nullable M getBehavior(Class<M> type) {
        for (GuBehavior behavior : behaviors) {
            if (type.isInstance(behavior)) return type.cast(behavior);
        }
        return null;
    }

    protected final List<GuBehavior> getModules() { return behaviors; }

    protected final @Nullable Refusal checkModules(Player player, ItemStack stack) {
        for (GuBehavior behavior : behaviors) {
            Refusal refusal = behavior.checkEffect(this, player, stack);
            if (refusal != null) return refusal;
        }
        return null;
    }

    protected final void applyModules(ServerPlayer player, ItemStack stack) {
        for (GuBehavior behavior : behaviors) behavior.applyEffect(this, player, stack);
    }
    //endregion

    //region refining [炼化]
    protected final @Nullable Refusal checkEssenceGate(Player player, long required, String key) {
        if (required <= 0) return null;
        return PrimevalStoneSupply.canAfford(player, required) ? null : new Refusal(key);
    }

    public final @Nullable Refusal checkStageUpGate(Aperture aperture) {
        if (aperture.rank() != getRank()) {
            return new Refusal(Refusal.RANK_MISMATCH);
        }
        return aperture.stage() == Stage.HIGHEST
                ? new Refusal(FAILED_STAGE_PEAK, Component.translatable(aperture.rank().getTranslationKey())) : null;
    }

    protected final void payRefineCost(ServerPlayer player) { PrimevalStoneSupply.pay(player, getRefineCost()); }
    //endregion

    //region charge [蓄力]
    protected int getRankGap(Player player) { return ApertureService.getRank(player).ordinal() - getRank().ordinal(); }

    protected int useChargeByGap(Player player) {
        int gap = getRankGap(player);
        if (gap > 0) return TimeFlow.shortenWait(player, USE_FAST_TICKS);
        return TimeFlow.shortenWait(player, gap == 0 ? USE_SAME_TICKS : USE_SLOW_TICKS);
    }
    //endregion

    //region the click [点击]
    @Override
    protected final boolean hasUse() { return true; }

    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return null; }

    protected int performOwnUse(ServerPlayer player, ItemStack stack) { return 0; }

    @Override
    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        super.spend(player, stack, count);
        ModEvents.post(new GuUsedEvent(player, this));
    }
    //endregion

    //region display [显示]
    protected final Component refineCaption(int invested) {
        return getRefineCost() > 0
                ? Component.translatable(CAPTION_REFINING, invested, getRefineCost())
                : Component.translatable(CAPTION_REFINING_PLAIN);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        StrengthPathBranch branch = guProperties.getStrengthPathBranch();
        if (getPath() == GuPath.STRENGTH && branch != StrengthPathBranch.NORMAL) {
            tooltip.add(Component.translatable(branch.getTranslationKey()).withStyle(ChatFormatting.GRAY));
        }
        GuType type = guProperties.getType();
        if (type != null) tooltip.add(formatType(type).withStyle(ChatFormatting.GRAY));
        MutableComponent line = formatProgressLine(stack);
        if (line != null) tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }

    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) { return null; }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Component name = super.getName(stack);
        return VitalGuState.isVital(stack) ? ModDisplayText.formatVital(name) : name;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) { return VitalGuState.isVital(stack) || super.isFoil(stack); }

    static MutableComponent formatType(GuType type) {
        Component name = Component.translatable(type.getTranslationKey());
        GuType parent = type.getParent();
        if (parent == null) return Component.translatable(TYPE_KEY, name);
        return Component.translatable(TYPE_BRANCH_KEY, Component.translatable(parent.getTranslationKey()), name);
    }
    //endregion

    @Override
    public boolean onDroppedByPlayer(@NotNull ItemStack stack, @NotNull Player player) {
        if (!VitalGuState.isVital(stack)) return true;

        if (player instanceof ServerPlayer server) {
            server.inventoryMenu.setRemoteSlot(InventoryMenu.USE_ROW_SLOT_START + server.getInventory().selected,
                    ItemStack.EMPTY);
        }
        return false;
    }
}
