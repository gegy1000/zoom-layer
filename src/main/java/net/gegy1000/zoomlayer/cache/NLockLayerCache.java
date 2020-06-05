package net.gegy1000.zoomlayer.cache;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.layer.util.LayerOperator;

import java.util.Arrays;

public final class NLockLayerCache implements BiomeLayerCache {
    private final long[] keys;
    private final int[] values;

    private final int capacity;
    private final int mask;

    private final Object[] locks;

    public NLockLayerCache(int capacity) {
        this.capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
        this.mask = this.capacity - 1;

        this.keys = new long[this.capacity];
        Arrays.fill(this.keys, Long.MIN_VALUE);
        this.values = new int[this.capacity];

        this.locks = new Object[this.capacity];
        for (int i = 0; i < this.capacity; i++) {
            this.locks[i] = new Object();
        }
    }

    @Override
    public int get(int x, int z, LayerOperator operator) {
        long key = key(x, z);
        int idx = hash(key) & this.mask;

        Object lock = this.locks[idx];

        // if the entry here has a key that matches ours, we have a cache hit
        synchronized (lock) {
            if (this.keys[idx] == key) {
                return this.values[idx];
            }
        }

        // cache miss: sample the operator and put the result into our cache entry
        int sampled = operator.apply(x, z);
        synchronized (lock) {
            this.keys[idx] = key;
            this.values[idx] = sampled;
        }

        return sampled;
    }

    private static int hash(long key) {
        return (int) HashCommon.mix(key);
    }

    private static long key(int x, int z) {
        return ChunkPos.toLong(x, z);
    }
}
