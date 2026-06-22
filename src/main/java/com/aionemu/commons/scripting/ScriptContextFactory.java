package com.aionemu.commons.scripting;

import com.aionemu.commons.scripting.impl.ScriptContextImpl;
import java.io.File;

/**
 * 脚本上下文工厂类，用于创建和管理脚本上下文实例
 * Script Context Factory class for creating and managing script context instances
 *
 * 该类实现了工厂模式，负责创建ScriptContext实例，支持父子上下文的层级关系
 * This class implements the Factory pattern, responsible for creating ScriptContext instances,
 * supporting parent-child context hierarchy relationships
 */
public final class ScriptContextFactory {

    /**
     * 创建脚本上下文实例
     * Create a script context instance
     *
     * @param root   脚本根目录 / Script root directory
     * @param parent 父级上下文(可选) / Parent context (optional)
     * @return 创建的脚本上下文实例 / Created script context instance
     * @throws InstantiationException 如果创建上下文失败 / If context creation fails
     */
    public static ScriptContext getScriptContext(File root, ScriptContext parent) throws InstantiationException {
        ScriptContextImpl ctx;
        if (parent == null) {
            ctx = new ScriptContextImpl(root);
        } else {
            ctx = new ScriptContextImpl(root, parent);
            parent.addChildScriptContext(ctx);
        }

        return ctx;
    }
}
