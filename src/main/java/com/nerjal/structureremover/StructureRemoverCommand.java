package com.nerjal.structureremover;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.gen.structure.Structure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.command.argument.RegistryPredicateArgumentType.registryPredicate;
import static net.minecraft.command.argument.RegistryPredicateArgumentType.getPredicate;

public class StructureRemoverCommand {
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION =
            new DynamicCommandExceptionType(id -> Text.translatable("commands.locate.structure.invalid", id));

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> r = LiteralArgumentBuilder.literal("struct-rem");
        dispatcher.register(r
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("structure", registryPredicate(Registry.STRUCTURE_KEY))
                        .executes(StructureRemoverCommand::remove)
                        .then(argument("location", Vec3ArgumentType.vec3())
                                .executes(StructureRemoverCommand::remove)
                        )
                )
        );
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> structureRegistry) {
        return predicate.getKey()
                .map(key -> structureRegistry.getEntry(key)
                        .map(RegistryEntryList::of), structureRegistry::getEntryList);
    }

    static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            Registry<Structure> registry = context.getSource().getWorld().getRegistryManager().get(Registry.STRUCTURE_KEY);
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate =
                    getPredicate(context, "structure", Registry.STRUCTURE_KEY, STRUCTURE_INVALID_EXCEPTION);
            RegistryEntryList<Structure> list = getStructureListForPredicate(predicate, registry)
                    .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
            Vec3d vec;
            try {
                vec = Vec3ArgumentType.getVec3(context, "location");
            } catch (IllegalArgumentException e) {
                vec = context.getSource().getPosition();
            }
            BlockPos pos = new BlockPos(vec);
            list.forEach(struct ->
                    StructureRemover.removeStructure(context.getSource().getWorld(), pos, struct.value()));
        } catch (Exception e) {
            StructureRemover.LOGGER.error(e);
            throw e;
        }
        return 0;
    }
}
