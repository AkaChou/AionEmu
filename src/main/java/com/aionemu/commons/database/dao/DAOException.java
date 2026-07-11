package com.aionemu.commons.database.dao;

/**
 * DAO 异常基类
 * Base DAO Exception
 *
 * 这个类是所有 DAO 相关异常的基类，继承自 RuntimeException。
 * This is the base class for all DAO related exceptions, extending RuntimeException.
 * 它用于封装在 DAO 操作过程中可能发生的各种异常。
 * It is used to encapsulate various exceptions that may occur during DAO operations.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public class DAOException extends RuntimeException {

    /**
 * 序列化版本 ID
     * Serialization version ID
     */
    private static final long serialVersionUID = 7637014806313099318L;

    /**
     * 默认构造函数
     * Default constructor
     */
    public DAOException() {
    }

    /**
     * 使用指定的错误消息构造异常
     * Constructs exception with specified message
     *
     * Error message
     */
    public DAOException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造异常
     * Constructs exception with specified message and cause
     *
     * Error message
     * @param cause 异常原因 / Cause of exception
     */
    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因构造异常
     * Constructs exception with specified cause
     *
     * @param cause 异常原因 / Cause of exception
     */
    public DAOException(Throwable cause) {
        super(cause);
    }
}
