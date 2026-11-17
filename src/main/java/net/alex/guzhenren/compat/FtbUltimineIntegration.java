package net.alex.guzhenren.compat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.item.material.RottenFrozenMudItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/**
 * FTB 连锁[FTB Ultimine]联动：按住连锁键、手持腐泥冻土右键时，把连锁选中的每一格交给
 * {@link RottenFrozenMudItem#convertEach}。
 *
 * <ul>
 *   <li>可选模组：主类只在 {@value #MOD_ID} 已加载时调 {@link #initialize}，否则本类不加载。</li>
 *   <li>全程反射，不对它编译：它自己强制依赖 FTB Library 与 Architectury，编译用的 jar 放进 {@code run/mods} 会被
 *       Gu World 的运行当模组加载、缺依赖即崩。碰的只有三个公开接口：{@code RegisterRightClickHandlerEvent.REGISTER}
 *       （Architectury 事件）、其 {@code Dispatcher#registerHandler}、{@code RightClickHandler#handleRightClickBlock}
 *       （第一个参数 {@code ShapeContext#player()}）。</li>
 *   <li>它在服务器启动时才向登记者要处理器，故构造期登记不嫌早；处理器返回转换的格数，0 表示不接手，它再问下一个
 *       处理器或放行原版右键。</li>
 *   <li>⚠ 接口对不上（它改了 API）只记一条警告并跳过：单格右键照旧，不让模组起不来。</li>
 *   <li>⚠ 代理要自己答 {@code Object} 的方法和接口默认方法，漏了它把处理器放进集合时会抛错。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class FtbUltimineIntegration {

    public static final String MOD_ID = "ftbultimine";
    private static final String EVENT = "dev.ftb.mods.ftbultimine.api.rightclick.RegisterRightClickHandlerEvent";
    private static final String HANDLER = "dev.ftb.mods.ftbultimine.api.rightclick.RightClickHandler";
    private static final String SHAPE = "dev.ftb.mods.ftbultimine.api.shape.ShapeContext";
    private static final String ARCHITECTURY_EVENT = "dev.architectury.event.Event";

    private FtbUltimineIntegration() {}

    public static void initialize() {
        try {
            Class<?> event = Class.forName(EVENT);
            Class<?> handler = Class.forName(HANDLER);
            Method player = Class.forName(SHAPE).getMethod("player");
            Method registerHandler = Class.forName(EVENT + "$Dispatcher").getMethod("registerHandler", handler);
            Object chain = createProxy(handler, (proxy, method, arguments) -> method.getName()
                    .equals("handleRightClickBlock") ? convert(player, arguments) : answer(proxy, method, arguments));
            Object listener = createProxy(event, (proxy, method, arguments) -> method.getName().equals("register")
                    ? registerHandler.invoke(arguments[0], chain) : answer(proxy, method, arguments));
            Class.forName(ARCHITECTURY_EVENT).getMethod("register", Object.class)
                    .invoke(event.getField("REGISTER").get(null), listener);
            Guzhenren.LOGGER.info("FTB Ultimine chains the rotten frozen mud conversion");
        } catch (ReflectiveOperationException | LinkageError e) {
            Guzhenren.LOGGER.warn("FTB Ultimine is loaded but its right-click API does not match; "
                    + "the rotten frozen mud converts one block per click", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convert(Method player, Object[] arguments) throws ReflectiveOperationException {
        return RottenFrozenMudItem.convertEach((ServerPlayer) player.invoke(arguments[0]),
                (InteractionHand) arguments[1], (Collection<BlockPos>) arguments[2]);
    }

    private static Object createProxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object answer(Object proxy, Method method, Object[] arguments) throws Throwable {
        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, arguments);

        return switch (method.getName()) {
            case "equals" -> proxy == arguments[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> FtbUltimineIntegration.class.getSimpleName() + "@" + System.identityHashCode(proxy);
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }
}
