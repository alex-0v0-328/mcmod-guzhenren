package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path strength}：写身体持有的兽力[Beast Strength]与人力，B 面板读取。
 *
 * <ul>
 *   <li>兽力 {@code grant|revoke|clear}；人力 {@code set|add|sub}，读 {@code int} 参数，{@code (int)} 收窄精确。</li>
 *   <li>写入全经 {@link PathStrengthService}，它也是刷新攻击修饰符的唯一入口。</li>
 *   <li>服务因道痕互斥拒绝的 {@code grant} 或人力上升，对该玩家报 {@code FAILED_MARK_INTERFERENCE}，不写入。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.command.ModEnumArgument
 * @since 1.0.0
 */

public final class CmdStrength {

    private CmdStrength() {}

    private static final String ARG_KIND = "kind";
    private static final String FAILED_MARK_INTERFERENCE = "guzhenren.command.failed.mark_interference";

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode() {
        return Commands.literal("strength")
                .then(buildBeastNode("grant", PathStrengthService::grant))
                .then(buildBeastNode("revoke", (player, beast) -> {
                    PathStrengthService.revoke(player, beast);
                    return true;
                }))
                .then(buildHumanStrength())
                .then(ModCommandSupport.withTargets(Commands.literal("clear"),
                        context -> ModCommandSupport.apply(context, PathStrengthService::clear)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildBeastNode(
            String literal, ModCommandSupport.EnumResultOperation<BeastStrength> operation) {
        return Commands.literal(literal).then(ModCommandSupport.withTargets(
                ModEnumArgument.createArgument(ModCommandSupport.ARG_VALUE, BeastStrength.values()),
                context -> {
                    BeastStrength beast = ModEnumArgument.get(
                            context, ModCommandSupport.ARG_VALUE, BeastStrength.values());
                    return ModCommandSupport.applyIfResult(context, ModCommandSupport.ANYONE, FAILED_MARK_INTERFERENCE,
                            player -> operation.apply(player, beast));
                }));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> buildHumanStrength() {
        return Commands.literal("human").then(ModCommandSupport.buildEnumCounter(ARG_KIND, HumanStrength.values(),
                IntegerArgumentType.integer(),
                (player, kind, amount) -> PathStrengthService.setHumanStrength(player, kind, (int) amount),
                (player, kind, amount) -> PathStrengthService.addHumanStrength(player, kind, (int) amount),
                FAILED_MARK_INTERFERENCE));
    }
}
