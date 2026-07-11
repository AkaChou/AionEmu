package com.aionemu.loginserver.network.aion;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.loginserver.model.Account;

/**
 * 登录会话密钥，用于 LS 与 GS 侧的二次校验。
 * Login session key used for secondary checks on LS and GS.
 *
 * @author -Nemesiss-
 */
public class SessionKey {

    /**
     * 账号 ID，游戏服侧鉴权使用。
     * Account id used for authentication on the game server.
     */
    public final int accountId;
    /**
     * loginOk 密钥。
     * Login-ok key.
     */
    public final int loginOk;
    /**
     * playOk1 密钥。
     * Play-ok1 key.
     */
    public final int playOk1;
    /**
     * playOk2 密钥。
     * Play-ok2 key.
     */
    public final int playOk2;

    /**
     * 为账号生成新的随机会话密钥。
     * Create a new random session key for the account.
     *
     * 账号 / Account
     */
    public SessionKey(Account acc) {
        this.accountId = acc.getId();
        this.loginOk = Rnd.nextInt();
        this.playOk1 = Rnd.nextInt();
        this.playOk2 = Rnd.nextInt();
    }

    /**
     * 用给定值构造会话密钥。
     * Construct session key with the given values.
     *
     * 账号 ID / Account id
     * Login-ok key
     * Play-ok1 key
     * Play-ok2 key
     */
    public SessionKey(int accountId, int loginOk, int playOk1, int playOk2) {
        this.accountId = accountId;
        this.loginOk = loginOk;
        this.playOk1 = playOk1;
        this.playOk2 = playOk2;
    }

    /**
     * 校验 accountId 与 loginOk 是否匹配。
     * Check whether accountId and loginOk match this key.
     *
     * 账号 ID / Account id
     * Login-ok key
     *
     * @return 若 both match 则为 true / True if both match
     */
    public boolean checkLogin(int accountId, int loginOk) {
        return this.accountId == accountId && this.loginOk == loginOk;
    }

    /**
     * 校验另一会话密钥是否完全一致。
     * Check whether another session key has the same values.
     *
     * @param key 待比较密钥 / Key to compare
     * @return 完全一致则为 true / True if all fields match
     */
    public boolean checkSessionKey(SessionKey key) {
        return (playOk1 == key.playOk1 && accountId == key.accountId && playOk2 == key.playOk2 && loginOk == key.loginOk);
    }
}
