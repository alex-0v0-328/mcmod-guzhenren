package net.alex.guzhenren.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModEnumArgumentTest {

    private enum Kind implements StringRepresentable {

        A, B;

        @Override
        public @NotNull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    @Test
    void getReadsThroughRedirectChildContext() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        var root = dispatcher.register(Commands.literal("root")
                .then(ModEnumArgument.createArgument("kind", Kind.values())));
        dispatcher.register(Commands.literal("alias").redirect(root));

        var context = dispatcher.parse("alias b", null).getContext().build("alias b");
        assertEquals(Kind.B, ModEnumArgument.get(context, "kind", Kind.values()));
        assertThrows(IllegalArgumentException.class, () -> StringArgumentType.getString(context, "kind"));
    }
}
