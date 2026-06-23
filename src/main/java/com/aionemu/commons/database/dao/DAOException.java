package com.aionemu.commons.database.dao;

/**
 * DAO异常基类
 * Base DAO Exception
 *
 * 这个类是所有DAO相关异常的基类，继承自RuntimeException。
 * This is the base class for all DAO related exceptions, extending RuntimeException.
 * 它用于封装在DAO操作过程中可能发生的各种异常。
 * It is used to encapsulate various exceptions that may occur during DAO operations.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public class DAOException extends RuntimeException {

    /**
     * 序列化版本ID
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
     * @param message 错误消息 / Error message
     */
    public DAOException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造异常
     * Constructs exception with specified message and cause
     *
     * @param message 错误消息 / Error message
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
