package com.aionemu.commons.services.cron;

/**
 * Cron服务异常类，用于处理定时任务执行过程中的异常情况
 * Exception class for Cron Service, used to handle exceptions during scheduled task execution
 *
 * @author SunAion
 */
public class CronServiceException extends RuntimeException {
    /**
     * 序列化版本ID
     * Serialization version ID
     */
    private static final long serialVersionUID = -354186843536711803L;

    /**
     * 默认构造函数
     * Default constructor
     */
    public CronServiceException() {
    }

    /**
     * 使用指定的错误消息构造异常
     * Constructs an exception with the specified error message
     *
     * @param message 错误消息 (error message)
     */
    public CronServiceException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造异常
     * Constructs an exception with the specified error message and cause
     *
     * @param message 错误消息 (error message)
     * @param cause 导致此异常的原因 (the cause of this exception)
     */
    public CronServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因构造异常
     * Constructs an exception with the specified cause
     *
     * @param cause 导致此异常的原因 (the cause of this exception)
     */
    public CronServiceException(Throwable cause) {
        super(cause);
    }
}
