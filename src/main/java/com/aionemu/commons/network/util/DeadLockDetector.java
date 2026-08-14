package com.aionemu.commons.network.util;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AionProcessExit;
import com.aionemu.commons.utils.ExitCode;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;

/**
 * 死锁检测器，定期扫描并按策略处理 Java 线程死锁。
 * Deadlock detector that periodically scans and handles Java thread deadlocks.
 *
 * @author -Nemesiss-, ATracer
 */
@Slf4j
public class DeadLockDetector extends Thread {

    /**
     * 死锁处理策略：仅记录。
     * Deadlock strategy: log only.
     */
    public static final byte NOTHING = 0;

    /**
     * 死锁处理策略：重启进程。
     * Deadlock strategy: restart process.
     */
    public static final byte RESTART = 1;

    /**
     * 检查间隔（毫秒）。
     * Check interval in milliseconds.
     */
    private final int sleepTime;

    /**
     * 线程管理 Bean。
     * Thread management bean.
     */
    private final ThreadMXBean tmx;

    /**
     * 死锁处理策略。
     * Deadlock handling strategy.
     */
    private final byte doWhenDL;
    private final IntConsumer exitHandler;

    /**
     * 创建死锁检测器（默认退出处理）。
     * Create deadlock detector with default exit handler.
     *
     * @param sleepTime 检查间隔（秒） / Check interval in seconds
     * @param doWhenDL 死锁处理策略 / Deadlock handling strategy
     */
    public DeadLockDetector(final int sleepTime, final byte doWhenDL) {
        this(sleepTime, doWhenDL, AionProcessExit::exit);
    }

    /**
     * 创建死锁检测器。
     * Create deadlock detector.
     *
     * @param sleepTime 检查间隔（秒） / Check interval in seconds
     * @param doWhenDL 死锁处理策略 / Deadlock handling strategy
     * @param exitHandler 退出处理器 / Exit handler
     */
    public DeadLockDetector(final int sleepTime, final byte doWhenDL, IntConsumer exitHandler) {
        super("DeadLockDetector");
        this.sleepTime = sleepTime * 1000;
        this.tmx = ManagementFactory.getThreadMXBean();
        this.doWhenDL = doWhenDL;
        this.exitHandler = exitHandler;
    }

    /**
     * 运行死锁检测循环。
     * Run deadlock detection loop.
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

                    handleDeadlock();
                }
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                log.warn(I18n.get("log.49697309af30", e, e));
            }
        }
    }

    /**
     * 按策略处理已检测死锁。
     * Handle detected deadlock by strategy.
     */
    void handleDeadlock() {
        if (doWhenDL == RESTART) {
            exitHandler.accept(ExitCode.CODE_RESTART);
        }
    }

    /**
     * 创建简短锁等待描述。
     * Create short lock-wait description.
     *
     * @param threadInfo 线程信息 / Thread information
     * @return 格式化锁信息 / Formatted lock information
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
     * 打印完整线程转储片段。
     * Print full thread dump fragment.
     *
     * @param threadInfo 线程信息 / Thread information
     * @return 格式化线程信息 / Formatted thread information
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
