package com.aionemu.commons.utils;

import java.util.Random;

/**
 * 基于Mersenne Twister算法的随机数生成器实现
 * Random number generator implementation based on Mersenne Twister algorithm
 */
public class MTRandom extends Random {
    private static final long serialVersionUID = -515082678588212038L;

    // 常量定义 / Constants definition
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

    // 实例变量 / Instance variables
    private transient int[] mt;
    private transient int mti;
    private transient boolean compat;
    private transient int[] ibuf;

    /**
     * 创建一个新的MTRandom实例
     * Create a new MTRandom instance
     */
    public MTRandom() {
        this(false);
    }

    /**
     * 创建一个新的MTRandom实例，可指定是否使用兼容模式
     * Create a new MTRandom instance with specified compatibility mode
     * 
     * @param compatible 是否使用兼容模式 / Whether to use compatibility mode
     */
    public MTRandom(boolean compatible) {
        super(0L);
        this.compat = false;
        this.compat = compatible;
        this.setSeed(this.compat ? DEFAULT_SEED : System.currentTimeMillis());
    }

    /**
     * 使用指定的种子创建MTRandom实例
     * Create MTRandom instance with specified seed
     * 
     * @param seed 随机数生成器的种子 / Seed for random number generator
     */
    public MTRandom(long seed) {
        super(seed);
        this.compat = false;
    }

    /**
     * 使用字节数组作为种子创建MTRandom实例
     * Create MTRandom instance with byte array as seed
     * 
     * @param buf 用作种子的字节数组 / Byte array used as seed
     */
    public MTRandom(byte[] buf) {
        super(0L);
        this.compat = false;
        this.setSeed(buf);
    }

    /**
     * 使用整数数组作为种子创建MTRandom实例
     * Create MTRandom instance with integer array as seed
     * 
     * @param buf 用作种子的整数数组 / Integer array used as seed
     */
    public MTRandom(int[] buf) {
        super(0L);
        this.compat = false;
        this.setSeed(buf);
    }

    /**
     * 设置随机数生成器的种子
     * Set the seed for random number generator
     * 
     * @param seed 种子值 / Seed value
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
     * 使用字节数组设置种子
     * Set seed using byte array
     * 
     * @param buf 字节数组 / Byte array
     */
    public final void setSeed(byte[] buf) {
        setSeed(pack(buf));
    }

    /**
     * 使用整数数组设置种子
     * Set seed using integer array
     * 
     * @param buf 整数数组 / Integer array
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
     * 将字节数组打包成整数数组
     * Pack byte array into integer array
     * 
     * @param buf 字节数组 / Byte array
     * @return 整数数组 / Integer array
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
