package com.aionl.slf4j.conversion;

import ch.qos.logback.core.FileAppender;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志文件压缩备份工具类
 * Log file compression and backup utility class
 *
 * 该类继承自FileAppender，用于在日志文件滚动时将其压缩为zip格式并备份
 * This class extends FileAppender to compress and backup log files in zip format during log rotation
 */
public class TruncateToZipFileAppender extends FileAppender<Object> {
    private static final Logger log = LoggerFactory.getLogger(TruncateToZipFileAppender.class);
    
    /**
     * 备份目录路径
     * Backup directory path
     */
    private String backupDir = "log/backup";

    /**
     * 打开新的日志文件，如果文件已存在则先进行压缩备份
     * Open a new log file, compress and backup if file already exists
     *
     * @param fname 日志文件名 / Log filename
     * @throws IOException 文件操作异常 / File operation exception
     */
    public void openFile(String fname) throws IOException {
        File file = new File(fname);
        if (file.exists()) {
            this.truncate(file);
        }

        super.openFile(fname);
    }

    /**
     * 将日志文件压缩为zip格式并备份
     * Compress log file to zip format and backup
     *
     * @param file 需要压缩的日志文件 / Log file to be compressed
     */
    protected void truncate(File file) {
        File backupRoot = new File(this.backupDir);
        if (!backupRoot.exists() && !backupRoot.mkdirs()) {
            log.warn("Can't create backup dir for backup storage");
        } else {
            String date = "";

            // 读取日志文件第一行获取日期
            // Read first line of log file to get date
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                date = reader.readLine().split("\f")[1];
            } catch (IOException e) {
                log.error("Error reading log file date", e);
            }

            File zipFile = new File(backupRoot, file.getName() + "." + date + ".zip");
            
            // 使用try-with-resources确保资源正确关闭
            // Use try-with-resources to ensure proper resource closure
            try (FileInputStream fis = FileUtils.openInputStream(file);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                
                // 创建zip文件条目
                // Create zip file entry
                ZipEntry entry = new ZipEntry(file.getName());
                entry.setMethod(ZipEntry.DEFLATED); // 使用压缩方式 / Use compression method
                entry.setCrc(FileUtils.checksumCRC32(file));
                zos.putNextEntry(entry);

                // 写入数据到zip文件
                // Write data to zip file
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                
                zos.closeEntry();
                
            } catch (Exception e) {
                log.warn("Can't create zip file", e);
                return;
            }

            // 删除原始日志文件
            // Delete original log file
            if (!file.delete()) {
                log.warn("Can't delete old log file " + file.getAbsolutePath());
            }
        }
    }
}
