package net.alex.guzhenren.item.gu.tending;

import java.util.List;
import java.util.UUID;
import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.PrimevalStoneSupply;
import net.alex.guzhenren.item.gu.stack.GuCooldowns;
import net.alex.guzhenren.item.gu.stack.RefinedGuState;
import net.alex.guzhenren.item.gu.stack.VitalGuState;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 持续照顾型蛊[Tended Gu]基类：需要经过炼化、日常进食与照料维护的凡蛊[Mortal Gu]生命周期宿主。
 * <ul>
 *   <li>野生、炼化、进食与催动状态共用同一份物品栈数据组件，因此堆叠上限固定为 1（{@code Item.Properties} 由本宿主统
 *       一设定，不交由注册处决定）。</li>
 *   <li>行为模块 {@link GuBehavior} 响应 {@code checkEffect}、{@code applyEffect} 与各类放宽限制，具备独立生命周期的特
 *       殊蛊通过子类覆写扩展。</li>
 *   <li>本类负责蓄力阶梯、灌注[Channel]与催动扣费；饱食[Hunger]与进食归 {@link GuHunger}，生命归 {@link GuHealth}，长
 *       冷却时间戳归 {@link GuCooldowns}，跨日维持归 {@link GuUpkeep}，灌注定额归 {@link GuChanneling}，长冷却门控归
 *       {@link GuRefineCooldown}。</li>
 *   <li>⚠ 已炼化的蛊仅通过 {@link #feed} 自动取食（规则见 {@link GuHunger}）：仅在催动前与每次扣减饱食后触发；扣减来
 *       源包括主动催动（{@link #drive}）、后援搭档扣费（{@link #billAsPartner}）、灌注每步、跨日心跳（
 *       {@link GuUpkeep}，涵盖随身与空窍内蛊虫且扣前也会进食）以及小光蛊[Little Light Gu]开启，其余场景均不进食。</li>
 *   <li>食物不走右键喂食交互，也没有单独的喂食手势；处于饱食下限的灌注类蛊仅在玩家背包完全没有对应食物时才会被拒绝继续
 *       灌注（{@link GuHunger#carriesFood}）。</li>
 *   <li>⚠ 创造模式下忽略饱食下限：饱食条仍会降至 0 用于界面展示，但蛊虫照常灌注，因为创造模式下强行催动不会导致力竭死
 *       亡。</li>
 *   <li>⚠ 进食后仍付不起饱食（{@code GuHunger.isShortOfUse}，含增幅倍数）的催动须经 {@link GuForcedUse} 二次确认才允
 *       许强行催动[Forced Use]：首次 {@code drive} 弹出红字警告且不扣费，并返回 {@code WARNED}（{@code spend} 据此跳过
 *       冷却）；五秒内再次催动则执行 {@code trySpendAndApply} 并在效果后使蛊力竭死亡，此时 {@code drive} 返回 1 表示力
 *       竭耗尽而非正常消耗，部分行为可通过 {@code GuBehavior.defersForcedDeath} 将死亡延迟到该轮结束。</li>
 *   <li>本命蛊[Vital Gu]只拒绝而绝不强行催动，创造模式不警告也不死亡，{@link #tryAutoUse} 则静默跳过饱食不足的催
 *       动。</li>
 *   <li>{@link #billAsPartner} 是后援蛊（功倍蛊[Amplify Effect Gu]与杀招[Killing Move]搭档）的唯一扣费入口：扣除真元
 *       并在前后自动进食；若扣量后饱食归零同样触发力竭死亡，且延后至所支援的催动结算之后。创造模式付不起真元也照常后援
 *       （{@code PrimevalStoneSupply.payOrCreative}），原地催动的真元门槛（{@code checkInPlaceUse}）同样对创造模式放行。</li>
 *   <li>⚠ 所有真元支出均经 {@code PrimevalStoneSupply.pay} 结算，每步灌注先补足至至少
 *       {@code GuChanneling.ESSENCE_FLOOR} （{@code topUpFor}）再做定量；灌注步会先进食以抬高饱食折算上限，炼化支持分期
 *       投入真元换取长期持有。</li>
 *   <li>{@link #isRefining} 与 {@link #isChanneling} 读取实体正在使用的物品栈，使服务端与可见范围内的客户端都能判断玩家
 *       是否正在按住炼化或灌注，供 Epic Fight 桥接切换对应动作姿态。</li>
 *   <li>⚠ {@link #canBeVital} 负责拦截催动即消失的一次性蛊成为本命蛊，防止玩家首次点击即失去本命蛊并受反噬。</li>
 *   <li>{@code announceDeath} 是全部死因（{@link #starve}、{@link #exhaust}、{@link #ruin}）的唯一汇流点，也是
 *       {@link VitalGuLostEvent} 的唯一发布方；新增死因必须经由此处统一收口。</li>
 *   <li>本文件有意超过 300 行：除上述规则外余下为原版 {@code Item} 覆写与行为钩子；灌注本身即本类的 {@code onUseTick}，
 *       直接驱动进食、付费与效果，而 {@code GuChanneling} 仅做纯数值定量且不持有物品逻辑。</li>
 * </ul>
 */

public class TendedGuItem extends MortalGuItem {

    private static final String TOOLTIP_REFINE = "guzhenren.item.gu.refine_progress";
    private static final String TOOLTIP_INVESTED = "guzhenren.item.gu.invested";
    private static final String TOOLTIP_HUNGER = "guzhenren.item.gu.hunger_progress";
    private static final String TOOLTIP_HEALTH = "guzhenren.item.gu.health";
    private static final String CAPTION_CHANNELING = "guzhenren.hud.using";
    private static final String FAILED_FORCED_USE_WARNING = "guzhenren.item.failed.forced_use_warning";
    private static final String FAILED_VITAL_NO_FORCED_USE = "guzhenren.item.failed.vital_no_forced_use";
    private static final String MSG_HUNGRY = "guzhenren.item.gu.hungry";
    private static final String MSG_STARVED = "guzhenren.item.gu.starved";
    private static final String MSG_FORCED_DEAD = "guzhenren.item.gu.forced_dead";
    private static final String MSG_REFINE_DEAD = "guzhenren.item.gu.refine_dead";
    protected static final int WARNED = -1;
    protected final GuHunger hunger;
    protected final GuHealth health;
    private final GuChanneling channeling;
    private final GuRefineCooldown refineCooldown;

    public TendedGuItem(GuProperties guProperties, GuBehavior... behaviors) {
        super(new Properties(), guProperties, behaviors);
        this.hunger = guProperties.buildHunger();
        this.health = guProperties.buildHealth();
        this.channeling = new GuChanneling(guProperties, hunger);
        this.refineCooldown = new GuRefineCooldown(guProperties);
    }

    //region what the behaviors answer [行为应答]
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) { return checkModules(player, stack); }

    protected void applyEffect(ServerPlayer player, ItemStack stack) { applyModules(player, stack); }

    protected int feedUnits(ItemStack food) { return guProperties.feedUnits(food); }

    public boolean canBeVital() { return true; }
    //endregion

    //region state [状态]
    public static RefinedGuState getState(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.REFINED_GU_STATE, RefinedGuState.WILD);
    }

    void store(ItemStack stack, RefinedGuState state) {
        stack.set(ModDataComponents.REFINED_GU_STATE, new RefinedGuState(state.refined(),
                Math.min(state.refineProgress(), getRefineCost()),
                Math.min(state.investedEssence(), guProperties.getEssencePerRound()),
                state.hunger(),
                Mth.clamp(state.damageTaken(), 0, getMaxHealth())));
    }

    public boolean isRefined(ItemStack stack) { return getState(stack).refined(); }

    public boolean isHungry(ItemStack stack) {
        return isRefined(stack) && hunger.isHungry(stack);
    }
    //endregion

    //region Gu health [蛊虫生命值]
    public int getMaxHealth() { return health.max(); }

    public int getHealth(ItemStack stack) { return health.get(stack); }

    public void setHealth(ItemStack stack, int value) { health.set(this, stack, value); }

    public boolean takeDamage(ServerPlayer holder, ItemStack stack, int amount) {
        return health.takeDamage(this, holder, stack, amount);
    }

    protected void heal(ItemStack stack, int amount) { health.heal(this, stack, amount); }
    //endregion

    //region refining [炼化]
    public static final int POST_REFINE_COOLDOWN_TICKS = Ticks.SECOND;

    private void refineStep(ServerPlayer player, ItemStack stack, int invest) {
        RefinedGuState s = getState(stack);
        int next = s.refineProgress() + invest;
        if (next < getRefineCost()) {
            store(stack, s.withRefine(next));
            return;
        }
        refineAtBirth(player, stack);
    }

    public void refineAtBirth(ServerPlayer player, ItemStack stack) {
        store(stack, new RefinedGuState(true, getRefineCost(), 0, 0, getState(stack).damageTaken()));
        hunger.bind(player, stack);
        stack.set(ModDataComponents.REFINED_AT,
                GuCooldowns.createCooldownStamp(player, POST_REFINE_COOLDOWN_TICKS));
        GuCooldowns.applyPostRefineCooldown(player.getCooldowns(), this,
                TimeFlow.shortenWait(player, POST_REFINE_COOLDOWN_TICKS));
    }
    //endregion

    //region channeling [灌注]
    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack,
                          int remaining) {
        if (!(entity instanceof ServerPlayer player)) return;

        int elapsed = GuChanneling.CHANNEL_MAX_TICKS - remaining;
        if (elapsed < 0 || elapsed % GuChanneling.STEP_TICKS != 0) return;

        if (!isRefined(stack)) {
            refineTick(player, stack, elapsed);
            return;
        }
        if (!guProperties.isChanneled()) return;

        feed(player, stack);
        PrimevalStoneSupply.topUpFor(player, GuChanneling.ESSENCE_FLOOR);
        int take = channeling.stepAmount(player, stack, elapsed, isGuPaced(player));
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
            player.stopUsingItem();
            return;
        }
        channeling.pour(this, player, stack, take);
        feed(player, stack);
    }

    private void refineTick(ServerPlayer player, ItemStack stack, int elapsed) {
        PrimevalStoneSupply.topUpFor(player, GuChanneling.ESSENCE_FLOOR);
        int left = getRefineCost() - getState(stack).refineProgress();
        int take = (int) Math.min(left, channeling.getPoured(player, elapsed, isGuPaced(player)));
        if (take <= 0 || !PrimevalStoneSupply.pay(player, take)) {
            player.stopUsingItem();
            return;
        }
        refineStep(player, stack, take);
        if (isRefined(stack)) player.stopUsingItem();
    }

    public static boolean isRefining(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof TendedGuItem gu
                && !gu.isRefined(player.getUseItem());
    }

    public static boolean isChanneling(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof TendedGuItem gu
                && gu.isRefined(player.getUseItem()) && gu.guProperties.isChanneled();
    }

    private boolean isGuPaced(Player player) { return getRankGap(player) > 0; }

    void applyEffectWithCooldown(ServerPlayer player, ItemStack stack) {
        applyEffect(player, stack);
        if (guProperties.getEffectCooldownTicks() > 0) {
            stack.set(ModDataComponents.USED_AT,
                    GuCooldowns.createCooldownStamp(player, guProperties.getEffectCooldownTicks()));
        }
    }
    //endregion

    //region the long cooldown [长冷却]
    @Override
    protected void spend(ServerPlayer player, ItemStack stack, int count) {
        if (count == WARNED) return;

        super.spend(player, stack, count);
        refineCooldown.put(this, player, stack);
    }
    //endregion

    //region the clicks [点击]
    @Override
    protected boolean keepsRefusedClick() { return true; }

    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) {
        if (!isRefined(stack)) {
            return checkEssenceGate(player, Math.min(GuChanneling.ESSENCE_FLOOR, getRefineCost()),
                    Refusal.ESSENCE);
        }
        Refusal poor = checkEssenceGate(player, useThreshold(stack), Refusal.ESSENCE);
        if (poor != null) return poor;

        if (guProperties.isChanneled() && channeling.getEssenceAboveHungerFloor(player, stack) <= 0
                && !hunger.carriesFood(this, player)) {
            return new Refusal(Refusal.GU_HUNGRY);
        }
        return checkCooldownThenOwnUse(player, stack);
    }

    private @Nullable Refusal checkCooldownThenOwnUse(Player player, ItemStack stack) {
        Refusal cooling = refineCooldown.check(this, player, stack);
        return cooling != null ? cooling : checkOwnUse(player, stack);
    }

    protected long useThreshold(ItemStack stack) {
        return guProperties.isChanneled() ? GuChanneling.ESSENCE_FLOOR : guProperties.getEssencePerRound();
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) { return checkEffect(player, stack); }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) ? performOwnUse(player, stack) : 0;
    }

    @Override
    protected int performOwnUse(ServerPlayer player, ItemStack stack) {
        return guProperties.isChanneled() ? 0 : drive(player, stack);
    }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) {
        if (!isRefined(stack)) return GuChanneling.CHANNEL_MAX_TICKS;
        return guProperties.isChanneled() ? GuChanneling.CHANNEL_MAX_TICKS : useChargeTicks(player, stack);
    }

    protected int useChargeTicks(Player player, ItemStack stack) { return useChargeByGap(player); }

    protected int drive(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (stopsShortUse(player, stack)) return WARNED;
        if (trySpendAndApply(player, stack)) return 1;
        feedAndWarn(player, stack);
        return 0;
    }

    /** 饱食[Hunger]不足的催动在此停下时为 true：已警告，或本命蛊直接拒。 */
    protected final boolean stopsShortUse(ServerPlayer player, ItemStack stack) {
        return isShortOfUse(player, stack) && !confirmForcedUse(player, stack);
    }

    protected final boolean isShortOfUse(ServerPlayer player, ItemStack stack) {
        return !player.getAbilities().instabuild
                && hunger.isShortOfUse(stack, getHungerCostMultiplier(player, stack));
    }

    private boolean confirmForcedUse(ServerPlayer player, ItemStack stack) {
        if (VitalGuState.isVital(stack)) {
            ActionBar.refuse(player, FAILED_VITAL_NO_FORCED_USE);
            return false;
        }
        if (GuForcedUse.confirm(player, stack)) return true;

        ActionBar.refuse(player, FAILED_FORCED_USE_WARNING);
        return false;
    }

    protected final boolean trySpendAndApply(ServerPlayer player, ItemStack stack) {
        PrimevalStoneSupply.pay(player, guProperties.getEssencePerRound());
        boolean drivenOnAnEmptyBar = hunger.spendUse(stack, getHungerCostMultiplier(player, stack));
        applyEffectWithCooldown(player, stack);
        if (!drivenOnAnEmptyBar || player.getAbilities().instabuild || defersForcedDeath(player, stack)) return false;

        exhaust(player, stack);
        return true;
    }

    private boolean defersForcedDeath(ServerPlayer player, ItemStack stack) {
        return getModules().stream().anyMatch(behavior -> behavior.defersForcedDeath(this, player, stack));
    }

    /** 为他蛊效果付一次后援催动，自身无效果；真元[Essence]池与元石[Primeval Stone]都不足才为 false。 */
    public final boolean billAsPartner(ServerPlayer player, ItemStack stack) {
        if (!PrimevalStoneSupply.payOrCreative(player, guProperties.getEssencePerRound())) return false;

        feed(player, stack);
        if (hunger.spendAsPartner(stack) && !player.getAbilities().instabuild) {
            exhaust(player, stack);
            stack.shrink(1);
            return true;
        }
        feedAndWarn(player, stack);
        return true;
    }

    @Override
    protected final @Nullable Refusal checkTapUse(Player player, ItemStack stack) {
        Refusal cooling = refineCooldown.check(this, player, stack);
        return cooling != null ? cooling : checkOwnTapUse(player, stack);
    }

    protected @Nullable Refusal checkOwnTapUse(Player player, ItemStack stack) { return null; }

    protected boolean canUseDuringEffectCooldown() {
        return getModules().stream().anyMatch(GuBehavior::canUseDuringEffectCooldown);
    }

    protected int getHungerCostMultiplier(Player player, ItemStack stack) {
        int multiplier = 1;
        for (GuBehavior behavior : getModules()) {
            multiplier = Math.max(multiplier, behavior.getHungerCostMultiplier(this, player, stack));
        }
        return multiplier;
    }

    public final boolean tryAutoUse(ServerPlayer player, ItemStack stack) {
        if (!isRefined(stack) || guProperties.isChanneled() || player.getCooldowns().isOnCooldown(this)) return false;
        if (checkInPlaceUse(player, stack) != null) return false;

        feed(player, stack);
        if (isShortOfUse(player, stack)) return false;
        spend(player, stack, performOwnUse(player, stack));
        return true;
    }

    @Override
    protected @Nullable Refusal checkInPlaceUse(Player player, ItemStack stack) {
        Refusal poor = checkInPlaceEssenceGate(player, useThreshold(stack));
        return poor != null ? poor : checkCooldownThenOwnUse(player, stack);
    }

    @Override
    protected int performInPlaceUse(ServerPlayer player, ItemStack stack) {
        return isRefined(stack) && !guProperties.isChanneled() ? performOwnUse(player, stack) : 0;
    }

    protected void feed(ServerPlayer player, ItemStack stack) { hunger.feed(this, player, stack); }

    /** 扣量后立即进食；下次日切会饿死它时再发饥饿警告。 */
    protected final void feedAndWarn(ServerPlayer player, ItemStack stack) {
        feed(player, stack);
        if (isHungry(stack)) announce(player, stack, MSG_HUNGRY);
    }

    /** 为不算催动的开销（杀招[Killing Move]启动）从饱食[Hunger]条扣 {@code points}；不足时为 false。 */
    public final boolean takeHunger(ItemStack stack, int points) {
        RefinedGuState state = getState(stack);
        if (state.hunger() < points) return false;
        store(stack, state.withHunger(state.hunger() - points));
        return true;
    }

    //endregion

    //region display [显示]
    @Override
    public Component chargeCaption(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineCaption(getState(stack).refineProgress());
        return guProperties.isChanneled()
                ? Component.translatable(CAPTION_CHANNELING, getState(stack).investedEssence(),
                guProperties.getEssencePerRound())
                : Component.translatable(CAPTION_USING_PLAIN);
    }

    @Override
    public @Nullable Float chargeFraction(ItemStack stack, int remainingTicks) {
        if (!isRefined(stack)) return refineFraction(stack);
        if (!guProperties.isChanneled()) return null;
        return getState(stack).investedEssence() / (float) guProperties.getEssencePerRound();
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return isRefined(stack) ? super.getName(stack) : ModDisplayText.formatWild(super.getName(stack));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (getState(stack).damageTaken() <= 0) return;

        tooltip.add(Component.translatable(TOOLTIP_HEALTH, getHealth(stack), getMaxHealth())
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected @Nullable MutableComponent formatProgressLine(ItemStack stack) {
        if (!isRefined(stack)) {
            return Component.translatable(TOOLTIP_REFINE, getState(stack).refineProgress(), getRefineCost());
        }
        if (guProperties.isChanneled()) {
            return Component.translatable(TOOLTIP_INVESTED, getState(stack).investedEssence(),
                    guProperties.getEssencePerRound());
        }
        return switch (hunger) {
            case GuHunger.HungerBar bar -> Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
            case GuHunger.RecoverBar bar -> Component.translatable(TOOLTIP_HUNGER, getState(stack).hunger(), bar.max());
            case GuHunger.NoClock ignored -> null;
        };
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return isRefined(stack) ? hunger.isBarVisible(stack) : getState(stack).refineProgress() > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) { return Math.round(getBarFraction(stack) * 13.0F); }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Mth.hsvToRgb(getBarFraction(stack) / 3.0F, 1.0F, 1.0F);
    }

    private float getBarFraction(ItemStack stack) {
        return isRefined(stack) ? hunger.getBarFraction(stack) : refineFraction(stack);
    }

    private float refineFraction(ItemStack stack) {
        return getRefineCost() <= 0 ? 0.0F : getState(stack).refineProgress() / (float) getRefineCost();
    }
    //endregion

    //region the day clock [日钟]
    protected void payOwnUpkeep(ServerPlayer player, ItemStack stack) {}
    //endregion

    //region the three deaths [三种死亡]
    private static void announce(ServerPlayer player, ItemStack stack, String key) {
        player.sendSystemMessage(Component.translatable(key, stack.getHoverName()));
    }

    private static void announceDeath(ServerPlayer holder, ItemStack stack, String key) {
        announce(holder, stack, key);
        UUID uuid = VitalGuState.getOwner(stack);
        if (uuid == null) return;

        ModEvents.post(new VitalGuLostEvent(holder.server, uuid, stack));
    }

    public static void starve(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_STARVED); }

    public static void exhaust(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_FORCED_DEAD); }

    public static void ruin(ServerPlayer holder, ItemStack stack) { announceDeath(holder, stack, MSG_REFINE_DEAD); }
    //endregion
}
