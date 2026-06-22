package com.aionemu.commons.scripting.impl.javacompiler;

import java.io.File;
import java.io.IOException;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;
import org.apache.commons.io.FileUtils;

/**
 * Java源文件对象，用于处理文件系统中的Java源代码文件
 * Java Source File Object for handling Java source code files in file system
 *
 * 该类继承自SimpleJavaFileObject，主要功能包括：
 * 1. 从文件系统读取Java源代码
 * 2. 提供源代码的字符序列访问
 * 3. 支持UTF-8编码的源文件读取
 *
 * This class extends SimpleJavaFileObject and provides:
 * 1. Reading Java source code from file system
 * 2. Providing character sequence access to source code
 * 3. Supporting UTF-8 encoded source file reading
 */
public class JavaSourceFromFile extends SimpleJavaFileObject {
   public JavaSourceFromFile(File file, Kind kind) {
      super(file.toURI(), kind);
   }

   public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return FileUtils.readFileToString(new File(this.toUri()));
   }
}
