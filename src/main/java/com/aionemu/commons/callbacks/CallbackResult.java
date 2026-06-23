package com.aionemu.commons.callbacks;

/**
 * 回调结果类，用于控制回调链和目标方法的执行流程
 * Callback result class that controls the execution flow of callback chain and target method
 *
 * @param <T> 结果值的类型 / Type of the result value
 */
public class CallbackResult<T> {
    /**
     * 继续执行后续回调和目标方法
     * Continue with subsequent callbacks and target method
     */
    public static final int CONTINUE = 0;

    /**
     * 阻止后续回调执行，但允许目标方法执行
     * Block subsequent callbacks but allow target method execution
     */
    public static final int BLOCK_CALLBACKS = 1;

    /**
     * 阻止目标方法执行，但允许后续回调执行
     * Block target method execution but allow subsequent callbacks
     */
    public static final int BLOCK_CALLER = 2;

    /**
     * 阻止所有后续执行，包括回调和目标方法
     * Block all subsequent execution including callbacks and target method
     */
    public static final int BLOCK_ALL = 3;

    private static final CallbackResult INSTANCE_CONTINUE = new CallbackResult(0);
    private static final CallbackResult INSTANCE_BLOCK_CALLBACKS = new CallbackResult(1);

    /**
     * 回调结果值
     * Callback result value
     */
    private final T result;

    /**
     * 阻塞策略
     * Blocking policy
     */
    private final int blockPolicy;

    private CallbackResult(int blockPolicy) {
        this(null, blockPolicy);
    }

    private CallbackResult(T result, int blockPolicy) {
        this.result = result;
        this.blockPolicy = blockPolicy;
    }

    /**
     * 获取回调结果值
     * Get the callback result value
     *
     * @return 结果值 / Result value
     */
    public T getResult() {
        return this.result;
    }

    /**
     * 检查是否阻止后续回调执行
     * Check if subsequent callbacks are blocked
     *
     * @return 是否阻止回调 / Whether callbacks are blocked
     */
    public boolean isBlockingCallbacks() {
        return (this.blockPolicy & 1) != 0;
    }

    /**
     * 检查是否阻止目标方法执行
     * Check if target method execution is blocked
     *
     * @return 是否阻止目标方法 / Whether target method is blocked
     */
    public boolean isBlockingCaller() {
        return (this.blockPolicy & 2) != 0;
    }

    /**
     * 创建继续执行的回调结果
     * Create a callback result that continues execution
     *
     * @return 继续执行的回调结果实例 / Callback result instance for continuation
     */
    public static <T> CallbackResult<T> newContinue() {
        return INSTANCE_CONTINUE;
    }

    /**
     * 创建阻止后续回调的结果
     * Create a callback result that blocks subsequent callbacks
     *
     * @return 阻止回调的结果实例 / Callback result instance for blocking callbacks
     */
    public static <T> CallbackResult<T> newCallbackBlocker() {
        return INSTANCE_BLOCK_CALLBACKS;
    }

    /**
     * 创建完全阻止（回调和目标方法）的结果
     * Create a callback result that blocks both callbacks and target method
     *
     * @param result 结果值 / Result value
     * @return 完全阻止的结果实例 / Callback result instance for full blocking
     */
    public static <T> CallbackResult<T> newFullBlocker(T result) {
        return new CallbackResult(result, 3);
    }
}
