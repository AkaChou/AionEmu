/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.utils.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * 通用Map XML适配器 (Generic Map XML Adapter)
 * 
 * 该适配器用于将Map对象与XML结构进行双向转换，支持包含集合和嵌套Map的复杂数据结构
 * 泛型参数：
 * @param <K> Map键类型 (Type of Map Key)
 * @param <V> Map值类型，支持普通对象、集合或嵌套Map (Type of Map Value, supports Object/Collection/Nested Map)
 * 
 * @author Oleh_Faizulin
 */
public class GenericMapAdapter<K, V> extends XmlAdapter<GenericMapAdapter.KeyValuePairContainer<K, V>, Map<K, V>> {
    
    /**
     * 将Map序列化为XML节点 (Serialize Map to XML nodes)
     * @param v 需要转换的Map对象 (Map to be marshalled)
     * @return 包含键值对结构的容器对象 (Container with key-value pair structure)
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
     * 将XML节点反序列化为Map (Deserialize XML nodes to Map)
     * @param v 包含键值对结构的容器对象 (Container with key-value pair structure)
     * @return 重建后的Map对象，自动识别集合和嵌套Map (Reconstructed Map with auto-detection for Collection/Nested Map)
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
     * XML键值对容器 (XML Key-Value Pair Container)
     * 使用@XmlElementWrapper替代会产生多余的包装节点，因此直接使用列表存储条目
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class KeyValuePairContainer<K, V> {
        
        @XmlElement(name = "mapEntry")
        private List<KeyValuePair<K, V>> values;
        
        public void addElement(Map.Entry<K, V> entry) {
            if (values == null) {
                values = new ArrayList<KeyValuePair<K, V>>();
            }
            values.add(new KeyValuePair<K, V>(entry));
        }
        
        public List<KeyValuePair<K, V>> getValues() {
            if (values == null) {
                return Collections.emptyList();
            }
            return values;
        }
    }
    
    /**
     * 复合键值对结构 (Composite Key-Value Structure)
     * 包含三种值存储方式：
     * 1. 普通对象值 (value字段)
     * 2. 集合类型值 (collectionValue字段)
     * 3. 嵌套Map值 (mapValue字段，使用适配器递归处理)
     */
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    public static class KeyValuePair<K, V> {
        
        public KeyValuePair() {
            
        }
        
        public KeyValuePair(Map.Entry<K, V> entry) {
            this(entry.getKey(), entry.getValue());
        }
        
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
        
        public K getKey() {
            return key;
        }
        
        public V getValue() {
            return value;
        }
        
        @SuppressWarnings("rawtypes")
        public Collection getCollectionValue() {
            return collectionValue;
        }
        
        @SuppressWarnings("rawtypes")
        public Map getMapValue() {
            return mapValue;
        }
    }
}
