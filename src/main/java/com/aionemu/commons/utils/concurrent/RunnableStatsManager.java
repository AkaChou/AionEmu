package com.aionemu.commons.utils.concurrent;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.configs.CommonsConfig;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * 运行时统计管理器：收集并导出方法执行耗时。
 * Runtime statistics manager for collecting and dumping method timings.
 *
 * @author NB4L1
 */
@SuppressWarnings("unchecked")
@Slf4j
@UtilityClass
public class RunnableStatsManager {

    /**
     * 类统计映射。
     * Class statistics map.
     */
    private static final Map<Class<?>, ClassStat> classStats = new HashMap<Class<?>, ClassStat>();

    /**
     * 类级统计信息。
     * Per-class statistics.
     */
    private static final class ClassStat {
        private final String className;
        private final MethodStat runnableStat;

        private String[] methodNames = new String[0];
        private MethodStat[] methodStats = new MethodStat[0];

        private ClassStat(Class<?> clazz) {
            className = clazz.getName().replace("com.aionemu.gameserver.", "");
            runnableStat = new MethodStat(className, "run()");

            methodNames = new String[] {"run()"};
            methodStats = new MethodStat[] {runnableStat};

            classStats.put(clazz, this);
        }

        private MethodStat getRunnableStat() {
            return runnableStat;
        }

        /**
         * 获取或创建方法统计。
         * Get or create method statistics.
         *
         * Method name
         * @param synchronizedAlready 是否已同步 / Whether already synchronized
         * Method statistics
         */
        private MethodStat getMethodStat(String methodName, boolean synchronizedAlready) {
            if ("run()".equals(methodName)) {
                return runnableStat;
            }

            for (int i = 0; i < methodNames.length; i++) {
                if (methodNames[i].equals(methodName)) {
                    return methodStats[i];
                }
            }

            if (!synchronizedAlready) {
                synchronized (this) {
                    return getMethodStat(methodName, true);
                }
            }

            methodName = methodName.intern();

            final MethodStat methodStat = new MethodStat(className, methodName);

            methodNames = Arrays.copyOf(methodNames, methodNames.length + 1);
            methodNames[methodNames.length - 1] = methodName;
            methodStats = Arrays.copyOf(methodStats, methodStats.length + 1);
            methodStats[methodStats.length - 1] = methodStat;

            return methodStat;
        }
    }

    /**
     * 方法级统计信息。
     * Per-method statistics.
     */
    private static final class MethodStat {
        private final ReentrantLock lock = new ReentrantLock();

        private final String className;
        private final String methodName;

        private long count;
        private long total;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;

        private MethodStat(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        /**
         * 累加一次运行耗时。
         * Accumulate one runtime sample.
         *
         * Runtime
         */
        private void handleStats(long runTime) {
            lock.lock();
            try {
                count++;
                total += runTime;
                min = Math.min(min, runTime);
                max = Math.max(max, runTime);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 获取或创建类统计。
     * Get or create class statistics.
     *
     * Class
     * @param synchronizedAlready 是否已同步 / Whether already synchronized
     * Class statistics
     */
    private ClassStat getClassStat(Class<?> clazz, boolean synchronizedAlready) {
        ClassStat classStat = classStats.get(clazz);

        if (classStat != null) {
            return classStat;
        }

        if (!synchronizedAlready) {
            synchronized (RunnableStatsManager.class) {
                return getClassStat(clazz, true);
            }
        }

        return new ClassStat(clazz);
    }

    /**
     * 记录 Runnable 类的运行统计。
     * Record runtime statistics for a Runnable class.
     *
     * Runnable class
     * Runtime
     */
    public void handleStats(Class<? extends Runnable> clazz, long runTime) {
        if (!CommonsConfig.RUNNABLESTATS_ENABLE) {
            return;
        }
        getClassStat(clazz, false).getRunnableStat().handleStats(runTime);
    }

    /**
     * 记录指定方法的运行统计。
     * Record runtime statistics for a named method.
     *
     * Class
     * Method name
     * Runtime
     */
    public void handleStats(Class<?> clazz, String methodName, long runTime) {
        if (!CommonsConfig.RUNNABLESTATS_ENABLE) {
            return;
        }
        getClassStat(clazz, false).getMethodStat(methodName, false).handleStats(runTime);
    }

    /**
     * 统计导出排序字段。
     * Sort fields for statistics export.
     */
    public static enum SortBy {
        /**
         * 平均耗时。
         * Average time.
         */
        AVG("average"),
        /**
         * 执行次数。
         * Execution count.
         */
        COUNT("count"),
        /**
         * 总耗时。
         * Total time.
         */
        TOTAL("total"),
        /**
         * 类名。
         * Class name.
         */
        NAME("class"),
        /**
         * 方法名。
         * Method name.
         */
        METHOD("method"),
        /**
         * 最小耗时。
         * Minimum time.
         */
        MIN("min"),
        /**
         * 最大耗时。
         * Maximum time.
         */
        MAX("max");

        private final String xmlAttributeName;

        private SortBy(String xmlAttributeName) {
            this.xmlAttributeName = xmlAttributeName;
        }

        private final Comparator<MethodStat> comparator = new Comparator<MethodStat>() {
            @Override
            @SuppressWarnings("rawtypes")
            public int compare(MethodStat o1, MethodStat o2) {
                final Comparable c1 = getComparableValueOf(o1);
                final Comparable c2 = getComparableValueOf(o2);

                if (c1 instanceof Number) {
                    return c2.compareTo(c1);
                }

                final String s1 = (String) c1;
                final String s2 = (String) c2;

                final int len1 = s1.length();
                final int len2 = s2.length();
                final int n = Math.min(len1, len2);

                for (int k = 0; k < n; k++) {
                    char ch1 = s1.charAt(k);
                    char ch2 = s2.charAt(k);

                    if (ch1 != ch2) {
                        if (Character.isUpperCase(ch1) != Character.isUpperCase(ch2)) {
                            return ch2 - ch1;
                        } else {
                            return ch1 - ch2;
                        }
                    }
                }

                final int result = len1 - len2;

                if (result != 0) {
                    return result;
                }

                switch (SortBy.this) {
                    case METHOD:
                        return NAME.comparator.compare(o1, o2);
                    default:
                        return 0;
                }
            }
        };

        @SuppressWarnings("rawtypes")
        private Comparable getComparableValueOf(MethodStat stat) {
            switch (this) {
                case AVG:
                    return stat.total / stat.count;
                case COUNT:
                    return stat.count;
                case TOTAL:
                    return stat.total;
                case NAME:
                    return stat.className;
                case METHOD:
                    return stat.methodName;
                case MIN:
                    return stat.min;
                case MAX:
                    return stat.max;
                default:
                    throw new InternalError();
            }
        }

        private static final SortBy[] VALUES = SortBy.values();
    }

    /**
     * 按默认顺序导出类统计。
     * Dump class statistics with default order.
     */
    public void dumpClassStats() {
        dumpClassStats(null);
    }

    /**
     * 按指定字段排序后导出类统计。
     * Dump class statistics sorted by the given field.
     *
     * @param sortBy 排序字段，可为 null / Sort field, may be null
     */
    public void dumpClassStats(final SortBy sortBy) {
        if (!CommonsConfig.RUNNABLESTATS_ENABLE) {
            return;
        }
        final List<MethodStat> methodStats = new ArrayList<MethodStat>();

        synchronized (RunnableStatsManager.class) {
            for (ClassStat classStat : classStats.values()) {
                for (MethodStat methodStat : classStat.methodStats) {
                    if (methodStat.count > 0) {
                        methodStats.add(methodStat);
                    }
                }
            }
        }

        if (sortBy != null) {
            Collections.sort(methodStats, sortBy.comparator);
        }

        final List<String> lines = new ArrayList<String>();

        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        lines.add("<entries>");
        lines.add("\t<!-- This XML contains statistics about execution times. -->");
        lines.add("\t<!-- Submitted results will help the developers to optimize the server. -->");

        final String[][] values = new String[SortBy.VALUES.length][methodStats.size()];
        final int[] maxLength = new int[SortBy.VALUES.length];

        for (int i = 0; i < SortBy.VALUES.length; i++) {
            final SortBy sort = SortBy.VALUES[i];

            for (int k = 0; k < methodStats.size(); k++) {
                @SuppressWarnings("rawtypes")
                final Comparable c = sort.getComparableValueOf(methodStats.get(k));

                final String value;

                if (c instanceof Number) {
                    value = NumberFormat.getInstance(Locale.ENGLISH).format(((Number) c).longValue());
                } else {
                    value = String.valueOf(c);
                }

                values[i][k] = value;

                maxLength[i] = Math.max(maxLength[i], value.length());
            }
        }

        for (int k = 0; k < methodStats.size(); k++) {
            StringBuilder sb = new StringBuilder();
            sb.append("\t<entry ");

            EnumSet<SortBy> set = EnumSet.allOf(SortBy.class);

            if (sortBy != null) {
                switch (sortBy) {
                    case NAME:
                    case METHOD:
                        appendAttribute(sb, SortBy.NAME, values[SortBy.NAME.ordinal()][k], maxLength[SortBy.NAME.ordinal()]);
                        set.remove(SortBy.NAME);

                        appendAttribute(sb, SortBy.METHOD, values[SortBy.METHOD.ordinal()][k], maxLength[SortBy.METHOD.ordinal()]);
                        set.remove(SortBy.METHOD);
                        break;
                    default:
                        appendAttribute(sb, sortBy, values[sortBy.ordinal()][k], maxLength[sortBy.ordinal()]);
                        set.remove(sortBy);
                        break;
                }
            }

            for (SortBy sort : SortBy.VALUES) {
                if (set.contains(sort)) {
                    appendAttribute(sb, sort, values[sort.ordinal()][k], maxLength[sort.ordinal()]);
                }
            }

            sb.append("/>");

            lines.add(sb.toString());
        }

        lines.add("</entries>");

        PrintStream ps = null;
        try {
            ps = new PrintStream("MethodStats-" + System.currentTimeMillis() + ".log");

            for (String line : lines) {
                ps.println(line);
            }
        } catch (Exception e) {
            log.warn(I18n.get("log.da39a3ee5e6b", e), e);
        } finally {
            IOUtils.closeQuietly(ps);
        }
    }

    /**
     * 追加 XML 属性，并按列宽填充空格。
     * Append an XML attribute with column padding.
     *
     * @param sb     输出 / Output builder
     * Field
     * Value
     * Column width
     */
    private void appendAttribute(StringBuilder sb, SortBy sortBy, String value, int fillTo) {
        sb.append(sortBy.xmlAttributeName);
        sb.append("=");

        if (sortBy != SortBy.NAME && sortBy != SortBy.METHOD) {
            for (int i = value.length(); i < fillTo; i++) {
                sb.append(" ");
            }
        }

        sb.append("\"");
        sb.append(value);
        sb.append("\" ");

        if (sortBy == SortBy.NAME || sortBy == SortBy.METHOD) {
            for (int i = value.length(); i < fillTo; i++) {
                sb.append(" ");
            }
        }
    }
}
