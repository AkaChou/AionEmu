package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 验证码（Captcha）下发与校验结果的服务端包。
 * Server packet for captcha delivery and verification results.
 *
 * @author Cura
 */
public class SM_CAPTCHA extends AionServerPacket {

	private int type;
	private int count;
	private int size;
	private byte[] data;
	private boolean isCorrect;
	private int banTime;

	/**
	 * 下发验证码图片数据。
	 * Delivers captcha image data to the client.
	 *
	 * @param count 剩余尝试次数 / remaining attempt count
	 * @param data 验证码图片字节 / captcha image bytes
	 */
	public SM_CAPTCHA(int count, byte[] data) {
		this.type = 1;
		this.count = count;
		this.size = data.length;
		this.data = data;
	}

	/**
	 * 返回验证码校验结果与封禁时长。
	 * Returns captcha verification result and ban duration.
	 *
	 * @param isCorrect 是否通过校验 / whether verification succeeded
	 * @param banTime 错误时的封禁秒数 / ban duration in seconds on failure
	 */
	public SM_CAPTCHA(boolean isCorrect, int banTime) {
		this.type = 3;
		this.isCorrect = isCorrect;
		this.banTime = banTime;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);

		switch (type) {
		case 0x01:
			writeC(count);
			writeD(size);
			writeB(data);
			break;
		case 0x03:
			writeH(isCorrect ? 1 : 0);

			// 时间设置无法提取（正式服默认 3000 秒） / time setting can't be extracted (retail server default value:3000 sec)
			writeD(banTime);
			break;
		}
	}
}
