package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Beast Phantom [兽力虚影] of the beast strength phantom path [兽力虚影流]: each melee hit a player with a
 * permanent beast strength lands has a {@link #CHANCE} to manifest one. It picks one of the player's beast families
 * at random -- the two boars are one family and bring both their points -- and for that one hit the family's points
 * count in full past the carrying capacity ({@link StrengthService#getPhantomBonusPoints}), added to the hit at
 * {@link PathStrengthData#ATTACK_PER_POINT} a point before armor. A {@link BeastPhantomEntity} shows it.
 *
 * <p>Only one beast is ever at full strength and only one phantom up: while the last one is still shown no hit rolls.
 * There is no cooldown past that and no load. A phantom manifests even when nothing was cut, and then adds nothing.
 *
 * <p>Reads {@link LivingIncomingDamageEvent} with the player as the DIRECT entity, like the load
 * ({@code StrengthLoadEvents}): an arrow, a Moon Blade, a Qi Wave or the area impact [范围冲击] carries none of the
 * body's own strength. A hit the target's hurt frames would swallow does not roll. Temporary beast buffs (Flower
 * Boar, the ox) are not permanent beasts and never manifest. {@link #manifest} takes the roll as a seam for tests.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @see StrengthService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class BeastPhantomEvents {

    private BeastPhantomEvents() {}

    public static final float CHANCE = 0.05F;
    private static final int HURT_FRAME_GATE = 10;

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (!(source.getDirectEntity() instanceof ServerPlayer player) || target == player) return;
        if (source.is(ModDamageTypes.AREA_IMPACT) || event.getAmount() <= 0.0F || !target.isAlive()) return;
        if (target.invulnerableTime > HURT_FRAME_GATE && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return;

        float bonus = manifest(player, player.getRandom().nextFloat());
        if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
    }

    public static float manifest(ServerPlayer player, float roll) {
        if (roll >= CHANCE || BeastPhantomEntity.findOwned(player) != null) return 0.0F;
        BeastStrengthFamily family = pickFamily(PathStrengthService.get(player), player.getRandom());
        if (family == null) return 0.0F;

        RandomSource random = player.getRandom();
        BeastPhantomEntity.summon(player, family, random.nextBoolean());
        int bonus = StrengthService.getPhantomBonusPoints(StrengthService.getCapacity(player),
                StrengthService.getStrengthPoints(player), PathStrengthService.get(player).getBeastPoints(family));
        return (float) (bonus * PathStrengthData.ATTACK_PER_POINT);
    }

    public static @Nullable BeastStrengthFamily pickFamily(PathStrengthData data, RandomSource random) {
        List<BeastStrengthFamily> families = new ArrayList<>(data.getBeastReadings().keySet());
        return families.isEmpty() ? null : families.get(random.nextInt(families.size()));
    }
}
