package com.aionemu.commons.scripting.classlistener;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * 聚合类监听器，用于管理和协调多个ClassListener实例
 * Aggregated class listener for managing and coordinating multiple ClassListener instances
 *
 * 该类实现了ClassListener接口，通过维护一个监听器列表来实现以下功能：
 * This class implements the ClassListener interface and maintains a list of listeners to:
 * - 统一管理多个监听器 (Manage multiple listeners uniformly)
 * - 按序执行监听器方法 (Execute listener methods in sequence)
 * - 支持动态添加监听器 (Support dynamic listener addition)
 */
public class AggregatedClassListener implements ClassListener {
    /**
     * 监听器列表，存储所有注册的ClassListener实例
     * List of listeners storing all registered ClassListener instances
     */
    private final List<ClassListener> classListeners;

    /**
     * 默认构造函数，初始化一个空的监听器列表
     * Default constructor, initializes an empty listener list
     */
    public AggregatedClassListener() {
        this.classListeners = Lists.newArrayList();
    }

    /**
     * 使用预设的监听器列表构造实例
     * Construct instance with a predefined list of listeners
     *
     * @param classListeners 初始监听器列表 / Initial list of listeners
     */
    public AggregatedClassListener(List<ClassListener> classListeners) {
        this.classListeners = classListeners;
    }

    /**
     * 获取当前的监听器列表
     * Get the current list of listeners
     *
     * @return 监听器列表 / List of listeners
     */
    public List<ClassListener> getClassListeners() {
        return this.classListeners;
    }

    /**
     * 添加新的监听器到列表中
     * Add a new listener to the list
     *
     * @param cl 要添加的监听器 / Listener to add
     */
    public void addClassListener(ClassListener cl) {
        this.getClassListeners().add(cl);
    }

    /**
     * 按照注册顺序执行所有监听器的postLoad方法
     * Execute postLoad method of all listeners in registration order
     *
     * @param classes 要处理的类数组 / Array of classes to process
     */
    @Override
    public void postLoad(Class<?>[] classes) {
        for (ClassListener cl : this.getClassListeners()) {
            cl.postLoad(classes);
        }
    }

    /**
     * 按照注册顺序的反序执行所有监听器的preUnload方法
     * Execute preUnload method of all listeners in reverse registration order
     *
     * @param classes 要处理的类数组 / Array of classes to process
     */
    @Override
    public void preUnload(Class<?>[] classes) {
        for (ClassListener cl : Lists.reverse(this.getClassListeners())) {
            cl.preUnload(classes);
        }
    }
}
