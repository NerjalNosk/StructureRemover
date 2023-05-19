package com.nerjal.structureremover;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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
                .then(argument("structure", registryPredicate(RegistryKeys.STRUCTURE))
                        .executes(StructureRemoverCommand::remove)
                        .then(argument("location", Vec3ArgumentType.vec3())
                                .executes(StructureRemoverCommand::remove)
                        )
                )
        );
    }

    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureListForPredicate(
            RegistryPredicateArgumentType.@NotNull RegistryPredicate<Structure> predicate,
            Registry<Structure> structureRegistry) {
        Either<RegistryKey<Structure>, TagKey<Structure>> either = predicate.getKey();
        Function<RegistryKey<Structure>, Optional<RegistryEntryList.Direct<?>>> var10001 = key ->
                structureRegistry.getEntry(key).map(entry ->
                        RegistryEntryList.of(new RegistryEntry[]{entry})
            );
        Objects.requireNonNull(structureRegistry);
        return (Optional<RegistryEntryList.ListBacked<Structure>>)either.map(var10001, structureRegistry::getEntryList);
    }

    static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            Registry<Structure> registry =
                    context.getSource().getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate =
                    getPredicate(context, "structure", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION);
            RegistryEntryList<Structure> list = getStructureListForPredicate(predicate, registry)
                    .orElseThrow(() -> STRUCTURE_INVALID_EXCEPTION.create(predicate.asString()));
            Vec3d vec;
            try {
                vec = Vec3ArgumentType.getVec3(context, "location");
            } catch (IllegalArgumentException e) {
                vec = context.getSource().getPosition();
            }
            BlockPos pos = new BlockPos(new Vec3i((int) vec.x, (int) vec.y, (int) vec.z));
            list.forEach(struct ->
                    StructureRemover.removeStructure(context.getSource().getWorld(), pos, struct.value()));
        } catch (Exception e) {
            StructureRemover.LOGGER.error(e);
            throw e;
        }
        return 0;
    }
}
