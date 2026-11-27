package net.alex.guzhenren.item.gu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.timed.BruteForceLonghornBeetleGuEffect;
import net.alex.guzhenren.effect.timed.FlowerBoarGuEffect;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.strength.HumanStrengthBehavior;
import net.alex.guzhenren.item.gu.tending.ConsumedGuItem;
import net.alex.guzhenren.item.gu.tending.GuHunger;
import net.alex.guzhenren.item.gu.tending.TendedGuAccess;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.alex.guzhenren.item.material.GuMaterialItem;
import net.alex.guzhenren.item.material.ImmortalMaterialItem;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import static net.alex.guzhenren.gametest.UnitAsserts.assertEquals;
import static net.alex.guzhenren.gametest.UnitAsserts.assertNotNull;
import static net.alex.guzhenren.gametest.UnitAsserts.assertTrue;

/**
 * 玩家 wiki 设定库物品表与已启动物品注册的对照测试。
 * <ul>
 *   <li>每个注册物品恰属一个已记录分组；每只蛊的转数、道[Path]、类型、真元[Essence]消耗、饱食、进食单位，
 *       及冷却与效果时长；仅当 GameTest 服务端收到 {@code -PwikiDir} 时校验，否则直接通过；{@link #run} 把解析器的
 *       {@link AssertionError} 转为 GameTest 失败。</li>
 *   <li>蛊系列由相邻分转表与共享字段组成，先拼接再校验；{@link Table#fits} 按字段把系列归入饱食类别，
 *       因页面只靠数值体现类别；覆盖检查拒绝缺失或重复的蛊/转条目；汇总计数与技术时钟另有页面；
 *       单份食物回复量与回复时钟是全页规则，所有蛊同值。</li>
 *   <li>单元格按页面原文比较：{@link #perFood} 为一份食物回复的饱食点，整数，否则
 *       {@code units/per}；{@link #lines} 按 {@code <br>} 拆行，去掉行首的 {@code 标签：}；{@link #cooldownCell} 为
 *       N/A，否则取效果与物品冷却较长者。</li>
 *   <li>冷却中可再催、用后消失是效果行，页面只在成立时写，由 {@link #effectFlag} 读；{@link #typeCell}
 *       按序列中文类型名，全同只写一个，无则写“无”；{@link #pathCell} 为道名去掉末尾“道”。</li>
 * </ul>
 */

@GameTestHolder(Guzhenren.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WikiItemsGameTests {

    private static final String ITEMS_PAGE = "玩家向/1.0.0/蛊真人Mod-蛊虫.md";
    private static final String MECHANISM_DIR = "玩家向/1.0.0/蛊虫机制";
    private static final String MECHANISM_SUFFIX = "道蛊虫.md";
    private static final String TIME_PAGE = "开发向-guzhenren/玩家资料维护.md";
    private static final String TOTAL_PAGE = "玩家向/1.0.0/蛊真人Mod-总表.md";
    private static final List<String> BRANCH_ROWS = List.of("一次性", "灌注型", "即时型");
    private static final String RECAST = "冷却中可再催，饱食 ×2";
    private static final String NO_RECAST = "不可再催";
    private static final String GONE = "用后消失";
    private static final List<Group> GROUPS = groups();
    private static Map<String, WikiTable.Row> mechanisms = Map.of();
    private static final Map<String, String> ZH_CN = loadZhCn();

    private WikiItemsGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void everyRegisteredItemHasExactlyOneDocumentedGroup(GameTestHelper helper) {
        run(helper, WikiItemsGameTests::checkItemGroups);
    }

    @GameTest(template = "empty9x9x9")
    public static void guRanksCostsHungerAndRepairUnitsMatchRegistrations(GameTestHelper helper) {
        run(helper, WikiItemsGameTests::checkRanksCostsHungerAndRepair);
    }

    @GameTest(template = "empty9x9x9")
    public static void cooldownAndSelectedDurationsMatchCode(GameTestHelper helper) {
        run(helper, WikiItemsGameTests::checkCooldownsAndDurations);
    }

    private static void run(GameTestHelper helper, WikiCheck check) {
        String configured = System.getProperty("gzr.wikiDir");
        if (configured != null) {
            assertTrue(!configured.isBlank(), "wikiDir must not be blank");
            Path wiki = Path.of(configured).toAbsolutePath().normalize();
            assertTrue(Files.isDirectory(wiki), () -> "Wiki directory does not exist: " + wiki);
            try {
                check.run(wiki);
            } catch (AssertionError | IOException e) {
                throw new GameTestAssertException(e.getMessage());
            }
        }
        helper.succeed();
    }

    private static void checkItemGroups(Path wiki) throws IOException {
        List<String> registered = ModItems.ITEMS.getEntries().stream().map(h -> h.getId().getPath()).toList();
        WikiTable.requireCoverage(GROUPS.stream().flatMap(g -> g.ids().stream()).toList(), registered);
        WikiTable page = page(wiki, ITEMS_PAGE);
        List<String> expectedEntries = GROUPS.stream().filter(g -> g.table() != Table.MATERIAL)
                .flatMap(g -> java.util.stream.IntStream.range(0, g.ids().size())
                        .mapToObj(i -> entryKey(g, i))).toList();
        List<String> documentedEntries = page.guRows().stream()
                .map(r -> documentedKey(r.cell("蛊虫"), r.cell("转"))).toList();
        WikiTable.requireCoverage(documentedEntries, expectedEntries);
        Map<String, WikiTable.Row> effects = mechanisms(wiki);
        WikiTable.requireCoverage(List.copyOf(effects.keySet()), expectedEntries);
        for (WikiTable.Row row : page.guRows()) {
            WikiTable.Row effect = effects.get(documentedKey(row.cell("蛊虫"), row.cell("转")));
            assertEquals(row.cell("类型"), effect.cell("类型"), effect.location() + " type");
            String file = Path.of(effect.location().substring(0, effect.location().lastIndexOf(':')))
                    .getFileName().toString();
            assertEquals(row.cell("道") + MECHANISM_SUFFIX, file, effect.location() + " path page");
        }
        WikiTable total = page(wiki, TOTAL_PAGE);
        total.requireExactRows(Table.MATERIAL.headers, GROUPS.stream().filter(g -> g.table() == Table.MATERIAL)
                .map(Group::label).toList());
        for (Group group : GROUPS) {
            if (group.table() == Table.MATERIAL) {
                total.row(Table.MATERIAL.headers, group.label()).expect("数量", List.of((long) group.ids().size()));
            }
        }
        long gu = ModItems.ITEMS.getEntries().stream().filter(h -> h.get() instanceof MortalGuItem).count();
        long material = ModItems.ITEMS.getEntries().stream().filter(h -> h.get() instanceof GuMaterialItem).count();
        long immortal = ModItems.ITEMS.getEntries().stream()
                .filter(h -> h.get() instanceof ImmortalMaterialItem).count();
        total.row("注册", "全部物品").expect("数量", List.of((long) registered.size()));
        total.row("注册", "蛊虫").expect("数量", List.of(gu));
        total.row("注册", "蛊材").expect("数量", List.of(material));
        total.row("注册", "仙材").expect("数量", List.of(immortal));
        total.row("注册", "其他（钢锭、钢块、知心草种子、黑土耕地、元泉流体方块、纯水桶、杀招书、书册、蛊方、进度图标、服饰）")
                .expect("数量", List.of(registered.size() - gu - material - immortal));
        List<MortalGuItem> items = guItems();
        long tended = items.stream().filter(TendedGuItem.class::isInstance).count();
        long channeled = items.stream().filter(g -> g.guProperties.isChanneled()).count();
        long consumed = items.stream().filter(ConsumedGuItem.class::isInstance).count();
        List<Long> branches = List.of(gu - tended, channeled, tended - channeled);
        for (int i = 0; i < BRANCH_ROWS.size(); i++) {
            total.row("分支", BRANCH_ROWS.get(i)).expect("数量", List.of(branches.get(i)));
        }
        total.expectMatch("需照顾蛊 = 灌注型 \\+ 即时型：(\\d+) 只", List.of(tended));
        total.expectMatch("成功使用后消失：(\\d+) 只", List.of(consumed));
        for (Table table : Table.values()) {
            if (table.title != null) total.row("饱食类别", table.title).expect("数量", List.of(count(table)));
        }
    }

    private static void checkRanksCostsHungerAndRepair(Path wiki) throws IOException {
        Map<String, MortalGuItem> registered = ModItems.ITEMS.getEntries().stream()
                .filter(h -> h.get() instanceof MortalGuItem)
                .collect(Collectors.toMap(h -> h.getId().getPath(), h -> (MortalGuItem) h.get()));
        WikiTable page = page(wiki, ITEMS_PAGE);
        mechanisms = mechanisms(wiki);
        List<String> channelTypes = new ArrayList<>();
        List<String> foodPoints = new ArrayList<>();
        for (Group group : GROUPS) {
            Table table = group.table();
            if (table == Table.MATERIAL) continue;
            List<MortalGuItem> items = group.ids().stream().map(id -> {
                MortalGuItem item = registered.get(id);
                assertNotNull(item, () -> ITEMS_PAGE + " missing registered Gu mapping " + id);
                return item;
            }).toList();
            WikiTable.Row row = groupRow(page, group);
            row.expect("转", values(items, g -> g.getRank().ordinal()));
            assertEquals(pathCell(items), row.cell("道"), row.location() + " path");
            if (table == Table.CHANNEL) channelTypes.add(typeCell(items));
            else assertEquals(typeCell(items), row.cell("类型"), row.location() + " type");
            if (table == Table.ONE_SHOT) {
                assertTrue(items.stream().noneMatch(TendedGuItem.class::isInstance), row.location());
                assertEquals(List.of(0L), values(items, MortalGuItem::getRefineCost), row.location() + " refine cost");
                continue;
            }
            assertTrue(items.stream().allMatch(TendedGuItem.class::isInstance), row.location());
            assertTrue(items.stream().allMatch(g -> g.guProperties.isChanneled() == (table == Table.CHANNEL)), row.location());
            List<TendedGuItem> tended = items.stream().map(TendedGuItem.class::cast).toList();
            List<GuHunger> hungers = items.stream().map(g -> g.guProperties.buildHunger()).toList();
            assertTrue(IntStream.range(0, items.size()).allMatch(i -> table.fits(tended.get(i), hungers.get(i))),
                    row.location() + " does not fit " + table);
            row.expect("炼化真元", values(items, MortalGuItem::getRefineCost));
            row.expect("使用真元", values(items, WikiItemsGameTests::useEssence));
            if (table.headers.contains("食物")) items.forEach(g -> foodPoints.add(perFood(g)));
            switch (table) {
                case CHANNEL -> {
                    List<GuHunger.HungerBar> bars = bars(hungers);
                    row.expect("每次扣", values(items, g -> g.guProperties.getHungerPerRound()));
                    row.expect("每天扣", collapse(bars.stream().map(b -> (long) b.perDay()).toList()));
                    row.expect("饱食上限", collapse(bars.stream().map(b -> (long) b.max()).toList()));
                    HumanStrengthBehavior human = items.getFirst().getBehavior(HumanStrengthBehavior.class);
                    if (human != null) {
                        String cap = "最高叠加 " + human.kind().getMaxLayers() + " 层";
                        assertTrue(Arrays.asList(mechanisms.get(entryKey(group, 0)).cell("效果").split("<br>"))
                                        .contains(cap),
                                row.location() + " layer cap");
                    }
                }
                case HIGH, LOW -> {
                    List<GuHunger.HungerBar> bars = bars(hungers);
                    row.expect("每次扣", collapse(bars.stream().map(b -> (long) b.perUse()).toList()));
                    row.expect("每天扣", collapse(bars.stream().map(b -> (long) b.perDay()).toList()));
                    row.expect("饱食上限", collapse(bars.stream().map(b -> (long) b.max()).toList()));
                    assertEquals(collapse(tended.stream().map(WikiItemsGameTests::recastCell).toList()),
                            lines(row.cell("冷却中再催")), row.location() + " recast");
                }
                case SHELF -> {
                    List<GuHunger.HungerBar> bars = bars(hungers);
                    row.expect("饱食上限", collapse(bars.stream().map(b -> (long) b.max()).toList()));
                    row.expect("每天扣", collapse(bars.stream().map(b -> (long) b.perDay()).toList()));
                    assertEquals(List.of("消失"), lines(row.cell("用后")), row.location() + " after use");
                }
                case RECOVER -> {
                    List<GuHunger.RecoverBar> bars = hungers.stream().map(GuHunger.RecoverBar.class::cast).toList();
                    row.expect("每次扣", collapse(bars.stream().map(b -> (long) b.perUse()).toList()));
                    row.expect("饱食上限", collapse(bars.stream().map(b -> (long) b.max()).toList()));
                    List<String> clock = collapse(bars.stream()
                            .map(b -> String.format("每 %,d tick 回 1", b.ticksPerPoint())).toList());
                    assertEquals(1, clock.size(), row.location() + " recover clocks differ " + clock);
                    WikiTable.Row rule = page.row("饱食类别", table.title);
                    assertTrue(rule.cell("日常变化").contains(clock.getFirst()), rule.location() + " recover clock");
                }
                case NO_CLOCK -> assertEquals(collapse(items.stream()
                                .map(g -> g instanceof ConsumedGuItem ? "消失" : "保留").toList()),
                        lines(row.cell("用后")), row.location() + " after use");
                default -> throw new AssertionError("Unchecked table " + table);
            }
        }
        List<String> food = collapse(foodPoints);
        assertTrue(food.size() == 1 && food.getFirst().matches("\\d+"), ITEMS_PAGE + " per food is not fixed " + food);
        page.expectMatch("每份食物固定回 \\*\\*(\\d+)\\*\\* 点", List.of(Long.parseLong(food.getFirst())));
        List<Long> stacks = values(List.copyOf(registered.values()), g -> new ItemStack(g).getMaxStackSize());
        assertEquals(List.of(1L), stacks, "Gu stack limits");
        page.expectMatch("所有蛊虫堆叠上限 (\\d+)", stacks);
        // The channeled table drops the type column because every one shares it, named once above the table.
        assertEquals(1L, channelTypes.stream().distinct().count(), ITEMS_PAGE + " channeled types " + channelTypes);
        String line = "灌注类：" + channelTypes.getFirst();
        assertTrue(Files.readString(wiki.resolve(ITEMS_PAGE)).lines().anyMatch(line::equals),
                ITEMS_PAGE + " missing line " + line);
    }

    private static String entryKey(Group group, int index) {
        String[] names = group.label().split("<br>");
        String name = names.length == group.ids().size() ? names[index] : group.label();
        int rank = gu(group.ids().get(index)).getRank().ordinal();
        return documentedKey(name, String.valueOf("一二三四五".charAt(rank - 1)));
    }

    private static String documentedKey(String name, String rank) {
        return (name.endsWith("蛊") ? name.substring(0, name.length() - 1) : name) + "/" + rank;
    }

    // Rebuild a series' value lists from its documented per-rank rows; no values come from registrations.
    private static WikiTable.Row groupRow(WikiTable page, Group group) {
        List<WikiTable.Row> entries = page.guRows();
        List<WikiTable.Row> selected = IntStream.range(0, group.ids().size()).mapToObj(i -> {
            String key = entryKey(group, i);
            List<WikiTable.Row> found = entries.stream()
                    .filter(r -> documentedKey(r.cell("蛊虫"), r.cell("转")).equals(key)).toList();
            assertEquals(1, found.size(), ITEMS_PAGE + " missing or duplicate entry " + key);
            return found.getFirst();
        }).toList();
        List<String> headers = new ArrayList<>(group.table().headers);
        List<String> cells = headers.stream().map(field -> switch (field) {
            case "蛊", "蛊虫" -> group.label();
            case "效果", "一轮结果" -> "见本节";
            case "冷却中再催" -> effectFlag(selected, RECAST, RECAST, NO_RECAST);
            case "用后" -> effectFlag(selected, GONE, "消失", "保留");
            default -> String.join("<br>", collapse(selected.stream().map(r -> r.cell(field)).toList()));
        }).toList();
        WikiTable.Row first = selected.getFirst();
        return new WikiTable.Row(first.location(), first.headerLocation(), first.section(), headers, cells);
    }

    private static String effectFlag(List<WikiTable.Row> rows, String line, String present, String absent) {
        return String.join("<br>", collapse(rows.stream()
                .map(r -> Arrays.asList(mechanisms.get(documentedKey(r.cell("蛊虫"), r.cell("转")))
                        .cell("效果").split("<br>")).contains(line) ? present : absent).toList()));
    }

    private static List<GuHunger.HungerBar> bars(List<GuHunger> hungers) {
        return hungers.stream().map(GuHunger.HungerBar.class::cast).toList();
    }

    // A drive pays the properties' cost, which is also its essence gate. Little Light's switch replaces the drive and gates
    // at 0, so it pays nothing; the amplifier also gates at 0 but is never used alone, and billAsPartner pays the properties' cost.
    private static long useEssence(MortalGuItem gu) {
        if (gu.guProperties.isChanneled() || gu instanceof AmplifyEffectGuItem) return gu.guProperties.getEssencePerRound();
        return TendedGuAccess.useThreshold((TendedGuItem) gu, new ItemStack(gu));
    }

    private static String recastCell(TendedGuItem gu) {
        if (gu.guProperties.getEffectCooldownTicks() == 0 && gu.guProperties.getItemCooldownTicks() == 0) return NO_RECAST;
        return TendedGuAccess.canUseDuringEffectCooldown(gu) ? RECAST : NO_RECAST;
    }

    private static String perFood(MortalGuItem gu) {
        int units = gu.guProperties.getFeedUnits();
        int per = gu.guProperties.getUnitsPerHunger();
        return units % per == 0 ? String.valueOf(units / per) : units + "/" + per;
    }

    private static List<String> lines(String cell) {
        return Arrays.stream(cell.split("<br>")).map(line -> line.substring(line.indexOf('：') + 1).trim()).toList();
    }

    private static void checkCooldownsAndDurations(Path wiki) throws IOException {
        WikiTable page = page(wiki, ITEMS_PAGE);
        mechanisms = mechanisms(wiki);
        for (Group group : GROUPS) {
            if (!group.table().headers.contains("冷却")) continue;
            WikiTable.Row row = groupRow(page, group);
            assertEquals(collapse(group.ids().stream().map(id -> cooldownCell(gu(id))).toList()),
                    lines(row.cell("冷却")), row.location() + " cooldown");
        }
        WikiTable time = page(wiki, TIME_PAGE);
        time.expectMatch("`HALF_SECOND (\\d+) · SECOND (\\d+) · HALF_MINUTE (\\d+)"
                        + " · MINUTE (\\d+) · DAY (\\d+) · HALF_DAY (\\d+)`",
                List.of((long) Ticks.HALF_SECOND, (long) Ticks.SECOND, (long) Ticks.HALF_MINUTE,
                        (long) Ticks.MINUTE, (long) Ticks.DAY, (long) Ticks.HALF_DAY));
        time.row("什么", "花豕 / 蛮力天牛").expectValue("duration seconds",
                time.row("什么", "花豕 / 蛮力天牛").cell("时长").split("；")[0].replace("s", ""),
                List.of((long) FlowerBoarGuEffect.DURATION_TICKS / Ticks.SECOND,
                        (long) BruteForceLonghornBeetleGuEffect.DURATION_TICKS / Ticks.SECOND));
    }

    private static Map<String, WikiTable.Row> mechanisms(Path wiki) throws IOException {
        Map<String, WikiTable.Row> rows = new java.util.LinkedHashMap<>();
        try (var files = Files.list(wiki.resolve(MECHANISM_DIR))) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(MECHANISM_SUFFIX)).sorted().toList()) {
                for (WikiTable.Row row : WikiTable.parse(file.toString(), Files.readString(file)).guRows()) {
                    String key = documentedKey(row.cell("蛊虫"), row.cell("转"));
                    assertTrue(rows.put(key, row) == null, row.location() + " duplicate mechanism row " + key);
                }
            }
        }
        return rows;
    }

    private static WikiTable page(Path wiki, String relative) throws IOException {
        Path path = wiki.resolve(relative);
        assertTrue(Files.isRegularFile(path), () -> "Missing mapped wiki page: " + path);
        return WikiTable.parse(path.toString(), Files.readString(path));
    }

    private static String cooldownCell(MortalGuItem gu) {
        int effect = gu.guProperties.getEffectCooldownTicks();
        int item = gu.guProperties.getItemCooldownTicks();
        if (effect == 0 && item == 0) return "N/A";
        return seconds(Math.max(effect, item)) + " 秒";
    }

    /** 整秒写整数，否则保留小数（如月刃类蛊的 2 tick 冷却）。 */
    private static String seconds(int ticks) {
        if (ticks % Ticks.SECOND == 0) return String.valueOf(ticks / Ticks.SECOND);
        return new java.math.BigDecimal(ticks).divide(new java.math.BigDecimal(Ticks.SECOND),
                java.math.MathContext.DECIMAL64).stripTrailingZeros().toPlainString();
    }

    private static String typeCell(List<MortalGuItem> items) {
        List<String> names = items.stream().map(g -> g.guProperties.getType())
                .map(type -> type == null ? "无" : zhName(type.getTranslationKey())).toList();
        return String.join(" / ", names.stream().distinct().count() == 1 ? names.subList(0, 1) : names);
    }

    private static String pathCell(List<MortalGuItem> items) {
        List<String> names = items.stream().map(g -> zhName(g.guProperties.getPath().getTranslationKey()))
                .map(name -> name.endsWith("道") ? name.substring(0, name.length() - 1) : name).toList();
        return String.join(" / ", names.stream().distinct().count() == 1 ? names.subList(0, 1) : names);
    }

    private static String zhName(String key) {
        return assertNotNull(ZH_CN.get(key), () -> "zh_cn lacks " + key);
    }

    private static Map<String, String> loadZhCn() {
        try (InputStream stream = WikiItemsGameTests.class.getResourceAsStream("/assets/guzhenren/lang/zh_cn.json")) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(
                    Objects.requireNonNull(stream, "zh_cn.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> names = new HashMap<>();
            json.entrySet().forEach(entry -> names.put(entry.getKey(), entry.getValue().getAsString()));
            return names;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<MortalGuItem> guItems() {
        return ModItems.ITEMS.getEntries().stream().map(DeferredHolder::get).filter(MortalGuItem.class::isInstance)
                .map(MortalGuItem.class::cast).toList();
    }

    private static MortalGuItem gu(String id) {
        return (MortalGuItem) ModItems.ITEMS.getEntries().stream().filter(h -> h.getId().getPath().equals(id))
                .findFirst().orElseThrow().get();
    }

    private static List<Long> values(List<MortalGuItem> items, ToLongFunction<MortalGuItem> getter) {
        return collapse(items.stream().mapToLong(getter).boxed().toList());
    }

    // A multi-rank cell writes one value when every rank shares it, else one line per rank, like typeCell.
    private static <T> List<T> collapse(List<T> values) {
        return values.stream().distinct().count() == 1 ? List.of(values.getFirst()) : List.copyOf(values);
    }

    private static long count(Table table) {
        return GROUPS.stream().filter(g -> g.table() == table).mapToLong(g -> g.ids().size()).sum();
    }

    private static String ladder(String prefix, int first, int last) {
        return IntStream.rangeClosed(first, last).mapToObj(i -> prefix + i).collect(Collectors.joining(" "));
    }

    private static Group group(Table table, String label, String ids) {
        return new Group(table, label, Arrays.asList(ids.split(" ")));
    }

    private static List<Group> groups() {
        return List.of(
                group(Table.ONE_SHOT, "希望蛊", "hope_gu"),
                group(Table.ONE_SHOT, "生机叶蛊", "vitality_leaf_gu_1"),
                group(Table.ONE_SHOT, "寿蛊<br>十年寿蛊<br>百年寿蛊<br>千年寿蛊",
                        "lifespan_gu tens_lifespan_gu hundreds_lifespan_gu thousands_lifespan_gu"),
                group(Table.ONE_SHOT, "青铜舍利蛊<br>赤铁舍利蛊<br>白银舍利蛊<br>黄金舍利蛊<br>紫晶舍利蛊",
                        "copper_relics_gu steel_relics_gu silver_relics_gu gold_relics_gu crystal_relics_gu"),
                group(Table.ONE_SHOT, "胆识蛊", "guts_gu_1"),
                group(Table.ONE_SHOT, "第二空窍蛊", ladder("second_aperture_gu_", 1, 5)),
                group(Table.CHANNEL, "白豕蛊", "white_boar_gu_1"),
                group(Table.CHANNEL, "黑豕蛊", "black_boar_gu_1"),
                group(Table.CHANNEL, "熊力蛊", "bear_strength_gu_1"),
                group(Table.CHANNEL, "虎力蛊", "tiger_strength_gu_2"),
                group(Table.CHANNEL, "石龟负力蛊", "stone_turtle_strength_gu_3"),
                group(Table.CHANNEL, "驰马骏力蛊", "galloping_horse_strength_gu_3"),
                group(Table.CHANNEL, "青牛劳力蛊", "green_bull_labor_gu_3"),
                group(Table.CHANNEL, "棕熊本力蛊", "brown_bear_innate_strength_gu_3"),
                group(Table.CHANNEL, "斤力蛊", "jin_strength_gu_1"),
                group(Table.CHANNEL, "十斤力蛊", "tens_jin_strength_gu_2"),
                group(Table.CHANNEL, "钧力蛊", "jun_strength_gu_3"),
                group(Table.CHANNEL, "十钧力蛊", "tens_jun_strength_gu_4"),
                group(Table.CHANNEL, "百钧之力蛊", "hundreds_jun_strength_gu_5"),
                group(Table.CHANNEL, "铜皮蛊", ladder("bronze_skin_gu_", 1, 4)),
                group(Table.CHANNEL, "铁皮蛊", ladder("iron_skin_gu_", 1, 4)),
                group(Table.CHANNEL, "钢筋蛊", ladder("steel_tendon_gu_", 3, 4)),
                group(Table.CHANNEL, "铁骨蛊", ladder("iron_bone_gu_", 3, 4)),
                group(Table.HIGH, "花豕", "flower_boar_gu_1"),
                group(Table.HIGH, "蛮力天牛", "brute_force_longhorn_beetle_gu_1"),
                group(Table.HIGH, "横冲<br>直撞", "horizontal_crash_gu_3 vertical_crash_gu_3"),
                group(Table.HIGH, "横冲直撞", ladder("charging_crash_gu_", 4, 5)),
                group(Table.HIGH, "自力更生", ladder("self_reliance_gu_", 2, 4)),
                group(Table.HIGH, "苦力蛊", "hardship_strength_gu_4"),
                group(Table.HIGH, "全力以赴", ladder("all_out_effort_gu_", 3, 5)),
                group(Table.HIGH, "蓄力蛊", "store_strength_gu_3"),
                group(Table.HIGH, "惯力蛊", "habitual_strength_gu_4"),
                group(Table.HIGH, "大力蛊", "big_strength_gu_2"),
                group(Table.HIGH, "天蓬蛊", "heavenly_mugwort_gu_3"),
                group(Table.HIGH, "力气蛊", "strength_qi_gu_3"),
                group(Table.HIGH, "玉皮蛊", "jade_skin_gu_1"),
                group(Table.HIGH, "白玉蛊", "white_jade_gu_2"),
                group(Table.HIGH, "功倍蛊", ladder("amplify_effect_gu_", 2, 5)),
                group(Table.HIGH, "月光蛊<br>月芒蛊", "moonlight_gu_1 moonglow_gu_2"),
                group(Table.HIGH, "血月蛊", "blood_moon_gu_3"),
                group(Table.HIGH, "小光蛊", "little_light_gu_1"),
                group(Table.LOW, "酒虫<br>四味酒虫<br>七香酒虫<br>九眼酒虫",
                        "liquor_worm_1 four_flavors_liquor_worm_2 seven_fragrances_liquor_worm_3 nine_eyes_liquor_worm_4"),
                group(Table.LOW, "游僵蛊<br>毛僵蛊<br>跳僵蛊", "roaming_zombie_gu_2 hairy_zombie_gu_3 hopping_zombie_gu_4"),
                group(Table.LOW, "天魔尸蛊<br>梦魇尸蛊<br>修罗尸蛊<br>地魁尸蛊<br>病瘟尸蛊<br>血鬼尸蛊", "heavenly_demon_zombie_gu_5 "
                        + "nightmare_zombie_gu_5 asura_zombie_gu_5 earth_chief_zombie_gu_5 plague_zombie_gu_5 blood_wight_gu_5"),
                group(Table.SHELF, "恶念蛊", ladder("malicious_thought_gu_", 2, 5)),
                group(Table.SHELF, "随意蛊", ladder("casual_gu_", 1, 2)),
                group(Table.RECOVER, "人如故", "man_as_before_gu_5"),
                group(Table.RECOVER, "山如故", "mountain_as_before_gu_5"),
                group(Table.RECOVER, "江如故", "river_as_before_gu_5"),
                group(Table.NO_CLOCK, "元老蛊", ladder("primeval_elder_gu_", 1, 5)),
                group(Table.NO_CLOCK, "天元宝莲<br>天元宝君莲<br>天元宝王莲", "heavenly_essence_treasure_lotus_gu_3 "
                        + "heavenly_essence_treasure_monarch_lotus_gu_4 heavenly_essence_treasure_king_lotus_gu_5"),
                group(Table.NO_CLOCK, "九叶生机草", "nine_leaf_vitality_grass_3"),
                group(Table.NO_CLOCK, "两更蛊<br>三更蛊", "second_watch_gu_4 third_watch_gu_5"),
                group(Table.NO_CLOCK, "石窍蛊", ladder("stone_aperture_gu_", 3, 5)),
                group(Table.MATERIAL, "元石", "primeval_stone"),
                group(Table.MATERIAL, "酒", "liquor sour_liquor sweet_liquor bitter_liquor spicy_liquor"),
                group(Table.MATERIAL, "人窍", ladder("human_aperture_", 1, 5)),
                group(Table.MATERIAL, "草木蛊材", "intimate_grass_bundle moon_orchid_petals"),
                group(Table.MATERIAL, "蛊植种子", "intimate_grass_seeds"),
                group(Table.MATERIAL, "土道蛊材", "rotten_frozen_mud jade rough_jade jadeite rough_jadeite"),
                group(Table.MATERIAL, "血道蛊材", "blood_bottle"),
                group(Table.MATERIAL, "金属", "steel_ingot steel_block"),
                group(Table.MATERIAL, "耕地", "black_soil_farmland"),
                group(Table.MATERIAL, "元泉", "spirit_spring"),
                group(Table.MATERIAL, "纯水", "pure_water_bucket"),
                group(Table.MATERIAL, "仙元", "green_grape_immortal_essence red_date_immortal_essence"
                        + " white_lychee_immortal_essence yellow_apricot_immortal_essence"),
                group(Table.MATERIAL, "杀招", "killing_move_mortal_little_light_amplify"),
                group(Table.MATERIAL, "书册", "gu_atlas_hope " + ladder("human_ancestor_legend_", 1, 38)),
                group(Table.MATERIAL, "蛊方", "gu_recipe"),
                group(Table.MATERIAL, "进度图标", "advancement_icon"),
                group(Table.MATERIAL, "服饰",
                        "school_uniform_chestplate school_uniform_leggings school_uniform_boots"));
    }

    /** 一张 wiki 表：即时型下的标题词（没有则为 {@code null}）及其确切列名。 */
    private enum Table {
        ONE_SHOT(null, "蛊", "转", "道", "类型", "效果"),
        CHANNEL(null, "蛊", "转", "道", "炼化真元", "使用真元", "每次扣", "每天扣", "饱食上限", "食物",
                "一轮结果"),
        HIGH("高频档", "蛊虫", "转", "道", "类型", "炼化真元", "使用真元", "每次扣", "每天扣", "饱食上限", "食物",
                "冷却", "冷却中再催", "效果"),
        LOW("低频档", "蛊虫", "转", "道", "类型", "炼化真元", "使用真元", "每次扣", "每天扣", "饱食上限", "食物",
                "冷却", "冷却中再催", "效果"),
        SHELF("保质期型", "蛊虫", "转", "道", "类型", "炼化真元", "使用真元", "饱食上限", "每天扣", "食物",
                "冷却", "用后", "效果"),
        RECOVER("回复条", "蛊虫", "转", "道", "类型", "炼化真元", "使用真元", "每次扣", "饱食上限",
                "冷却", "效果"),
        NO_CLOCK("无饱食条", "蛊虫", "转", "道", "类型", "炼化真元", "使用真元", "冷却", "用后", "效果"),
        MATERIAL(null, "分组", "数量", "内容");

        private final String title;
        private final List<String> headers;

        Table(String title, String... headers) {
            this.title = title;
            this.headers = List.of(headers);
        }

        // The tiers as ModItems builds them: high is createRankedProperties, low a bag of two uses plus two days.
        boolean fits(TendedGuItem gu, GuHunger hunger) {
            return switch (this) {
                case CHANNEL -> hunger instanceof GuHunger.HungerBar;
                case HIGH -> hunger instanceof GuHunger.HungerBar b && b.perUse() == 3 && b.perDay() == 1
                        && b.max() == 3 * (2 << gu.getRank().ordinal()) + 2 * b.perDay();
                case LOW -> hunger instanceof GuHunger.HungerBar b && b.perUse() > 0 && b.perUse() == 3 * b.perDay()
                        && b.max() == 8 * b.perDay();
                case SHELF -> hunger instanceof GuHunger.HungerBar b && b.perUse() == 0 && gu instanceof ConsumedGuItem;
                case RECOVER -> hunger instanceof GuHunger.RecoverBar;
                case NO_CLOCK -> hunger instanceof GuHunger.NoClock;
                case ONE_SHOT, MATERIAL -> false;
            };
        }
    }

    private record Group(Table table, String label, List<String> ids) {}

    @FunctionalInterface
    private interface WikiCheck {

        void run(Path wiki) throws IOException;
    }
}
