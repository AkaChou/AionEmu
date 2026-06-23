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
package com.aionemu.commons.utils.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.configs.CommonsConfig;

import javolution.text.TextBuilder;

/**
 * @author NB4L1
 */
/**
 * 任务执行包装器，提供运行时间统计和异常处理
 * Task execution wrapper with runtime statistics and exception handling
 */
public class ExecuteWrapper implements Executor {

    private static final Logger log = LoggerFactory.getLogger(ExecuteWrapper.class);

    /**
     * 执行可运行任务（默认无超时警告）
     * Execute runnable task with default no timeout warning
     */
    @Override
    public void execute(Runnable runnable) {
        execute(runnable, Long.MAX_VALUE);
    }

    /**
     * 执行任务并统计运行时间
     * Execute task with runtime statistics
     * 
     * @param runnable 要执行的任务对象 / Task object to execute
     * @param maximumRuntimeInMillisecWithoutWarning 触发警告的毫秒阈值 / Warning threshold in milliseconds
     */
    public static void execute(Runnable runnable, long maximumRuntimeInMillisecWithoutWarning) {
        long begin = System.nanoTime();
        
        try {
            runnable.run();
        } catch (Throwable t) {
            log.warn("Runnable执行异常:", t);
        } finally {
            long runtimeInNanosec = System.nanoTime() - begin;
            Class<? extends Runnable> clazz = runnable.getClass();
            
            if (CommonsConfig.RUNNABLESTATS_ENABLE) {
                RunnableStatsManager.handleStats(clazz, runtimeInNanosec);
            }
            
            long runtimeInMillisec = TimeUnit.NANOSECONDS.toMillis(runtimeInNanosec);
            if (runtimeInMillisec > maximumRuntimeInMillisecWithoutWarning) {
                TextBuilder tb = TextBuilder.newInstance();
                tb.append(clazz)
                  .append(" - 执行时间: ")
                  .append(runtimeInMillisec)
                  .append("毫秒");
                log.warn(tb.toString());
            }
        }
    }
}
