package net.alex.guzhenren.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.alex.guzhenren.gameplay.path.strength.BeastStrength;
import net.alex.guzhenren.gameplay.path.strength.HumanStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /gzr path strength} 子命令：修改目标玩家肉身持有的兽力[Beast Strength]与人力，结果由 B 面板读取。
 * <ul>
 *   <li>支持兽力的赋予、移除与清空（{@code grant|revoke|clear}）以及人力的增减与设置
 *       （{@code human <kind> set|add|sub}，读取 {@code int} 参数后安全收窄）。</li>
 *   <li>所有变更统一经由 {@link PathStrengthService} 写入并同步刷新攻击伤害属性修饰符；若因异道道痕[Path Marks]互斥导致
 *       赋予兽力或增加人力被服务层拒绝，则向命令源反馈 {@link #FAILED_MARK_INTERFERENCE}。</li>
 * </ul>
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
