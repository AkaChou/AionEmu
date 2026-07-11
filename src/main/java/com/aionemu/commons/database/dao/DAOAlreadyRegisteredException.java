package com.aionemu.commons.database.dao;

/**
 * DAO 已注册异常
 * DAO Already Registered Exception
 *
 * 当尝试注册一个已经注册过的 DAO 实现类时抛出此异常。
 * This exception is thrown when attempting to register a DAO implementation
 * that has already been registered.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public class DAOAlreadyRegisteredException extends DAOException {

    /**
 * 序列化版本 ID
     * Serialization version ID
     */
    private static final long serialVersionUID = -4966845154050833016L;

    /**
     * 默认构造函数
     * Default constructor
     */
    public DAOAlreadyRegisteredException() {
    }

    /**
     * 使用指定的错误消息构造异常
     * Constructs exception with specified message
     *
     * Error message
     */
    public DAOAlreadyRegisteredException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造异常
     * Constructs exception with specified message and cause
     *
     * Error message
     * @param cause 异常原因 / Cause of exception
     */
    public DAOAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因构造异常
     * Constructs exception with specified cause
     *
     * @param cause 异常原因 / Cause of exception
     */
    public DAOAlreadyRegisteredException(Throwable cause) {
        super(cause);
    }
}
