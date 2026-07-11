package com.aionemu.commons.utils;

import java.util.Random;

/**
 * 基于 Mersenne Twister 算法的随机数生成器。
 * Random number generator based on the Mersenne Twister algorithm.
 */
public class MTRandom extends Random {
    private static final long serialVersionUID = -515082678588212038L;

    private static final int UPPER_MASK = Integer.MIN_VALUE;
    private static final int LOWER_MASK = Integer.MAX_VALUE;
    private static final int N = 624;
    private static final int M = 397;
    private static final int[] MAGIC = new int[]{0, -1727483681};
    private static final int MAGIC_FACTOR1 = 1812433253;
    private static final int MAGIC_FACTOR2 = 1664525;
    private static final int MAGIC_FACTOR3 = 1566083941;
    private static final int MAGIC_MASK1 = -1658038656;
    private static final int MAGIC_MASK2 = -272236544;
    private static final int MAGIC_SEED = 19650218;
    private static final long DEFAULT_SEED = 5489L;

    private transient int[] mt;
    private transient int mti;
    private transient boolean compat;
    private transient int[] ibuf;

    /**
     * 创建默认实例。
     * Create a default instance.
     */
    public MTRandom() {
        this(false);
    }

    /**
     * 创建实例，可指定兼容模式。
     * Create an instance with optional compatibility mode.
     *
     * @param compatible 是否兼容模式 / Whether compatibility mode
     */
    public MTRandom(boolean compatible) {
        super(0L);
        this.compat = false;
        this.compat = compatible;
        this.setSeed(this.compat ? DEFAULT_SEED : System.currentTimeMillis());
    }

    /**
     * 使用 long 种子创建实例。
     * Create an instance with a long seed.
     *
     * Seed
     */
    public MTRandom(long seed) {
        super(seed);
        this.compat = false;
    }

    /**
     * 使用字节数组种子创建实例。
     * Create an instance with a byte-array seed.
     *
     * @param buf 种子字节 / Seed bytes
     */
    public MTRandom(byte[] buf) {
        super(0L);
        this.compat = false;
        this.setSeed(buf);
    }

    /**
     * 使用 int 数组种子创建实例。
     * Create an instance with an int-array seed.
     *
     * @param buf 种子数组 / Seed array
     */
    public MTRandom(int[] buf) {
        super(0L);
        this.compat = false;
        this.setSeed(buf);
    }

    /**
     * 以单 int 种子初始化状态。
     * Initialize state with a single int seed.
     *
     * Seed
     */
    private void setSeed(int seed) {
        if (mt == null) {
            mt = new int[N];
        }
        mt[0] = seed;
        for (mti = 1; mti < N; mti++) {
            mt[mti] = MAGIC_FACTOR1 * (mt[mti - 1] ^ (mt[mti - 1] >>> 30)) + mti;
        }
    }

    /**
     * 设置 long 种子（兼容模式退化为 int 种子）。
     * Set a long seed (compat mode falls back to int seed).
     *
     * Seed
     */
    @Override
    public synchronized void setSeed(long seed) {
        if (compat) {
            setSeed((int) seed);
        } else {
            if (ibuf == null) {
                ibuf = new int[2];
            }
            ibuf[0] = (int) seed;
            ibuf[1] = (int) (seed >>> 32);
            setSeed(ibuf);
        }
    }

    /**
     * 使用字节数组设置种子。
     * Set seed from a byte array.
     *
     * @param buf 字节数组 / Byte array
     */
    public final void setSeed(byte[] buf) {
        setSeed(pack(buf));
    }

    /**
     * 使用 int 数组设置种子。
     * Set seed from an int array.
     *
     * @param buf 整数数组 / Int array
     */
    public final synchronized void setSeed(int[] buf) {
        int length = buf.length;
        if (length == 0) {
            throw new IllegalArgumentException("Seed buffer may not be empty");
        }

        int i = 1;
        int j = 0;
        int k = Math.max(N, length);
        setSeed(MAGIC_SEED);

        for (; k > 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * MAGIC_FACTOR2)) + buf[j] + j;
            i++;
            j++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
            if (j >= length) {
                j = 0;
            }
        }

        for (k = N - 1; k > 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * MAGIC_FACTOR3)) - i;
            i++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
        }
        mt[0] = UPPER_MASK;
    }

    /**
     * 生成下一批随机位。
     * Generate the next random bits.
     *
     * Bit count
     * Random bits
     */
    @Override
    protected final synchronized int next(int bits) {
        if (mti >= N) {
            int kk;
            for (kk = 0; kk < N - M; kk++) {
                int y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                mt[kk] = mt[kk + M] ^ (y >>> 1) ^ MAGIC[y & 1];
            }

            for (; kk < N - 1; kk++) {
                int y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ MAGIC[y & 1];
            }

            int y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ MAGIC[y & 1];
            mti = 0;
        }

        int y = mt[mti++];
        y ^= y >>> 11;
        y ^= (y << 7) & MAGIC_MASK1;
        y ^= (y << 15) & MAGIC_MASK2;
        y ^= y >>> 18;

        return y >>> (32 - bits);
    }

    /**
     * 将字节数组打包为 int 数组。
     * Pack a byte array into an int array.
     *
     * @param buf 字节数组 / Byte array
     * Int array
     */
    public static int[] pack(byte[] buf) {
        int blen = buf.length;
        int ilen = (buf.length + 3) >>> 2;
        int[] ibuf = new int[ilen];
        for (int n = 0; n < ilen; n++) {
            int m = (n + 1) << 2;
            if (m > blen) {
                m = blen;
            }
            m--;

            int k = buf[m] & 0xff;
            while ((m & 3) != 0) {
                k = (k << 8) | (buf[--m] & 0xff);
            }
            ibuf[n] = k;
        }
        return ibuf;
    }
}
