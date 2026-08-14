package com.aionemu.gameserver.dataholders;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.makeSVNAware;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.prefixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.xml.sax.SAXException;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

/**
 * 可热重载静态数据基类，提供 Schema 加载与 XML 文件枚举辅助。
 * Abstract base for reloadable static data, providing Schema loading and XML file listing helpers.
 *
 * @author ViAl
 */
@Slf4j(access = AccessLevel.PROTECTED)
public abstract class ReloadableData {

	/**
	 * 由管理员触发，重新加载本数据容器。
	 * Reloads this data holder, triggered by an admin.
	 *
	 * @param admin 发起重载的管理员 / admin who requested the reload
	 */
	public abstract void reload(Player admin);

	/**
	 * 返回当前持有的数据列表（子类实现）。
	 * Returns the currently held data list (implemented by subclasses).
	 *
	 * @return 当前持有的数据列表（由子类实现） / Returns the currently held data list (implemented by subclasses).
	 */
	protected abstract List<?> getData();

	/**
	 * 用新列表替换当前数据（子类实现）。
	 * Replaces the current data with the given list (implemented by subclasses).
	 *
	 * @param data 新数据列表 / new data list
	 */
	protected abstract void setData(List<?> data);

	/**
	 * 按路径加载 XML Schema。
	 * Loads an XML Schema from the given path.
	 *
	 * @param xml_schema Schema 文件相对路径 / schema file relative path
	 * @return Schema 实例 / schema instance
	 */
	protected Schema getSchema(String xml_schema) {
		Schema schema = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			schema = sf.newSchema(Config.dataFile(xml_schema));
		} catch (SAXException saxe) {
			throw new Error("Error while getting schema", saxe);
		}
		return schema;
	}

	/**
	 * 枚举目录下可见的 .xml 文件，忽略以 new 开头的文件与隐藏文件。
	 * Lists visible .xml files under the root, ignoring files prefixed with "new" and hidden files.
	 *
	 * @param root 根目录 / root directory
	 * @param recursive 是否递归子目录 / whether to recurse into subdirectories
	 * @return 匹配的文件集合 / matching file collection
	 */
	protected Collection<File> listFiles(File root, boolean recursive) {
		IOFileFilter dirFilter = recursive ? makeSVNAware(HiddenFileFilter.VISIBLE) : null;
		return FileUtils.listFiles(root,
				and(and(notFileFilter(prefixFileFilter("new")), suffixFileFilter(".xml")), HiddenFileFilter.VISIBLE),
				dirFilter);
	}
}
