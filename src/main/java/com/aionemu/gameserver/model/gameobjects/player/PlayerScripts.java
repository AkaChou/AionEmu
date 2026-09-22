package com.aionemu.gameserver.model.gameobjects.player;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.dao.HouseScriptsDAO;
import com.aionemu.gameserver.model.house.PlayerScript;

/**
 * 玩家 Scripts 游戏对象。
 * Player Scripts game object.
 * @author Rolandas
 */
@Slf4j
public class PlayerScripts {

	private final Map<Integer, PlayerScript> scripts;
	private final int houseObjId;

	public PlayerScripts(int houseObjectId) {
		this.scripts = new HashMap<>(8);
		for (int index = 0; index < 8; index++) {
			this.scripts.put(index, new PlayerScript());
		}
		this.houseObjId = houseObjectId;
	}

	/** 返回 scripts / Returns the scripts */
	public Map<Integer, PlayerScript> getScripts() {
		return Collections.unmodifiableMap(scripts);
	}

	/** 添加 script / Adds script */
	public boolean addScript(int position, String scriptXML) {
		PlayerScript script = scripts.get(position);

		if (scriptXML == null) {
			script.setData(null, -1);
		} else if (StringUtils.EMPTY.equals(scriptXML)) {
			script.setData(new byte[0], 0);
		}
		try {
			byte[] bytes = compress(scriptXML);
			int oldLength = bytes.length;
			bytes = Arrays.copyOf(bytes, bytes.length + 8);
			for (int i = oldLength; i < bytes.length; i++) {
				bytes[i] = -51;
				// 添加 NC 特殊字节，缺失则加载失败。 / Add NC shit bytes, without which fails to load :)
			}
			script.setData(bytes, scriptXML.length() * 2);
		} catch (Exception ex) {
			log.error(I18n.get("log.3d086e850a89"), ex);
			return false;
		}
		return script == null;
	}

	/** 返回 uncompressed script / Returns the uncompressed script */
	public String getUncompressedScript(int position) {
		if (!scripts.containsKey(position)) {
			return null;
		}

		PlayerScript script = scripts.get(position);
		byte[] bytes = null;

		script.readLock();
		bytes = script.getCompressedBytes();
		script.readUnlock();

		if (bytes == null) {
			return null;
		}

		if (bytes.length == 0) {
			return StringUtils.EMPTY;
		}

		try {
			return decompress(bytes);
		} catch (Exception ex) {
			log.error(I18n.get("log.4c634fc1f594"), ex);
			return null;
		}
	}

	/** 添加 script / Adds script */
	public boolean addScript(int position, byte[] compressedXML, int uncompressedSize) {
		String content = null;
		int size = -1;

		if (compressedXML != null && compressedXML.length == 0) {
			content = StringUtils.EMPTY;
			size = 0;
		} else if (compressedXML != null) {
			try {
				content = decompress(compressedXML);
				byte[] bytes = content.getBytes(StandardCharsets.UTF_16LE);
				if (bytes.length != uncompressedSize) {
					return false;
				}
				size = uncompressedSize;
			} catch (Exception ex) {
				return false;
			}
		}

		PlayerScript script = scripts.get(position);
		script.readLock();
		byte[] bytes = script.getCompressedBytes();
		script.readUnlock();
		script.setData(compressedXML, size);

		if (bytes == null) {
			DAOManager.getDAO(HouseScriptsDAO.class).addScript(houseObjId, position, content);
		} else {
			DAOManager.getDAO(HouseScriptsDAO.class).updateScript(houseObjId, position, content);
		}

		if (HousingConfig.HOUSE_SCRIPT_DEBUG) {
			log.info(content);
		}
		return true;
	}

	/** 移除 script / Removes script */
	public boolean removeScript(int position) {
		PlayerScript script = scripts.get(position);

		script.readLock();
		byte[] bytes = script.getCompressedBytes();
		script.readUnlock();

		if (bytes == null) {
			return false;
		} else {
			script.setData(null, -1);
			DAOManager.getDAO(HouseScriptsDAO.class).deleteScript(houseObjId, position);
		}
		return true;
	}

	/** 返回大小 / Returns the size*/
	public int getSize() {
		return 8;
	}

	/**
	 * 将文本以 UTF-16LE 编码后 Deflate 压缩。
	 * Encode text as UTF-16LE and Deflate-compress it.
	 * @param text 源文本 / source text
	 * @return 压缩字节 / compressed bytes
	 * @throws Exception 压缩失败时 / on compress failure
	 */
	private static byte[] compress(String text) throws Exception {
		Deflater compressor = new Deflater();
		byte[] bytes = text.getBytes(StandardCharsets.UTF_16LE);
		compressor.setInput(bytes);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		compressor.finish();

		byte[] buffer = new byte[1024];
		try {
			while (!compressor.finished()) {
				int count = compressor.deflate(buffer);
				bos.write(buffer, 0, count);
			}
		} finally {
			compressor.end();
		}

		bos.close();
		return bos.toByteArray();
	}

	/**
	 * 解压字节数组为 UTF-16LE 字符串。
	 * Decompress a byte array into a UTF-16LE string.
	 * @param bytes 压缩数据 / compressed bytes
	 * @return 解压后的文本 / decompressed text
	 * @throws Exception 解压失败时 / on decompress failure
	 */
	private static String decompress(byte[] bytes) throws Exception {
		Inflater decompressor = new Inflater();
		decompressor.setInput(bytes);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);

		byte[] buffer = new byte[1024];
		try {
			while (true) {
				int count = decompressor.inflate(buffer);
				if (count > 0) {
					bos.write(buffer, 0, count);
				} else {
					if ((count == 0) && (decompressor.finished())) {
						break;
					}
					throw new RuntimeException("Bad zip data, size: " + bytes.length);
				}
			}
		} finally {
			decompressor.end();
		}

		bos.close();
		return bos.toString(StandardCharsets.UTF_16LE);
	}
}
