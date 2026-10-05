package net.alex.guzhenren.gameplay.body;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.function.Predicate;
import net.alex.guzhenren.command.ModCommandSupport;
import net.alex.guzhenren.command.ModEnumArgument;
import net.alex.guzhenren.gameplay.aperture.AwakenedGate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * {@code /gzr body}: writes body [肉身] state -- physiques, race, lifespan and age; {@code /gzr info}
 * reads it.
 *
 * <p>Assembles the body subtree under {@code /gzr body}: physiques, race, lifespan and age. The
 * path domain writes live under {@code /gzr path}. All of {@code /gzr body} is ungated -- a
 * mortal ages and changes form too -- except {@code extreme set}, which needs an awakened target.
 *
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModCommandSupport
 * @since 1.0.0
 */

public final class CmdBody {

    private CmdBody() {}

    public static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("body")
                .then(physique())
                .then(ModCommandSupport.enumSetNode("race", Race.values(),
                        BodyService::setRace, ModCommandSupport.ANYONE, null))
                .then(ModCommandSupport.counter("lifespan", BodyService::setLifespan, BodyService::addLifespan))
                .then(ModCommandSupport.counter("age", BodyService::setAge, BodyService::addAge));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> physique() {
        return Commands.literal("physique")
                .then(enumAction("add", Physique.values(), BodyService::addPhysique,
                        value -> value != Physique.EXTREME, player -> true,
                        ModCommandSupport.FAILED_EXTREME))
                .then(enumAction("remove", Physique.values(), BodyService::removePhysique, value -> true,
                        player -> true, null))
                .then(Commands.literal("extreme")
                        .then(enumAction("set", ExtremePhysique.settable(), BodyService::setExtremePhysique,
                                value -> true, AwakenedGate.AWAKENED,
                                ModCommandSupport.FAILED_UNAWAKENED)));
    }

    private static <E extends Enum<E> & StringRepresentable> ArgumentBuilder<CommandSourceStack, ?> enumAction(
            String literal, E[] values, ModCommandSupport.EnumOperation<E> operation,
            Predicate<E> valueAllowed, Predicate<ServerPlayer> allowed, String refusedKey) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.arg(ModCommandSupport.ARG_VALUE, values), context -> {
                    E value = ModEnumArgument.get(context, ModCommandSupport.ARG_VALUE, values);
                    boolean valid = valueAllowed.test(value);
                    Predicate<ServerPlayer> gate = !valid
                            ? player -> false : allowed;
                    String key = !valid
                            ? ModCommandSupport.FAILED_EXTREME : refusedKey;
                    return ModCommandSupport.applyIf(context, gate, key, player -> operation.apply(player, value));
                }));
    }
}
