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
 *       Gu World 的运行当模组加载、缺依赖即崩。碰的只有三个公开接口：{@value #EVENT} 的 {@code REGISTER}
 *       （Architectury 事件）、其 {@code Dispatcher#registerHandler}、{@value #HANDLER} 的
 *       {@code handleRightClickBlock}（第一个参数 {@value #SHAPE} 的 {@code player()}）。</li>
 *   <li>它每次开服前向登记者要处理器，故构造期登记一次即可；处理器返回转换的格数，0 表示不接手。</li>
 *   <li>⚠ 处理器先到先得，先后即登记先后：{@code neoforge.mods.toml} 声明本模组先于它构造，我们才排在它自带的收割
 *       处理器前。否则连锁点作物时，选区里有一株成熟作物就被收割截走。手里不是腐泥冻土时我们返回 0，收割照旧。</li>
 *   <li>⚠ 接口对不上（它改了 API）只记一条警告并跳过，单格右键照旧，不让模组起不来：登记时就按确切签名取方法，运行期
 *       异常也一并接住。</li>
 *   <li>代理自己答 {@code Object} 的方法和接口默认方法：它现在不调，属防御。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class FtbUltimineIntegration {

    public static final String MOD_ID = "ftbultimine";
    static final String EVENT = "dev.ftb.mods.ftbultimine.api.rightclick.RegisterRightClickHandlerEvent";
    static final String DISPATCHER = EVENT + "$Dispatcher";
    static final String HANDLER = "dev.ftb.mods.ftbultimine.api.rightclick.RightClickHandler";
    static final String SHAPE = "dev.ftb.mods.ftbultimine.api.shape.ShapeContext";
    static final String ARCHITECTURY_EVENT = "dev.architectury.event.Event";

    private FtbUltimineIntegration() {}

    public static void initialize() {
        try {
            Class<?> event = Class.forName(EVENT);
            Class<?> handler = Class.forName(HANDLER);
            Class<?> shape = Class.forName(SHAPE);
            Method player = shape.getMethod("player");
            Method handle = handler.getMethod("handleRightClickBlock", shape, InteractionHand.class, Collection.class);
            Method register = event.getMethod("register", Class.forName(DISPATCHER));
            Method registerHandler = Class.forName(DISPATCHER).getMethod("registerHandler", handler);
            if (handle.getReturnType() != int.class || !ServerPlayer.class.isAssignableFrom(player.getReturnType())) {
                throw new NoSuchMethodException(handle + " or " + player + " changed its return type");
            }
            Object chain = createProxy(handler, (proxy, method, arguments) -> method.equals(handle)
                    ? convert(player, arguments) : answer(proxy, method, arguments));
            Object listener = createProxy(event, (proxy, method, arguments) -> method.equals(register)
                    ? registerHandler.invoke(arguments[0], chain) : answer(proxy, method, arguments));
            Class.forName(ARCHITECTURY_EVENT).getMethod("register", Object.class)
                    .invoke(event.getField("REGISTER").get(null), listener);
            Guzhenren.LOGGER.info("FTB Ultimine chains the rotten frozen mud conversion");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
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
