package net.alex.guzhenren.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Method;
import java.util.List;
import net.alex.guzhenren.command.root.ModCommand;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModCommandTest {

    @Test
    void soulWritesHaveTheirOwnRootDomain() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        var root = dispatcher.getRoot().getChild("guzhenren");
        var soul = root.getChild("soul");
        assertNotNull(soul);
        assertNotNull(soul.getChild("max"));
        assertNotNull(soul.getChild("current"));
        assertNotNull(soul.getChild("refill"));
        assertNull(root.getChild("body").getChild("soul"));
    }

    @Test
    void pathWritesHaveTheirOwnRootDomain() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        var root = dispatcher.getRoot().getChild("guzhenren");
        var path = root.getChild("path");
        assertNotNull(path);
        assertNotNull(path.getChild("marks"));
        assertNotNull(path.getChild("attainment"));
        assertNotNull(path.getChild("effect"));
        assertNotNull(path.getChild("strength"));
        assertNull(root.getChild("body").getChild("path"));
        assertNull(root.getChild("info"));
    }

    @Test
    void marksWriteWholeMarksThenPointsOnTheBody() {
        var path = PathCommand.buildNode().build().getChild("marks").getChild("path");
        assertEquals(List.of("set", "add", "sub", "points"), getChildNames(path));
        var marks = path.getChild("set").getChild("value");
        assertNotNull(marks.getCommand());
        assertEquals(List.of("targets", "points"), getChildNames(marks));
        var both = marks.getChild("points").getChild("add").getChild("points");
        assertNotNull(both.getCommand());
        assertNotNull(both.getChild("targets"));
        var pointsOnly = path.getChild("points").getChild("sub").getChild("points");
        assertNotNull(pointsOnly.getCommand());
        assertNotNull(pointsOnly.getChild("targets"));
    }

    @Test
    void effectsCanOnlyBeTakenOff() {
        var effect = PathCommand.buildNode().build().getChild("effect");
        assertEquals(List.of("remove", "clear"), getChildNames(effect));
        assertNotNull(effect.getChild("remove").getChild("type").getChild("targets"));
        assertNotNull(effect.getChild("clear").getCommand());
        assertNotNull(effect.getChild("clear").getChild("targets"));
    }

    @Test
    void bodyPhysiqueCommandsReplaceTheOldLifeFormCommands() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        var body = dispatcher.getRoot().getChild("guzhenren").getChild("body");
        var physique = body.getChild("physique");
        assertNotNull(physique);
        assertNotNull(physique.getChild("add"));
        assertNotNull(physique.getChild("remove"));
        assertNull(physique.getChild("extreme"));
        assertNull(body.getChild("lifeform"));
        assertNull(dispatcher.getRoot().getChild("guzhenren").getChild("aperture").getChild("physique"));
    }

    @Test
    void bodyPartCommandsNameAGradeByFamilyAndRank() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        var body = dispatcher.getRoot().getChild("guzhenren").getChild("body");
        var skin = body.getChild("skin").getChild("set");
        assertEquals(List.of("native", "jade_skin", "white_jade_skin", "bronze_skin", "iron_skin"),
                getChildNames(skin));
        assertNotNull(skin.getChild("native").getChild("targets"));
        assertEquals(List.of("seconds", "targets"), getChildNames(skin.getChild("jade_skin")));
        assertNotNull(skin.getChild("jade_skin").getChild("seconds").getChild("targets"));
        assertEquals(List.of("seconds", "targets"), getChildNames(skin.getChild("white_jade_skin")));
        assertEquals(List.of("rank", "targets"), getChildNames(skin.getChild("bronze_skin")));
        assertEquals(List.of("rank", "targets"), getChildNames(skin.getChild("iron_skin")));
        assertNull(skin.getChild("bronze_skin").getChild("seconds"));
        assertEquals(List.of("native", "steel_tendon"), getChildNames(body.getChild("tendon").getChild("set")));
        assertEquals(List.of("rank", "targets"),
                getChildNames(body.getChild("tendon").getChild("set").getChild("steel_tendon")));
        assertEquals(List.of("native", "iron_bone"), getChildNames(body.getChild("bone").getChild("set")));
        assertEquals(List.of("rank", "targets"),
                getChildNames(body.getChild("bone").getChild("set").getChild("iron_bone")));
        assertNull(body.getChild("flesh"));
    }

    private static List<String> getChildNames(CommandNode<CommandSourceStack> node) {
        return node.getChildren().stream().map(CommandNode::getName).toList();
    }

    @Test
    void dimensionTravelCommandsLiveOnTheGuworldRoot() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher();

        var guzhenren = dispatcher.getRoot().getChild("guzhenren");
        assertNull(guzhenren.getChild("enter"));
        assertNull(guzhenren.getChild("exit"));

        var guworld = dispatcher.getRoot().getChild("guworld");
        assertNotNull(guworld);
        var enter = guworld.getChild("enter");
        assertNotNull(enter);
        assertNotNull(enter.getChild("dimension"));
        assertNotNull(enter.getChild("dimension").getChild("targets"));
        var exit = guworld.getChild("exit");
        assertNotNull(exit);
        assertNotNull(exit.getChild("targets"));
    }

    @Test
    void rankAndStageShareOneCommandWithAnOptionalSelector() {
        CommandNode<CommandSourceStack> aperture = ApertureCommand.buildNode().build();
        var rank = aperture.getChild("rank");
        assertEquals(List.of("set", "up", "down"), getChildNames(rank));
        assertEquals(List.of("rank:one", "rank:two", "rank:three", "rank:four", "rank:five"),
                getChildNames(rank.getChild("set")));
        for (var rankWord : List.of(rank.getChild("set").getChild("rank:one"),
                rank.getChild("set").getChild("rank:five"), rank.getChild("up"), rank.getChild("down"))) {
            var stage = rankWord.getChild("stage");
            assertEquals(List.of("set", "up", "down"), getChildNames(stage));
            assertEquals(List.of("stage:init", "stage:middle", "stage:upper", "stage:peak"),
                    getChildNames(stage.getChild("set")));
            for (var leaf : List.of(stage.getChild("set").getChild("stage:middle"), stage.getChild("up"),
                    stage.getChild("down"))) {
                assertNotNull(leaf.getCommand(), rankWord.getName() + " stage " + leaf.getName());
                assertNotNull(leaf.getChild("first").getChild("targets"));
                assertNotNull(leaf.getChild("second").getChild("targets"));
                assertNotNull(leaf.getChild("targets"));
            }
            assertNotNull(rankWord.getCommand());
            assertNotNull(rankWord.getChild("first"));
        }
        assertNull(rank.getChild("set").getCommand());
        assertNotNull(aperture.getChild("stage").getChild("set").getChild("stage:peak").getChild("second"));
        assertNotNull(aperture.getChild("stage").getChild("down").getChild("targets"));
    }

    @Test
    void talentSetsGradesAndConcreteTenExtremes() {
        var set = ApertureCommand.buildNode().build().getChild("talent").getChild("set");
        for (String grade : new String[]{"first", "second", "third", "fourth"}) {
            assertNotNull(set.getChild(grade).getChild("targets"));
        }
        for (ExtremePhysique physique : ExtremePhysique.getSettable()) {
            assertNotNull(set.getChild(ApertureCommand.TEN_EXTREME + physique.getSerializedName()).getChild("targets"));
        }
        assertNull(set.getChild("ten_extremes"));
        assertNull(set.getChild(ApertureCommand.TEN_EXTREME + "none"));
        assertEquals(4 + ExtremePhysique.getSettable().length, set.getChildren().size());
    }

    @Test
    void essenceKeepsCurrentBaseAndRefill() {
        var essence = ApertureCommand.buildNode().build().getChild("essence");
        assertEquals(3, essence.getChildren().size());
        assertNull(essence.getChild("distilled"));
        assertNotNull(essence.getChild("current").getChild("sub").getChild("value").getChild("second"));
        assertNotNull(essence.getChild("base").getChild("set").getChild("value").getChild("first"));
        assertNotNull(essence.getChild("refill").getChild("targets"));
        assertNotNull(essence.getChild("refill").getChild("second").getChild("targets"));
        assertEquals(Aperture.MAX_BASE - 1, ApertureCommand.MAX_COMMAND_BASE);
    }

    @Test
    void selectorIsReadFromTheParsedPath() {
        assertEquals("second", ApertureCommand.getSelector(
                parsedContext("rank", "up", "stage", "set", "stage:middle", "second")));
        assertEquals("first", ApertureCommand.getSelector(parsedContext("rank", "set", "rank:two", "first")));
        assertNull(ApertureCommand.getSelector(parsedContext("stage", "set", "stage:peak")));
    }

    private static CommandContext<CommandSourceStack> parsedContext(String... words) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandNode<CommandSourceStack> node = ApertureCommand.buildNode().build();
        dispatcher.getRoot().addChild(node);
        CommandContextBuilder<CommandSourceStack> context = new CommandContextBuilder<>(
                dispatcher, null, dispatcher.getRoot(), 0).withNode(node, StringRange.between(0, 8));
        int cursor = 9;
        for (String word : words) {
            node = node.getChild(word);
            context.withNode(node, StringRange.between(cursor, cursor + word.length()));
            cursor += word.length() + 1;
        }
        return context.build("aperture " + String.join(" ", words));
    }

    /** 注册全部模组命令根的调度器，做法同命令事件。 */
    private static CommandDispatcher<CommandSourceStack> dispatcher() throws ReflectiveOperationException {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommand.class.getDeclaredMethod("register", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        return dispatcher;
    }
}
