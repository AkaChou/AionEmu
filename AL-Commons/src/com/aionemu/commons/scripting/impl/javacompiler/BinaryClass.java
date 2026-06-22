/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.scripting.impl.javacompiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/**
 * 二进制类文件对象，用于处理编译后的类文件
 * Binary Class File Object for handling compiled class files
 *
 * 该类是一个特殊的实现，用于使javac编译器能够处理由之前的类加载器加载的类。
 * 它同时也作为已加载类的容器使用。主要功能包括：
 * 1. 存储类的二进制数据
 * 2. 提供类文件的基本操作接口
 * 3. 管理已定义的类实例
 *
 * This is a special implementation to make the javac compiler work with
 * classes loaded by previous classloader. It also serves as a container
 * for loaded classes. Main features include:
 * 1. Store binary data of classes
 * 2. Provide basic operation interface for class files
 * 3. Manage defined class instances
 *
 * @author SoulKeeper
 */
public class BinaryClass extends SimpleJavaFileObject {
	
	/**
	 * ClassName
	 */
	private final String name;
	
	/**
	 * Class data will be written here
	 */
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	/**
	 * Locaded class will be set here
	 */
	private Class<?> definedClass;
	
	/**
	 * Constructor that accepts class name as parameter
	 *
	 * @param name class name
	 */
	protected BinaryClass(String name) {
		super(URI.create("byte:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
		this.name = name;
	}
	
	/**
	 * Returns name of this class with ".class" suffix
	 *
	 * @return name of this class with ".class" suffix
	 * @deprecated
	 */
	@Deprecated
	@Override
	public String getName() {
		return name + Kind.CLASS.extension;
	}
	
	/**
	 * Creates new ByteArrayInputStream, it just wraps class binary data
	 *
	 * @return input stream for class data
	 * @throws IOException never thrown
	 */
	@Override
	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(baos.toByteArray());
	}
	
	/**
	 * Opens ByteArrayOutputStream for class data
	 *
	 * @return output stream
	 * @throws IOException never thrown
	 */
	@Override
	public OutputStream openOutputStream() throws IOException {
		return baos;
	}
	
	/**
	 * Throws {@link UnsupportedOperationException}
	 *
	 * @return nothing
	 */
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Throws {@link UnsupportedOperationException}
	 *
	 * @return nothing
	 */
	@Override
	public Writer openWriter() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Unsupported operation, always reutrns 0
	 *
	 * @return 0
	 */
	@Override
	public long getLastModified() {
		return 0;
	}
	
	/**
	 * Unsupported operation, returns false
	 *
	 * @return false
	 */
	@Override
	public boolean delete() {
		return false;
	}
	
	/**
	 * Returns class name
	 *
	 * @param path doesn't matter
	 * @return class name
	 */
	protected String inferBinaryName(Iterable<? extends File> path) {
		return name;
	}
	
	/**
	 * Returns true if {@link javax.tools.JavaFileObject.Kind#CLASS}
	 *
	 * @param simpleName doesn't matter
	 * @param kind kind to compare
	 * @return true if Kind is {@link javax.tools.JavaFileObject.Kind#CLASS}
	 */
	@Override
	public boolean isNameCompatible(String simpleName, Kind kind) {
		return Kind.CLASS.equals(kind);
	}
	
	/**
	 * Returns bytes of class
	 *
	 * @return bytes of class
	 */
	public byte[] getBytes() {
		return baos.toByteArray();
	}
	
	/**
	 * Returns class that was loaded from binary data of this object
	 *
	 * @return loaded class
	 */
	public Class<?> getDefinedClass() {
		return definedClass;
	}
	
	/**
	 * Sets class that was loaded by this object
	 *
	 * @param definedClass class that was loaded
	 */
	public void setDefinedClass(Class<?> definedClass) {
		this.definedClass = definedClass;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.tools.JavaFileObject#getKind()
	 */
	@Override
	public Kind getKind() {
		return Kind.CLASS;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof BinaryClass && name.equals(((BinaryClass) other).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
}
