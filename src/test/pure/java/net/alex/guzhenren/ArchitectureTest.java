package net.alex.guzhenren;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 主代码架构规则：分层向下（{@link #LAYERS_ABOVE}，字节码看不见的内联常量由 {@code SourceLayersTest} 按 import 补查）；
 * 仅客户端专用代码引用客户端代码，仅 mixin 引用 mixin；共用蛊物品基类低于凡蛊；空窍[Aperture]
 * 压力爆炸坑低于空窍其余部分，仙窍[Immortal Aperture]状态层低于仙窍其余部分；顶层包无环，不计注册枢纽（{@code registry}
 * 与根包类）；二级包亦无环，另不计记录门面
 * {@code gameplay.data}；本命蛊[Vital Gu]绑定唯一读写入口。
 */
@AnalyzeClasses(packages = "net.alex.guzhenren", importOptions = ArchitectureTest.MainOutputOnly.class)
class ArchitectureTest {

    private static final String ROOT = "net.alex.guzhenren.";
    // Each layer and the top-level packages above it, shared with SourceLayersTest.
    static final Map<String, List<String>> LAYERS_ABOVE = Map.of(
            "gameplay", List.of("item", "command", "lifecycle", "refinement", "vault", "network"),
            "item", List.of("command", "vault"),
            "lifecycle", List.of("compat"));
    // Client-only classes outside the client package; the mixins come from the "client" list of guzhenren.mixins.json.
    private static final Set<String> CLIENT_CLASSES = Stream.concat(
            Stream.of(ROOT + "GuzhenrenClient", ROOT + "compat.EpicFightClientIntegration"),
            readClientMixins().stream()).collect(Collectors.toUnmodifiableSet());
    private static final DescribedPredicate<JavaClass> CLIENT_ONLY = resideInAPackage(ROOT + "client..")
            .or(DescribedPredicate.describe("are listed client classes",
                    c -> CLIENT_CLASSES.contains(c.getName().split("\\$")[0])))
            .as("are client-only");
    // Data generation runs only under runData, whose providers may read client model classes.
    private static final DescribedPredicate<JavaClass> RUN_DATA_ONLY = resideInAPackage(ROOT + "datagen..");
    private static final DescribedPredicate<JavaClass> REGISTRATION_HUB = resideInAPackage(ROOT + "registry..")
            .or(resideInAPackage("net.alex.guzhenren")).as("are registration hubs");
    private static final SliceAssignment SECOND_LEVEL_PACKAGES = new SliceAssignment() {

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String packageName = javaClass.getPackageName();
            if (!packageName.startsWith(ROOT)) return SliceIdentifier.ignore();

            String[] parts = packageName.substring(ROOT.length()).split("\\.");
            String first = parts[0];
            if (first.equals("registry") || first.equals("gameplay") && parts.length > 1 && parts[1].equals("data")) {
                return SliceIdentifier.ignore();
            }
            String strength = ROOT + "gameplay.path.strength";
            int depth = packageName.equals(strength) || packageName.startsWith(strength + ".") ? 3 : 2;
            return SliceIdentifier.of(String.join(".", Arrays.copyOf(parts, Math.min(depth, parts.length))));
        }

        @Override
        public String getDescription() { return "second-level packages, gameplay.path.strength apart"; }
    };
    private static final String VITAL_GU_STATE = ROOT + "item.gu.stack.VitalGuState";
    private static final String DATA_COMPONENTS = ROOT + "registry.item.ModDataComponents";
    private static final DescribedPredicate<JavaFieldAccess> VITAL_COMPONENT = DescribedPredicate.describe(
            "a Vital Gu component", access -> access.getTargetOwner().getName().equals(DATA_COMPONENTS)
                    && access.getTarget().getName().startsWith("VITAL_"));

    @ArchTest
    static final ArchRule gameplayStaysBelowTheLayersAboveIt = staysBelow("gameplay");
    @ArchTest
    static final ArchRule itemStaysBelowCommandsAndTheVault = staysBelow("item");
    @ArchTest
    static final ArchRule sharedItemsStayBelowMortalGu = noClasses()
            .that().haveFullyQualifiedName(ROOT + "item.GuItem").or().resideInAPackage(ROOT + "item.material..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "item.gu..")
            .because("GuItem holds only what Gu and Gu material share; the Vital Gu and rank pacing are mortal Gu's");
    @ArchTest
    static final ArchRule onlyVitalGuStateTouchesVitalComponents = noClasses()
            .that().doNotHaveFullyQualifiedName(VITAL_GU_STATE).and().doNotHaveFullyQualifiedName(DATA_COMPONENTS)
            .should().accessFieldWhere(VITAL_COMPONENT)
            .because("the Vital Gu binding is read and written only through VitalGuState");
    @ArchTest
    static final ArchRule lifecycleStaysBelowIntegrations = staysBelow("lifecycle");
    @ArchTest
    static final ArchRule recipesStayBelowTheRestOfRefinement = noClasses()
            .that().resideInAPackage(ROOT + "refinement.recipe..").should().dependOnClassesThat(
                    resideInAPackage(ROOT + "refinement..").and(not(resideInAPackage(ROOT + "refinement.recipe.."))));
    @ArchTest
    static final ArchRule pressureCratersStayBelowTheRestOfTheAperture = noClasses()
            .that().resideInAPackage(ROOT + "gameplay.aperture.pressure..").should().dependOnClassesThat(
                    resideInAPackage(ROOT + "gameplay.aperture..")
                            .and(not(resideInAPackage(ROOT + "gameplay.aperture.pressure.."))))
            .because("the aperture calls down into the crater runtime; a call back up would close a cycle");
    @ArchTest
    static final ArchRule immortalApertureStateStaysBelowTheRestOfTheAperture = noClasses()
            .that().resideInAPackage(ROOT + "gameplay.immortalaperture.state..").should().dependOnClassesThat(
                    resideInAPackage(ROOT + "gameplay.immortalaperture..")
                            .and(not(resideInAPackage(ROOT + "gameplay.immortalaperture.state.."))))
            .because("features, the map pipeline, the panel and the assembly sit on the land's state; the state "
                    + "tells them through events");
    @ArchTest
    static final ArchRule topLevelPackagesFormNoCycle = slices().matching(ROOT + "(*)..")
            .should().beFreeOfCycles()
            .ignoreDependency(REGISTRATION_HUB, DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), REGISTRATION_HUB)
            .as("top-level packages form no cycle")
            .because("registry and the root package classes are the registration hubs and are left out; a lower layer "
                    + "tells a higher one through ModEvents.post");
    @ArchTest
    static final ArchRule secondLevelPackagesFormNoCycle = slices().assignedFrom(SECOND_LEVEL_PACKAGES)
            .should().beFreeOfCycles()
            .because("registry and the root package classes are the registration hubs and gameplay.data is the "
                    + "facade over every record, so each is left out; gameplay.path.strength sits above the "
                    + "attribute, aperture and body packages that the base path package sits below");
    @ArchTest
    static final ArchRule commonCodeNeverReachesClientClasses = noClasses().that(not(CLIENT_ONLY).as("are common"))
            .should().dependOnClassesThat(CLIENT_ONLY)
            .because("a dedicated server must load every common class without client classes");
    @ArchTest
    static final ArchRule onlyClientClassesReachGameClientCode = noClasses()
            .that(not(CLIENT_ONLY).and(not(RUN_DATA_ONLY)).as("are common")).should().dependOnClassesThat()
            .resideInAnyPackage("net.minecraft.client..", "com.mojang.blaze3d..", "net.neoforged.neoforge.client..",
                    "com.lowdragmc.photon.client..");
    @ArchTest
    static final ArchRule onlyMixinsReachMixins = noClasses().that().resideOutsideOfPackage(ROOT + "mixin..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "mixin..");

    private static ArchRule staysBelow(String layer) {
        return noClasses().that().resideInAPackage(ROOT + layer + "..").should().dependOnClassesThat()
                .resideInAnyPackage(LAYERS_ABOVE.get(layer).stream().map(above -> ROOT + above + "..")
                        .toArray(String[]::new));
    }

    /** 只导入主输出：pureTest 类路径还带同包的 gametest 与 pureTest 类。 */
    static final class MainOutputOnly implements ImportOption {

        @Override
        public boolean includes(Location location) {
            return location.contains("/classes/java/main/");
        }
    }

    private static List<String> readClientMixins() {
        try (InputStream input = Objects.requireNonNull(ArchitectureTest.class.getResourceAsStream(
                "/guzhenren.mixins.json"), "guzhenren.mixins.json is missing");
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            String mixinPackage = config.get("package").getAsString();
            return config.getAsJsonArray("client").asList().stream().map(JsonElement::getAsString)
                    .map(name -> mixinPackage + "." + name).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
