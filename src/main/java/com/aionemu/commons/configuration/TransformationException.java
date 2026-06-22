package com.aionemu.commons.configuration;

/**
 * 配置值转换异常类，用于处理配置值转换过程中的错误
 * Configuration value transformation exception class for handling errors during value transformation
 *
 * 该异常在配置值无法转换为目标类型时抛出
 * This exception is thrown when configuration values cannot be transformed to target types
 *
 * @author SunAion
 */
public class TransformationException extends RuntimeException {
    private static final long serialVersionUID = -6641235751743285902L;

    /**
     * 创建一个无参数的转换异常
     * Create a transformation exception with no parameters
     */
    public TransformationException() {
    }

    /**
     * 创建一个带有错误消息的转换异常
     * Create a transformation exception with an error message
     *
     * @param message 错误消息 Error message
     */
    public TransformationException(String message) {
        super(message);
    }

    /**
     * 创建一个带有错误消息和原因的转换异常
     * Create a transformation exception with an error message and cause
     *
     * @param message 错误消息 Error message
     * @param cause 异常原因 Exception cause
     */
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建一个带有原因的转换异常
     * Create a transformation exception with a cause
     *
     * @param cause 异常原因 Exception cause
     */
    public TransformationException(Throwable cause) {
        super(cause);
    }
}
