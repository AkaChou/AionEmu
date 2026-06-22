package com.aionemu.commons.callbacks;

import com.aionemu.commons.callbacks.enhancer.GlobalCallbackEnhancer;
import com.aionemu.commons.callbacks.enhancer.ObjectCallbackEnhancer;
import java.lang.instrument.Instrumentation;

/**
 * Java代理增强器，负责在JVM启动时注册字节码转换器
 * Java agent enhancer responsible for registering bytecode transformers during JVM startup
 */
public class JavaAgentEnhancer {
    /**
     * JVM启动时的代理入口方法
     * Agent entry point method called during JVM startup
     *
     * @param args JVM参数 / JVM arguments
     * @param instrumentation 字节码增强工具 / Bytecode instrumentation tool
     */
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ObjectCallbackEnhancer(), true);
        instrumentation.addTransformer(new GlobalCallbackEnhancer(), true);
    }
}
