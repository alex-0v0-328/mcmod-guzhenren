package net.alex.guzhenren.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestAssertException;

/**
 * 无场景取值检查用的 JUnit 式断言。供 GameTest 检查需模组已加载的注册对象、物品堆、编解码器；失败抛
 * {@link GameTestAssertException}，只败本测试，{@link AssertionError} 会逃出运行器使服务端崩溃；{@link #assertAll} 同
 * JUnit 一次报全部失败。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class UnitAsserts {

    private UnitAsserts() {}

    public static void assertTrue(boolean condition) {
        assertTrue(condition, "expected true");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    public static void assertTrue(boolean condition, Supplier<String> message) {
        if (!condition) throw new GameTestAssertException(message.get());
    }

    public static void assertFalse(boolean condition) {
        assertTrue(!condition, "expected false");
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, "");
    }

    public static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) throw mismatch(expected, actual, message);
    }

    public static void assertEquals(long expected, long actual, Supplier<String> message) {
        if (expected != actual) throw mismatch(expected, actual, message.get());
    }

    public static void assertEquals(double expected, double actual) {
        if (Double.compare(expected, actual) != 0) throw mismatch(expected, actual, "");
    }

    public static void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) throw mismatch(expected, actual, "within " + delta);
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "");
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw mismatch(expected, actual, message);
    }

    public static void assertSame(Object expected, Object actual) {
        if (expected != actual) throw mismatch(expected, actual, "not the same instance");
    }

    public static void assertNotSame(Object unexpected, Object actual) {
        assertTrue(unexpected != actual, () -> "expected another instance than " + actual);
    }

    public static void assertNull(Object actual) {
        assertTrue(actual == null, () -> "expected null, was " + actual);
    }

    public static <T> T assertNotNull(T actual, Supplier<String> message) {
        assertTrue(actual != null, message);
        return actual;
    }

    public static void assertAll(Runnable... checks) {
        List<String> failures = new ArrayList<>();
        for (Runnable check : checks) {
            try {
                check.run();
            } catch (GameTestAssertException e) {
                failures.add(e.getMessage());
            }
        }
        assertTrue(failures.isEmpty(), () -> failures.size() + " failures: " + String.join("; ", failures));
    }

    private static GameTestAssertException mismatch(Object expected, Object actual, String message) {
        String prefix = message.isEmpty() ? "" : message + " ==> ";
        return new GameTestAssertException(prefix + "expected <" + expected + "> but was <" + actual + ">");
    }
}
