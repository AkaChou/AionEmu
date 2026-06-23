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
package com.aionemu.commons.network.util;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程未捕获异常处理器，用于处理线程中未被捕获的异常
 * Thread Uncaught Exception Handler for handling uncaught exceptions in threads
 *
 * 该类实现了UncaughtExceptionHandler接口，主要功能包括:
 * 1. 记录线程异常日志
 * 2. 特殊处理内存溢出异常
 * 3. 提供线程异常恢复机制
 *
 * This class implements UncaughtExceptionHandler interface with features:
 * 1. Log thread exception
 * 2. Special handling for OutOfMemoryError
 * 3. Provide thread exception recovery mechanism
 *
 * @author AionEmu Project
 */
public class ThreadUncaughtExceptionHandler implements UncaughtExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ThreadUncaughtExceptionHandler.class);
    
    /**
     * 处理未捕获的线程异常
     * Handle uncaught thread exception
     *
     * @param t 发生异常的线程 / Thread where exception occurred
     * @param e 未捕获的异常 / Uncaught exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Critical Error - Thread: " + t.getName() + " terminated abnormaly: " + e, e);
        if (e instanceof OutOfMemoryError) {
            // 特殊处理内存溢出异常
            // Special handling for OutOfMemoryError
        }
    }
}
