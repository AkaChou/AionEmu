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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.ExitCode;

/**
 * 死锁检测器，用于检测和处理Java线程死锁
 * Dead Lock Detector for detecting and handling Java thread deadlocks
 *
 * 该类通过ThreadMXBean监控线程状态，定期检查是否存在死锁。
 * 当检测到死锁时，可以选择不同的处理策略:
 * 1. 仅记录日志(NOTHING)
 * 2. 重启服务器(RESTART)
 * 
 * This class monitors thread states through ThreadMXBean and periodically checks for deadlocks.
 * When a deadlock is detected, different handling strategies can be chosen:
 * 1. Only log the deadlock (NOTHING)
 * 2. Restart the server (RESTART)
 *
 * @author -Nemesiss-, ATracer
 */
public class DeadLockDetector extends Thread {
    
    private static final Logger log = LoggerFactory.getLogger(DeadLockDetector.class);
    
    /**
     * 死锁处理策略:不做任何处理
     * Deadlock handling strategy: Do nothing
     */
    public static final byte NOTHING = 0;
    
    /**
     * 死锁处理策略:重启服务器
     * Deadlock handling strategy: Restart server
     */
    public static final byte RESTART = 1;
    
    /**
     * 检查死锁的时间间隔(毫秒)
     * Time interval to check for deadlocks (milliseconds)
     */
    private final int sleepTime;
    
    /**
     * 线程管理Bean，用于检测死锁
     * Thread management bean for deadlock detection
     */
    private final ThreadMXBean tmx;
    
    /**
     * 死锁处理策略
     * Deadlock handling strategy
     */
    private final byte doWhenDL;
    
    /**
     * 创建新的死锁检测器
     * Create new DeadLock Detector
     *
     * @param sleepTime 检查间隔(秒) / Check interval in seconds
     * @param doWhenDL 处理策略 / Handling strategy
     */
    public DeadLockDetector(final int sleepTime, final byte doWhenDL) {
        super("DeadLockDetector");
        this.sleepTime = sleepTime * 1000;
        this.tmx = ManagementFactory.getThreadMXBean();
        this.doWhenDL = doWhenDL;
    }
    
    /**
     * 运行死锁检测
     * Run deadlock detection
     */
    @Override
    public final void run() {
        boolean deadlock = false;
        while (!deadlock) {
            try {
                long[] ids = tmx.findDeadlockedThreads();
                
                if (ids != null) {
                    deadlock = true;
                    ThreadInfo[] tis = tmx.getThreadInfo(ids, true, true);
                    String info = "DeadLock Found!\n";
                    for (ThreadInfo ti : tis) {
                        info += ti.toString();
                    }
                    
                    for (ThreadInfo ti : tis) {
                        LockInfo[] locks = ti.getLockedSynchronizers();
                        MonitorInfo[] monitors = ti.getLockedMonitors();
                        if (locks.length == 0 && monitors.length == 0) {
                            continue;
                        }
                        
                        ThreadInfo dl = ti;
                        info += "Java-level deadlock:\n";
                        info += createShortLockInfo(dl);
                        while ((dl = tmx.getThreadInfo(new long[] {dl.getLockOwnerId()}, true, true)[0]).getThreadId() != ti.getThreadId()) {
                            info += createShortLockInfo(dl);
                        }
                        
                        info += "\nDumping all threads:\n";
                        for (ThreadInfo dumpedTI : tmx.dumpAllThreads(true, true)) {
                            info += printDumpedThreadInfo(dumpedTI);
                        }
                    }
                    log.warn(info);
                    
                    if (doWhenDL == RESTART) {
                        System.exit(ExitCode.CODE_RESTART);
                    }
                }
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                log.warn("DeadLockDetector: " + e, e);
            }
        }
    }
    
    /**
     * 创建简短的锁信息描述
     * Create short lock information description
     * 
     * 示例 Example:
     * Java-level deadlock:
     * Thread-0 is waiting to lock java.lang.Object@276af2 which is held by main. Locked synchronizers:0 monitors:1
     * main is waiting to lock java.lang.Object@fa3ac1 which is held by Thread-0. Locked synchronizers:0 monitors:1
     *
     * @param threadInfo 线程信息 / Thread information
     * @return 格式化的锁信息 / Formatted lock information
     */
    private String createShortLockInfo(ThreadInfo threadInfo) {
        StringBuilder sb = new StringBuilder("\t");
        sb.append(threadInfo.getThreadName());
        sb.append(" is waiting to lock ");
        sb.append(threadInfo.getLockInfo().toString());
        sb.append(" which is held by ");
        sb.append(threadInfo.getLockOwnerName());
        sb.append(". Locked synchronizers:");
        sb.append(threadInfo.getLockedSynchronizers().length);
        sb.append(" monitors:");
        sb.append(threadInfo.getLockedMonitors().length);
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 打印完整的线程信息(包括简短信息和堆栈跟踪)
     * Print full thread information (including short info and stack trace)
     * 
     * 示例 Example:
     * "Thread-0" Id=10 BLOCKED
     * at com.aionemu.gameserver.DeadlockTest$1$1.run(DeadlockTest.java:70)
     * - locked java.lang.Object@fa3ac1
     * at java.lang.Thread.run(Thread.java:662)
     *
     * @param threadInfo 线程信息 / Thread information
     * @return 格式化的线程信息 / Formatted thread information
     */
    private String printDumpedThreadInfo(ThreadInfo threadInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\"" + threadInfo.getThreadName() + "\"" + " Id=" + threadInfo.getThreadId() + " " + threadInfo.getThreadState() + "\n");
        StackTraceElement[] stacktrace = threadInfo.getStackTrace();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            sb.append("\t" + "at " + ste.toString() + "\n");
            for (MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked " + mi);
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }
}
