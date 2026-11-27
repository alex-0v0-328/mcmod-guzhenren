package net.alex.guzhenren;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主代码分层的源码补查：读 {@code src/main/java} 每个文件的 import 与静态 import，按 {@link ArchitectureTest} 的同一张
 * 分层表（{@code LAYERS_ABOVE}）判下层有没有引上层，并查仙窍界面有没有引服务端运行类（{@code *Work}、{@code LandMaps}、
 * {@code BlissfulLandData}、根包）。
 * <ul>
 *   <li>⚠ 补的是字节码的盲区：别层的字符串等常量在编译期内联，类文件不留引用，ArchUnit 放过；源码里的 import 行还在。
 *       </li>
 *   <li>只认 import；代码里直接写全限定名不查。</li>
 * </ul>
 */
class SourceLayersTest {

    private static final Pattern IMPORT = Pattern.compile(
            "^import\\s+(?:static\\s+)?net\\.alex\\.guzhenren\\.([a-z_]+)\\.", Pattern.MULTILINE);

    @Test
    void lowerLayersNeverImportUpperOnes() throws IOException {
        Path root = Path.of(System.getProperty("gzr.projectDir"), "src/main/java/net/alex/guzhenren");
        List<String> breaches = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                scanned++;
                Path relative = root.relativize(file);
                if (relative.getNameCount() < 2) continue;
                List<String> above = ArchitectureTest.LAYERS_ABOVE.get(relative.getName(0).toString());
                if (above == null) continue;
                Matcher matcher = IMPORT.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (above.contains(matcher.group(1))) breaches.add(relative.toString().replace(file.getFileSystem()
                            .getSeparator(), "/") + " imports " + matcher.group(1));
                }
            }
        }
        assertTrue(scanned > 0, "no sources under " + root);
        assertEquals(List.of(), breaches, "a lower layer imports an upper one");
    }

    @Test
    void immortalApertureScreensNeverImportServerRuntime() throws IOException {
        Path root = Path.of(System.getProperty("gzr.projectDir"),
                "src/main/java/net/alex/guzhenren/client/screen/immortalaperture");
        Pattern runtime = Pattern.compile("^import\\s+(?:static\\s+)?net\\.alex\\.guzhenren\\.gameplay\\."
                + "immortalaperture\\.(?:(?:[a-z]+\\.)+(?:[A-Z]\\w*Work|LandMaps|BlissfulLandData)\\b|[A-Z])",
                Pattern.MULTILINE);
        List<String> breaches = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = runtime.matcher(Files.readString(file));
                while (matcher.find()) breaches.add(root.relativize(file) + ": " + matcher.group());
            }
        }
        assertEquals(List.of(), breaches, "an aperture screen imports server runtime");
    }
}
