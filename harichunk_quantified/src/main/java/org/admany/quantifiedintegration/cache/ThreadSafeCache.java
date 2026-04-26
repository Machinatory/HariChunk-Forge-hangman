package org.admany.quantifiedintegration.cache;

import java.util.function.Function;

public interface ThreadSafeCache<K, V> {

    V get(K key, Function<K, V> loader);

    void invalidateAll();
}
