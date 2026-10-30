package net.alex.guzhenren.item.gu.mortal;

import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuModule;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.metal.BoneModule;
import net.alex.guzhenren.item.gu.mortal.metal.TendonModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shared module behind the body tempering Gu [炼体蛊]: makes one grade of one body part of the holder's -- a
 * skin [皮], tendons [筋] or bones [骨]. Which part comes from the subclass, which grade from registration; the Gu
 * itself is never consumed, so a holder who has outgrown it can hand it on. The two jade skins are instant defensive
 * Gu; every permanent grade is a channeled [灌注] dao mark carving Gu whose one round is the payout, after which the
 * gate refuses the next.
 *
 * <p>The gate asks the grade itself ({@link BodyGrade#canReplace}) whether the holder's current grade gives way,
 * and refuses with the part's own key and the worn grade's name; the effect is the subclass's {@link #temper}, one
 * write through {@code BodyService}. Every number and rule hangs off that write and the grade enum, not off this
 * module. {@code CarvingEffects} finds the Gu that makes a grade by this module.
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinModule
 * @see TendonModule
 * @see BoneModule
 * @since 1.0.0
 */

public abstract class TemperingModule<G extends Enum<G> & BodyGrade<G>> implements GuModule {

    private final G grade;
    private final String failedTakenKey;

    protected TemperingModule(G grade, String failedTakenKey) {
        this.grade = grade;
        this.failedTakenKey = failedTakenKey;
    }

    public @NotNull G getGrade() { return grade; }

    protected abstract @NotNull G getWorn(Player player);

    protected abstract void temper(MortalGuItem gu, ServerPlayer player);

    @Override
    public final @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        G worn = getWorn(player);
        return grade.canReplace(worn) ? null
                : new Refusal(failedTakenKey, Component.translatable(worn.getTranslationKey()));
    }

    @Override
    public final void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) { temper(gu, player); }
}
