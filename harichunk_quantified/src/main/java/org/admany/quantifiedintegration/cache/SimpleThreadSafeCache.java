package org.admany.quantifiedintegration.cache;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SimpleThreadSafeCache<K, V> implements ThreadSafeCache<K, V> {

    private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final long maxSize;
    private final long ttlNanos;

    private SimpleThreadSafeCache(long maxSize, Duration ttl) {
        this.maxSize = Math.max(1L, maxSize);
        this.ttlNanos = ttl == null ? 0L : Math.max(0L, ttl.toNanos());
    }

    public static <K, V> SimpleThreadSafeCache<K, V> create(long maxSize, Duration ttl) {
        return new SimpleThreadSafeCache<>(maxSize, ttl);
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        long now = System.nanoTime();
        Entry<V> cached = entries.get(key);
        if (cached != null && !cached.isExpired(now, ttlNanos)) {
            return cached.value;
        }

        V loaded = loader.apply(key);
        entries.put(key, new Entry<>(loaded, now));
        trimIfNeeded();
        return loaded;
    }

    @Override
    public void invalidateAll() {
        entries.clear();
    }

    private void trimIfNeeded() {
        int overshoot = entries.size() - (int) Math.min(Integer.MAX_VALUE, maxSize);
        if (overshoot <= 0) {
            return;
        }

        Iterator<Map.Entry<K, Entry<V>>> iterator = entries.entrySet().iterator();
        while (overshoot-- > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record Entry<V>(V value, long writeNanos) {
        boolean isExpired(long now, long ttlNanos) {
            return ttlNanos > 0L && now - writeNanos > ttlNanos;
        }
    }
}
