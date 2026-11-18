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
 * FTB 连锁联动：当玩家按住连锁键并手持腐泥冻土右键方块时，将连锁选区内的全部坐标转发给
 * {@link RottenFrozenMudItem#convertEach} 批量改良为黑土。
 * <ul>
 *   <li>作为可选联动，仅当主类检测到 {@value #MOD_ID} 已加载时才调用 {@link #initialize}。</li>
 *   <li>全程使用反射与动态代理（{@link Proxy}）挂载处理器，不引入编译期依赖（因 FTB 连锁强依赖 FTB Library 与
 *       Architectury，若放入 {@code run/mods} 会影响配套仓库运行）；仅反射调用公开 API {@value #EVENT}、
 *       {@value #DISPATCHER}、{@value #HANDLER} 与 {@value #SHAPE}，返回实际转换方块数（返回 {@code 0} 表示不接
 *       管）。</li>
 *   <li>⚠ 右键处理器按注册先后顺序以「先到先得」方式拦截事件，因此 {@code neoforge.mods.toml} 必须声明本模组在 FTB 连锁
 *       之前加载，确保本处理器排在其内置的右键收割处理器之前（否则选区内只要包含成熟作物就会被收割逻辑截断）。</li>
 *   <li>⚠ 若未来 FTB 连锁更改公开 API 签名，{@link #initialize} 会捕获反射与运行时异常并仅输出一条警告日志，降级为普通
 *       单格右键而不影响模组启动。</li>
 * </ul>
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
