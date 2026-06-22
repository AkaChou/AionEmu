package com.aionemu.commons.database.dao;

/**
 * DAO未找到异常
 * DAO Not Found Exception
 *
 * 当请求的DAO实现类未找到时抛出此异常。
 * This exception is thrown when the requested DAO implementation
 * cannot be found in the DAO registry.
 *
 * @author SoulKeeper
 * @author Saelya
 */
public class DAONotFoundException extends DAOException {

    /**
     * 序列化版本ID
     * Serialization version ID
     */
    private static final long serialVersionUID = 4241980426435305296L;

    /**
     * 默认构造函数
     * Default constructor
     */
    public DAONotFoundException() {
    }

    /**
     * 使用指定的错误消息构造异常
     * Constructs exception with specified message
     *
     * @param message 错误消息 / Error message
     */
    public DAONotFoundException(String message) {
        super(message);
    }

    /**
     * 使用指定的错误消息和原因构造异常
     * Constructs exception with specified message and cause
     *
     * @param message 错误消息 / Error message
     * @param cause 异常原因 / Cause of exception
     */
    public DAONotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的原因构造异常
     * Constructs exception with specified cause
     *
     * @param cause 异常原因 / Cause of exception
     */
    public DAONotFoundException(Throwable cause) {
        super(cause);
    }
}
