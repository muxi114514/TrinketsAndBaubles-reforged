package xzeroair.trinkets.util.handlers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

/**
 * 计数器集合，随 capability 一并存取。
 *
 * 线程安全说明：实例随 capability 挂在单个实体上，只在其所属线程（服务端主线程或客户端线程）访问，
 * 故沿用 TreeMap——有序遍历还能让 NBT 写入顺序稳定，换成 ConcurrentHashMap 反而丢失该性质。
 */
public class TickHandler {

    private static final String TAG_COUNTERS = "Counters";

    private final Map<String, Counter> counters;

    public TickHandler() {
        this.counters = new TreeMap<>();
    }

    public Map<String, Counter> getCounters() {
        return this.counters;
    }

    public void addCounter(String key, int length, boolean countdown, boolean shouldTick, boolean saveToNBT) {
        if (!this.counters.containsKey(key)) {
            this.counters.put(key, new Counter(key, length, countdown, shouldTick, saveToNBT));
        }
    }

    public void removeCounter(String key) {
        this.counters.remove(key);
    }

    public void clearCounters() {
        this.counters.clear();
    }

    @Nullable
    public Counter getCounter(String key) {
        return this.counters.get(key);
    }

    @Nullable
    public Counter getCounter(String key, int length, boolean isCountdown, boolean shouldTick, boolean create, boolean saveNBT) {
        return this.getCounter(key, length, isCountdown, shouldTick, true, create, saveNBT);
    }

    @Nullable
    public Counter getCounter(String key, int length, boolean isCountdown, boolean shouldTick, boolean autoReset, boolean create, boolean saveNBT) {
        final Counter existing = this.counters.get(key);
        if (existing != null) {
            return existing;
        }
        if (create) {
            final Counter value = new Counter(key, length, isCountdown, shouldTick, autoReset, saveNBT);
            this.counters.put(key, value);
            return value;
        }
        return null;
    }

    public void saveCountersToNBT(CompoundTag compound) {
        final CompoundTag counterTags = new CompoundTag();
        for (Entry<String, Counter> entry : this.counters.entrySet()) {
            final Counter counter = entry.getValue();
            if (!counter.saveToNBT()) {
                continue;
            }
            final CompoundTag nbt = new CompoundTag();
            nbt.putInt("Tick", counter.getTick());
            nbt.putInt("Length", counter.getLength());
            nbt.putBoolean("Countdown", counter.getCountdown());
            nbt.putBoolean("ShouldTick", counter.shouldTick());
            counterTags.put(counter.getName(), nbt);
        }
        if (!counterTags.isEmpty()) {
            compound.put(TAG_COUNTERS, counterTags);
        }
    }

    public void loadCountersFromNBT(CompoundTag compound) {
        if (!compound.contains(TAG_COUNTERS)) {
            return;
        }
        final CompoundTag counterTags = compound.getCompound(TAG_COUNTERS);
        for (String name : counterTags.getAllKeys()) {
            final CompoundTag counter = counterTags.getCompound(name);
            final Counter loaded = new Counter(
                    name,
                    counter.getInt("Length"),
                    counter.getBoolean("Countdown"),
                    counter.getBoolean("ShouldTick"),
                    true).setTick(counter.getInt("Tick"));
            this.counters.put(name, loaded);
        }
    }
}
