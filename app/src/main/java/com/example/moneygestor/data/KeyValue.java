package com.example.moneygestor.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KeyValue<K,V> {
    K key;
    V value;

    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public String toDebugString() {
        return String.format("{%s = %s}", key.toString(), value.toString());
    }

    public static <K,V> List<KeyValue<K,V>> toList(Map<K, V> map) {
        List<KeyValue<K,V>> list = new LinkedList<>();

        if(map == null)
            return list;

        for(Map.Entry<K,V> value : map.entrySet()) {
            list.add(new KeyValue<K,V>(value.getKey(), value.getValue()));
        }

        return list;
    }
}
