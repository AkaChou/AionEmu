package com.aionemu.loginserver.utils;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.IntConsumer;

import com.aionemu.commons.utils.ExitCode;
import com.aionemu.loginserver.lifecycle.LoginProcessRuntimeBridge;

/**
 * 死锁检测线程：周期性扫描 JVM 死锁，发现后记录日志并可按策略重启进程。
 * Deadlock detector thread: periodically scans for JVM deadlocks, logs details and may restart the process.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class DeadLockDetector extends Thread {

    /**
     * 发现死锁时不采取额外动作。
     * Do nothing when a deadlock is detected.
     */
    public static final byte NOTHING = 0;
    /**
     * 发现死锁时重启进程。
     * Restart the process when a deadlock is detected.
     */
    public static final byte RESTART = 1;
    /**
     * 死锁检测间隔（毫秒，构造时由秒转换）。
     * How often to check for deadlocks (milliseconds, converted from seconds at construction).
     */
    private final int sleepTime;
    /**
     * 线程管理 MXBean。
     * ThreadMXBean used for deadlock detection.
     */
    private final ThreadMXBean tmx;
    /**
     * 发现死锁时的处理策略。
     * Action to take when a deadlock is detected.
     */
    private final byte doWhenDL;
    private final IntConsumer exitHandler;

    /**
     * 使用默认退出桥创建死锁检测器（已弃用构造）。
     * Create a detector with the default exit bridge (deprecated constructor).
     *
     * @param sleepTime 检测间隔（秒） / check interval in seconds
     * @param doWhenDL 死锁处理策略 / action on deadlock
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public DeadLockDetector(int sleepTime, byte doWhenDL) {
        this(sleepTime, doWhenDL, status -> new LoginProcessRuntimeBridge().exit(status));
    }

    /**
     * 使用自定义退出处理器创建死锁检测器。
     * Create a detector with a custom exit handler.
     *
     * @param sleepTime 检测间隔（秒） / check interval in seconds
     * @param doWhenDL 死锁处理策略 / action on deadlock
     * @param exitHandler 退出回调 / exit callback
     */
    public DeadLockDetector(int sleepTime, byte doWhenDL, IntConsumer exitHandler) {
        super("DeadLockDetector");
        this.sleepTime = sleepTime * 1000;
        this.tmx = ManagementFactory.getThreadMXBean();
        this.doWhenDL = doWhenDL;
        this.exitHandler = exitHandler;
    }

    /**
     * 循环检测死锁并在发现后记录详情、执行处理策略。
     * Loop that checks for deadlocks, logs details when found and applies the configured action.
     */
    @Override
    public final void run() {
        boolean deadlock = false;
        while (!deadlock) {
            try {
                long[] ids = tmx.findDeadlockedThreads();

                if (ids != null) {
                    /**
	 * 检测到死锁。 / deadlock found :/
	 */
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
                            /**
                             * 该线程已死锁，但未必是罪魁。
	 * this thread is deadlocked but it is not necessarily guilty
                             */
                            continue;
                        }

                        ThreadInfo dl = ti;
                        info += "Java-level deadlock:\n";
                        info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
                        while ((dl = tmx.getThreadInfo(new long[]{dl.getLockOwnerId()}, true, true)[0]).getThreadId() != ti.getThreadId()) {
                        info += "\t" + dl.getThreadName() + " is waiting to lock " + dl.getLockInfo().toString() + " which is held by " + dl.getLockOwnerName() + "\n";
                        }
                    }
                    log.warn(info);

                    handleDeadlock();
                }
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                log.warn(I18n.get("log.49697309af30", e, e));
            }
        }
    }

    /**
     * 按策略处理已发现的死锁（如请求进程重启）。
     * Handle a detected deadlock according to the configured policy (e.g. request restart).
     */
    void handleDeadlock() {
        if (doWhenDL == RESTART) {
            exitHandler.accept(ExitCode.CODE_RESTART);
        }
    }
}
