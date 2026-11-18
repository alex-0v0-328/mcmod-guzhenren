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
 * 凡蛊抽象基类：继承 {@link GuItem}，组合静态数值规格 {@link GuProperties}、行为列表 {@link GuBehavior} 与真元门槛校
 * 验，自身不直接维护物品栈可变状态。
 * <ul>
 *   <li>⚠ 所有凡蛊堆叠上限固定为 1：本类构造器统一调用 {@code stacksTo(1)}，子类无需重复设置；因为物品栈数据组件在整组
 *       物品间共享，一旦允许堆叠会导致整组蛊虫共用同一份炼化、饱食或资质状态。</li>
 *   <li>承上启下连接 {@link GuItem} 与三类生命周期宿主：炼化即催动的一次性消耗蛊 {@link OneShotGuItem}（无物品栈状
 *       态）、需炼化喂养的持续照顾型蛊[Tended Gu] {@link TendedGuItem}（携带 {@link RefinedGuState}）及其一次性消耗子类
 *       {@link ConsumedGuItem}（希望蛊[Hope Gu]直接继承本类以启动开窍[Awakening]仪式）；构造器在注册期调用
 *       {@link GuProperties#validate} 执行数值自检，配置错误在启动时即点名报错；{@link #checkStageUpGate} 封装升阶类凡
 *       蛊的公共门槛（目标空窍转数等于本蛊转数且阶段未达巅峰）。</li>
 *   <li>{@link #getUseEssence} 返回单次催动消耗的真元（持续照顾型蛊取单次消耗，一次性消耗蛊取炼化消耗，灌注[Channel]型
 *       蛊返回 0）；{@link #canBeAmplified} 判断是否可被功倍蛊[Amplify Effect Gu]增幅；{@link #getEffectSource} 固定以
 *       本蛊的物品注册 ID 作为其施加的全部流派效果来源；{@link #formatType} 将用途分类格式化为顶层名或「父类型 · 子类
 *       型」（如 {@code 防御 · 反击}）。</li>
 *   <li>⚠ 单只凡蛊至多只能挂载一个声明了 {@link GuBehavior#canBeAmplified} 的行为（每个可增幅行为都会在生效时向功倍蛊扣
 *       费一次，若超过一个由构造器直接抛出异常拦截）；{@link #checkModules} 按注册顺序返回首个拒绝原
 *       因，{@link #applyModules} 依次执行全部行为，外部通过 {@link #getBehavior} 检索行为实例。</li>
 *   <li>⚠ 每只凡蛊均固定覆写 {@link #hasUse} 返回 {@code true} 以响应右键（野生蛊至少需响应炼化，若不响应会让玩家误以为
 *       按键失灵）。</li>
 *   <li>⚠ 真元门槛 {@link #checkEssenceGate} 通过 {@link PrimevalStoneSupply#canAfford} 将随身元石[Primeval Stone]折算
 *       入可用真元，双端判定一致，实际扣费统一走 {@link PrimevalStoneSupply#pay}。</li>
 *   <li>⚠ 凡蛊本身不单独校验 {@code isAwakened}，而是由真元门槛自然拦截（未开窍者真元上限为 0 且不计随身元石）：炼化消
 *       耗为 0 的一次性消耗蛊按设计允许未开窍凡人直接使用（如生机叶蛊[Vitality Leaf Gu]、寿蛊[Lifespan Gu]、
 *       胆识蛊[Guts Gu]、第二空窍蛊[Second Aperture Gu]），舍利蛊[Relics Gu]由 {@link #checkStageUpGate} 拦截，唯独希望
 *       蛊显式拒绝已开窍玩家。</li>
 *   <li>所有成功催动均以 {@link #spend} 收尾：设置冷却、扣减物品栈后在事件总线发布 {@link GuUsedEvent}，作为监听凡蛊催
 *       动的唯一公共接缝；催动蓄力时长按持有者与蛊虫的转数差分为三档（{@link #useChargeByGap}），与小阶段无关。</li>
 *   <li>⚠ 本命蛊绑定状态存于物品栈组件并仅通过 {@link VitalGuState} 读写，本类负责渲染本命前缀名称、附魔光效并拦截丢
 *       弃；当 {@link #onDroppedByPlayer} 拦截 Q 键丢弃时，由于客户端已先行清空快捷栏槽位，服务端必须将该槽位的远端同步
 *       快照置空以强制下一帧重发原物品。</li>
 * </ul>
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
        super(properties.stacksTo(1), guProperties.getRank(), guProperties.getPath());
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
