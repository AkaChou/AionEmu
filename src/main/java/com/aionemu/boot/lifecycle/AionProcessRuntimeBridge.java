package com.aionemu.boot.lifecycle;

import com.aionemu.commons.utils.AionProcessExit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 进程退出/中止的运行时桥接，隔离对底层进程工具的直接依赖。
 * Runtime bridge for process exit/halt, isolating direct process-tool dependencies.
 */
@Component
@Lazy
public class AionProcessRuntimeBridge {

    /**
     * 以指定状态码正常退出当前进程。
     * Exit the current process with the given status code.
     *
     * @param status 退出状态码 / exit status code
     */
    public void exit(int status) {
        AionProcessExit.exit(status);
    }

    /**
     * 以指定状态码强制中止当前进程。
     * Halt the current process with the given status code.
     *
     * @param status 中止状态码 / halt status code
     */
    public void halt(int status) {
        AionProcessExit.halt(status);
    }
}
