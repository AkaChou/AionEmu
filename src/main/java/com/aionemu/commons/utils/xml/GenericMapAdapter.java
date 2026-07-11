package com.aionemu.commons.utils.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * 通用 Map XML 适配器，支持集合与嵌套 Map。
 * Generic Map XML adapter supporting collections and nested maps.
 *
 * @param <K> 键类型 / Map key type
 * @param <V> 值类型（对象 / 集合 / 嵌套 Map） / Value type (object / collection / nested map)
 * @author Oleh_Faizulin
 */
public class GenericMapAdapter<K, V> extends XmlAdapter<GenericMapAdapter.KeyValuePairContainer<K, V>, Map<K, V>> {

    /**
     * 将 Map 序列化为 XML 节点容器。
     * Marshal a Map into an XML key-value container.
     *
     * Source map
     * Key-value container
     *
     * @param v @throws Exception 序列化失败 / On marshal failure
     */
    @Override
    public KeyValuePairContainer<K, V> marshal(Map<K, V> v) throws Exception {
        if (v == null) {
            return null;
        }

        KeyValuePairContainer<K, V> result = new KeyValuePairContainer<K, V>();
        for (Map.Entry<K, V> entry : v.entrySet()) {
            result.addElement(entry);
        }
        return result;
    }

    /**
     * 将 XML 节点容器反序列化为 Map。
     * Unmarshal an XML key-value container into a Map.
     *
     * @param v 键值容器 / Key-value container
     * Reconstructed map
     *
     * @param v @throws Exception 反序列化失败 / On unmarshal failure
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public Map<K, V> unmarshal(KeyValuePairContainer<K, V> v) throws Exception {
        Map<K, V> result = new HashMap<K, V>();
        for (KeyValuePair<K, V> kvp : v.getValues()) {
            if (kvp.getMapValue() != null) {
                result.put(kvp.getKey(), (V) kvp.getMapValue());
            } else if (kvp.getCollectionValue() != null) {
                result.put(kvp.getKey(), (V) kvp.getCollectionValue());
            } else {
                result.put(kvp.getKey(), kvp.getValue());
            }
        }
        return result;
    }

    /**
     * XML 键值对容器。
     * XML key-value pair container.
     *
     * @param <K> 键类型 / Key type
     * @param <V> 值类型 / Value type
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class KeyValuePairContainer<K, V> {

        @XmlElement(name = "mapEntry")
        private List<KeyValuePair<K, V>> values;

        /**
         * 追加一个 Map 条目。
         * Append a map entry.
         *
         * Entry
         */
        public void addElement(Map.Entry<K, V> entry) {
            if (values == null) {
                values = new ArrayList<KeyValuePair<K, V>>();
            }
            values.add(new KeyValuePair<K, V>(entry));
        }

        /**
         * 获取全部键值对。
         * Get all key-value pairs.
         *
         * @return 列表（可能为空） / List (may be empty)
         */
        public List<KeyValuePair<K, V>> getValues() {
            if (values == null) {
                return Collections.emptyList();
            }
            return values;
        }
    }

    /**
     * 复合键值对：普通值 / 集合 / 嵌套 Map。
     * collection / nested map. / collection / nested map.
     *
     * @param <K> 键类型 / Key type
     * @param <V> 值类型 / Value type
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class KeyValuePair<K, V> {

        /**
         * JAXB 无参构造。
         * JAXB no-arg constructor.
         */
        public KeyValuePair() {
        }

        /**
         * 从 Map.Entry 构造。
         * Construct from a Map.Entry.
         *
         * Entry
         */
        public KeyValuePair(Map.Entry<K, V> entry) {
            this(entry.getKey(), entry.getValue());
        }

        /**
         * 根据值类型写入对应字段。
         * Populate the matching field based on value type.
         *
         * Key
         * Value
         */
        @SuppressWarnings("rawtypes")
        public KeyValuePair(K key, V value) {
            this.key = key;

            if (value instanceof Collection) {
                this.collectionValue = (Collection) value;
            } else if (value instanceof Map) {
                this.mapValue = (Map) value;
            } else {
                this.value = value;
            }
        }

        @XmlElement
        private K key;

        @XmlElement
        private V value;

        @XmlElement
        @SuppressWarnings("rawtypes")
        private Collection collectionValue;

        @XmlElement
        @SuppressWarnings("rawtypes")
        @XmlJavaTypeAdapter(value = GenericMapAdapter.class)
        private Map mapValue;

        /**
         * 获取键。
         * Get the key.
         *
         * Key
         */
        public K getKey() {
            return key;
        }

        /**
         * 获取普通值。
         * Get the plain value.
         *
         * Value
         */
        public V getValue() {
            return value;
        }

        /**
         * 获取集合值。
         * Get the collection value.
         *
         * Collection
         */
        @SuppressWarnings("rawtypes")
        public Collection getCollectionValue() {
            return collectionValue;
        }

        /**
         * 获取嵌套 Map 值。
         * Get the nested map value.
         *
         * Nested map
         */
        @SuppressWarnings("rawtypes")
        public Map getMapValue() {
            return mapValue;
        }
    }
}
