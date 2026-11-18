package net.alex.guzhenren.gameplay.data;

import java.util.Objects;
import java.util.function.Supplier;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.storage.ApertureStorage;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.ShieldData;
import net.alex.guzhenren.gameplay.body.ShieldWindow;
import net.alex.guzhenren.gameplay.dimension.DimensionReturnData;
import net.alex.guzhenren.gameplay.dimension.TossStashData;
import net.alex.guzhenren.gameplay.mind.GuRecipeKnowledgeData;
import net.alex.guzhenren.gameplay.mind.KillingMoveData;
import net.alex.guzhenren.gameplay.mind.MindData;
import net.alex.guzhenren.gameplay.path.PathData;
import net.alex.guzhenren.gameplay.path.PathEffectData;
import net.alex.guzhenren.gameplay.rewind.PlayerRewindData;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家附件数据的统一读写门面：聚合本模组挂载在 {@link Player} 上的全部持久化、同步与内存状态。
 * <ul>
 *   <li>⚠ {@link #get} 是双端访问玩家附件的唯一入口（各 setter 返回值是否发生变更）；全仓仅本类直接调用 NeoForge 的
 *       {@code getData} / {@code setData} / {@code removeData}，业务服务一律经本类方法读写。</li>
 *   <li>共管理 16 个存档附件；二段飞行[Second-Stage Flight]标记、坠击起点高度与移动蓄力标记仅网络同步而不落盘；另有 3
 *       个纯内存状态：真元回复余数（{@link #getEssenceCarry}）与二段飞行油门（{@link #getSecondStageThrottle}）由双端各
 *       自维护，护盾无敌帧窗口（{@link #getShieldWindow}）仅在服务端使用。</li>
 *   <li>变更即自动同步的附件共 13 个：其中 8 个通过 {@code OWNER_ONLY} 仅同步给玩家本人，而二段飞行标记、坠击起点高度、
 *       移动蓄力标记、温养[Nourish]状态与开窍[Awakening]仪式 5 个附件同步给所有追踪该玩家的客户端以驱动外观与动
 *       作。</li>
 *   <li>⚠ 5 个状态以「附件是否存在（{@code hasData}）」编码：二段飞行标记与移动蓄力标记存在即开启，坠击起点高度仅在坠击
 *       期间非 {@code null}（三者重登或死亡即清除）；开窍仪式仅在仪式期间非 {@code null}；登出结转生命值通过
 *       {@link #takeHealthCarry} 取出时一并移除附件。</li>
 * </ul>
 */

public final class PlayerData {

    private final Player owner;

    private PlayerData(@NotNull Player owner) { this.owner = owner; }

    public static @NotNull PlayerData get(@NotNull Player player) { return new PlayerData(player); }

    public ApertureData getAperture() { return owner.getData(ModAttachments.APERTURE); }

    public float[] getEssenceCarry() { return owner.getData(ModAttachments.ESSENCE_CARRY); }

    public ApertureStorage getApertureStorage() { return owner.getData(ModAttachments.APERTURE_STORAGE); }

    public ApertureNourishData getNourish() { return owner.getData(ModAttachments.APERTURE_NOURISH); }

    public @Nullable AwakeningRitual getAwakeningRitual() { return find(ModAttachments.AWAKENING_RITUAL); }

    public BodyData getBody() { return owner.getData(ModAttachments.BODY); }

    public ShieldData getShield() { return owner.getData(ModAttachments.SHIELD); }

    public ShieldWindow getShieldWindow() { return owner.getData(ModAttachments.SHIELD_WINDOW); }

    public SoulData getSoul() { return owner.getData(ModAttachments.SOUL); }

    public PathData getPath() { return owner.getData(ModAttachments.PATH); }

    public PathEffectData getPathEffect() { return owner.getData(ModAttachments.PATH_EFFECT); }


    public MindData getMind() { return owner.getData(ModAttachments.MIND); }

    public KillingMoveData getKillingMoves() { return owner.getData(ModAttachments.KILLING_MOVE); }

    public GuRecipeKnowledgeData getGuRecipeKnowledge() { return owner.getData(ModAttachments.GU_RECIPE_KNOWLEDGE); }

    public DimensionReturnData getDimensionReturn() { return owner.getData(ModAttachments.DIMENSION_RETURN); }

    public TossStashData getTossStash() { return owner.getData(ModAttachments.TOSS_STASH); }

    public PlayerRewindData getRewind() { return owner.getData(ModAttachments.PLAYER_REWIND); }

    public boolean isInSecondStage() { return owner.hasData(ModAttachments.SECOND_STAGE); }

    public double getSecondStageThrottle() { return owner.getData(ModAttachments.SECOND_STAGE_THROTTLE); }

    public boolean isInMobileCharge() { return owner.hasData(ModAttachments.MOBILE_CHARGE); }

    public @Nullable Double getPlungeTopY() { return find(ModAttachments.PLUNGE); }

    public boolean isBorn() { return owner.getData(ModAttachments.BORN); }

    public boolean setAperture(@NotNull ApertureData value) { return set(ModAttachments.APERTURE, value); }

    public boolean setApertureStorage(@NotNull ApertureStorage value) {
        return set(ModAttachments.APERTURE_STORAGE, value);
    }

    public boolean setNourish(@NotNull ApertureNourishData value) {
        return set(ModAttachments.APERTURE_NOURISH, value);
    }

    public boolean setAwakeningRitual(@Nullable AwakeningRitual value) {
        return setOrRemove(ModAttachments.AWAKENING_RITUAL, value);
    }

    public boolean setBody(@NotNull BodyData value) { return set(ModAttachments.BODY, value); }

    public boolean setShield(@NotNull ShieldData value) { return set(ModAttachments.SHIELD, value); }

    public void setShieldWindow(@NotNull ShieldWindow value) { owner.setData(ModAttachments.SHIELD_WINDOW, value); }

    public boolean setSoul(@NotNull SoulData value) { return set(ModAttachments.SOUL, value); }

    public boolean setPath(@NotNull PathData value) { return set(ModAttachments.PATH, value); }

    public boolean setPathEffect(@NotNull PathEffectData value) { return set(ModAttachments.PATH_EFFECT, value); }


    public boolean setMind(@NotNull MindData value) { return set(ModAttachments.MIND, value); }

    public boolean setKillingMoves(@NotNull KillingMoveData value) {
        return set(ModAttachments.KILLING_MOVE, value);
    }

    public boolean setGuRecipeKnowledge(@NotNull GuRecipeKnowledgeData value) {
        return set(ModAttachments.GU_RECIPE_KNOWLEDGE, value);
    }

    public boolean setDimensionReturn(@NotNull DimensionReturnData value) {
        return set(ModAttachments.DIMENSION_RETURN, value);
    }

    public boolean setTossStash(@NotNull TossStashData value) { return set(ModAttachments.TOSS_STASH, value); }

    public boolean setRewind(@NotNull PlayerRewindData value) { return set(ModAttachments.PLAYER_REWIND, value); }

    public boolean setSecondStage(boolean value) {
        if (value == isInSecondStage()) return false;
        if (value) {
            owner.setData(ModAttachments.SECOND_STAGE, true);
        } else {
            owner.removeData(ModAttachments.SECOND_STAGE);
        }
        return true;
    }

    public boolean setMobileCharge(boolean value) {
        if (value == isInMobileCharge()) return false;
        if (value) {
            owner.setData(ModAttachments.MOBILE_CHARGE, true);
        } else {
            owner.removeData(ModAttachments.MOBILE_CHARGE);
        }
        return true;
    }

    public void setSecondStageThrottle(double value) {
        if (value > 0.0D) {
            owner.setData(ModAttachments.SECOND_STAGE_THROTTLE, value);
        } else {
            owner.removeData(ModAttachments.SECOND_STAGE_THROTTLE);
        }
    }

    public boolean setPlungeTopY(@Nullable Double value) { return setOrRemove(ModAttachments.PLUNGE, value); }

    public boolean setBorn(boolean value) { return set(ModAttachments.BORN, value); }

    public void setHealthCarry(float value) { owner.setData(ModAttachments.HEALTH_CARRY, value); }

    /** 取出并清除登出结转生命值，未设置时返回 {@code null}。 */
    public @Nullable Float takeHealthCarry() { return owner.removeData(ModAttachments.HEALTH_CARRY); }

    public void resetEssenceCarry() { owner.removeData(ModAttachments.ESSENCE_CARRY); }

    /** 将所有死亡继承类附件重置为默认值并同步（供未开启 {@code keepInventory} 的死亡或完全重置调用）。 */
    public void resetCarriedOnDeath() {
        for (AttachmentType<?> type : ModAttachments.getInherited()) owner.removeData(type);
    }

    private <T> @Nullable T find(Supplier<AttachmentType<T>> type) {
        return owner.hasData(type) ? owner.getData(type) : null;
    }

    private <T> boolean setOrRemove(Supplier<AttachmentType<T>> type, @Nullable T value) {
        if (Objects.equals(value, find(type))) return false;
        if (value == null) {
            owner.removeData(type);
        } else {
            owner.setData(type, value);
        }
        return true;
    }

    private <T> boolean set(Supplier<AttachmentType<T>> type, T value) {
        if (value.equals(owner.getData(type))) return false;
        owner.setData(type, value);
        return true;
    }
}
