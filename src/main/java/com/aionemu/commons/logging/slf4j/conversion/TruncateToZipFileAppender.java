package com.aionemu.commons.logging.slf4j.conversion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
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
/**
 * 日志文件压缩备份工具类
 * Log file compression and backup utility class
 *
 * 该类继承自 FileAppender，用于在日志文件滚动时将其压缩为 zip 格式并备份
 * This class extends FileAppender to compress and backup log files in zip format during log rotation
 */
@Slf4j
public class TruncateToZipFileAppender extends FileAppender<Object> {
    
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
 * 将日志文件压缩为 zip 格式并备份
     * Compress log file to zip format and backup
     *
     * @param file 需要压缩的日志文件 / Log file to be compressed
     */
    protected void truncate(File file) {
        File backupRoot = new File(this.backupDir);
        if (!backupRoot.exists() && !backupRoot.mkdirs()) {
            log.warn(I18n.get("log.9e0cf8b88e2a"));
        } else {
            String date = "";

            // 读取日志文件第一行以获取日期
            // Read first line of log file to get date
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                date = reader.readLine().split("\f")[1];
            } catch (IOException e) {
                log.error(I18n.get("log.1004c98fd450", e));
            }

            File zipFile = new File(backupRoot, file.getName() + "." + date + ".zip");
            
            // 使用 try-with-resources 确保资源正确关闭
            // Use try-with-resources to ensure proper resource closure
            try (FileInputStream fis = FileUtils.openInputStream(file);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                
                // 创建 zip 文件条目
                // Create zip file entry
                ZipEntry entry = new ZipEntry(file.getName());
                entry.setMethod(ZipEntry.DEFLATED); // 使用压缩方式 / Use compression method
                entry.setCrc(FileUtils.checksumCRC32(file));
                zos.putNextEntry(entry);

                // 将数据写入 zip 文件
                // Write data to zip file
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                
                zos.closeEntry();
                
            } catch (Exception e) {
                log.warn(I18n.get("log.a0e571dbdb33", e));
                return;
            }

            // 删除原始日志文件
            // 删除原始日志文件 / Delete original log file
            if (!file.delete()) {
                log.warn(I18n.get("log.0317a437492f", file.getAbsolutePath()));
            }
        }
    }
}
