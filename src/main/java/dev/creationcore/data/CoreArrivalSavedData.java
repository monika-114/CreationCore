package dev.creationcore.data;

import dev.creationcore.entity.CreativeCoreEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.ArrayList;
import java.util.List;

/** Persistent, independent arrivals: unloading the End does not cancel the ritual. */
public final class CoreArrivalSavedData extends SavedData {
    private record Arrival(long due, int x, int z) {}
    private final List<Arrival> arrivals = new ArrayList<>();

    public static CoreArrivalSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                CoreArrivalSavedData::new, CoreArrivalSavedData::load,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE), "creationcore_core_arrivals");
    }

    private static CoreArrivalSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CoreArrivalSavedData data = new CoreArrivalSavedData();
        ListTag list = tag.getList("arrivals", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.arrivals.add(new Arrival(entry.getLong("due"), entry.getInt("x"), entry.getInt("z")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Arrival arrival : arrivals) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("due", arrival.due());
            entry.putInt("x", arrival.x());
            entry.putInt("z", arrival.z());
            list.add(entry);
        }
        tag.put("arrivals", list);
        return tag;
    }

    public void schedule(ServerLevel overworld) {
        BlockPos spawn = overworld.getSharedSpawnPos();
        int dx, dz;
        do {
            dx = overworld.random.nextInt(51) - 25;
            dz = overworld.random.nextInt(51) - 25;
        } while (dx == 0 && dz == 0);
        arrivals.add(new Arrival(overworld.getGameTime() + 100, spawn.getX() + dx, spawn.getZ() + dz));
        setDirty();
    }

    public void tick(ServerLevel level) {
        var iterator = arrivals.iterator();
        while (iterator.hasNext()) {
            Arrival arrival = iterator.next();
            if (level.getGameTime() < arrival.due()) continue;
            level.getChunk(arrival.x() >> 4, arrival.z() >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, arrival.x(), arrival.z());
            CreativeCoreEntity core = new CreativeCoreEntity(level, arrival.x() + 0.5, y + 2.5, arrival.z() + 0.5);
            if (level.addFreshEntity(core)) {
                iterator.remove();
                setDirty();
            }
        }
    }
}
