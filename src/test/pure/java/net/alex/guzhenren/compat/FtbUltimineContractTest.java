package net.alex.guzhenren.compat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * FTB 连锁[FTB Ultimine]反射契约：{@link FtbUltimineIntegration} 不对它编译，这里读整合包镜像 {@code run/mods} 里的
 * FTB 连锁与 Architectury 两个 jar，核对联动按名字取的字段与方法签名还在；jar 不在（CI 没有整合包镜像）即跳过。整合包
 * 升级这两个模组后签名变了，本测试先红，不必等游戏里那条警告。
 */

public class FtbUltimineContractTest {

    private static final Path MODS = Path.of(System.getProperty("gzr.projectDir"), "run", "mods");

    @Test
    public void theReflectedRightClickApiKeepsItsSignatures() throws IOException {
        List<JarFile> jars = new ArrayList<>();
        for (String prefix : List.of("ftb-ultimine-neoforge-", "architectury-")) {
            Path jar = find(prefix);
            assumeTrue(jar != null, () -> "no " + prefix + "*.jar in " + MODS.toAbsolutePath());
            jars.add(new JarFile(jar.toFile()));
        }
        JavaClasses classes = new ClassFileImporter().importJars(jars);
        JavaClass event = classes.get(FtbUltimineIntegration.EVENT);

        assertEquals(FtbUltimineIntegration.ARCHITECTURY_EVENT, event.getField("REGISTER").getRawType().getName(),
                "REGISTER is an Architectury event");
        event.getMethod("register", FtbUltimineIntegration.DISPATCHER);
        classes.get(FtbUltimineIntegration.ARCHITECTURY_EVENT).getMethod("register", Object.class);
        classes.get(FtbUltimineIntegration.DISPATCHER).getMethod("registerHandler", FtbUltimineIntegration.HANDLER);
        assertEquals("int", classes.get(FtbUltimineIntegration.HANDLER).getMethod("handleRightClickBlock",
                        FtbUltimineIntegration.SHAPE, InteractionHand.class.getName(), Collection.class.getName())
                .getRawReturnType().getName(), "the handler returns the converted count");
        assertEquals(ServerPlayer.class.getName(), classes.get(FtbUltimineIntegration.SHAPE).getMethod("player")
                .getRawReturnType().getName(), "the shape context carries the server player");
    }

    private static Path find(String prefix) throws IOException {
        if (!Files.isDirectory(MODS)) return null;

        try (DirectoryStream<Path> jars = Files.newDirectoryStream(MODS, prefix + "*.jar")) {
            for (Path jar : jars) return jar;
        }
        return null;
    }
}
