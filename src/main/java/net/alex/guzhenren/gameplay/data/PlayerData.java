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
 * 本模组存于玩家的全部数据，经一个对象读写。
 * <ul>
 *   <li>{@link #get} 是两端唯一入口，setter 返回是否改变；⚠ 唯一碰附件 API 的类（NeoForge 附件的视图），服务只用这些方
 *       法。</li>
 *   <li>存档 16 个；二段飞行[Second-Stage Flight]标记、坠击起点高度同步不存档；内存 3 个：真元[Essence]余数
 *       （{@link #getEssenceCarry}）、二段油门（{@link #getSecondStageThrottle}）两端各推，护盾无敌窗口
 *       （{@link #getShieldWindow}）仅服务端。</li>
 *   <li>12 个值变更即同步本人（{@code ModAttachments} 用 {@code OWNER_ONLY}）；二段标记、温养空窍[Nourish Aperture]进
 *       程、开窍[Awakening]仪式同步所有可见客户端。</li>
 *   <li>⚠ 4 个值底层是附件有无：二段标记有即开，坠击起点高度仅坠击中非 {@code null}，二者重登或死亡即丢；开窍仪式仅仪式
 *       中非 {@code null}；结转生命值存或无，{@link #takeHealthCarry} 取即清。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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

    public DimensionReturnData getDimensionReturn() { return owner.getData(ModAttachments.DIMENSION_RETURN); }

    public TossStashData getTossStash() { return owner.getData(ModAttachments.TOSS_STASH); }

    public PlayerRewindData getRewind() { return owner.getData(ModAttachments.PLAYER_REWIND); }

    public boolean isInSecondStage() { return owner.hasData(ModAttachments.SECOND_STAGE); }

    public double getSecondStageThrottle() { return owner.getData(ModAttachments.SECOND_STAGE_THROTTLE); }

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

    /** 取出并清除结转生命值；无则 {@code null}。 */
    public @Nullable Float takeHealthCarry() { return owner.removeData(ModAttachments.HEALTH_CARRY); }

    public void resetEssenceCarry() { owner.removeData(ModAttachments.ESSENCE_CARRY); }

    /** 死亡继承值全部复位并同步；未开 keepInventory 的死亡重置。 */
    public void resetCarriedOnDeath() {
        for (AttachmentType<?> type : ModAttachments.getInherited()) owner.removeData(type);
    }

    private <T> @Nullable T find(Supplier<AttachmentType<T>> type) {
        return owner.hasData(type) ? owner.getData(type) : null;
    }

    /** 存入 {@code value}，{@code null} 则移除附件；有变返回 true。 */
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
