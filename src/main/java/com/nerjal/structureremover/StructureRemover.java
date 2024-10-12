package com.nerjal.structureremover;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class StructureRemover {
    static final Logger LOGGER = LogManager.getLogger("StructureRemover");

    public static boolean removeStructure(@NotNull ServerWorld world, BlockPos pos, Structure structure) {
        Map<Structure, LongSet> structures = world.getStructureAccessor().getStructureReferences(pos);
        boolean b = false;
        if (structures.containsKey(structure)) {
            b = true;
            BlockPos start = world.getStructureAccessor().getStructureContaining(pos, structure).getPos().getStartPos();
            Map<Structure, StructureStart> startMap = new HashMap<>(world.getChunk(start).getStructureStarts());
            startMap.remove(structure);
            world.getChunk(start).setStructureStarts(startMap);
        }
        return b;
    }
}
