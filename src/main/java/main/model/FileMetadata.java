package main.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Metadata for shared files in the Resources tab
 */
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fileId;           // Unique identifier
    private String fileName;         // Original file name
    private long fileSize;           // Size in bytes
    private String uploader;         // Username who uploaded
    private LocalDateTime uploadTime;
    private String fileType;         // Extension or MIME type
    private String filePath;         // Storage path on server
    
    public FileMetadata() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public FileMetadata(String fileId, String fileName, long fileSize, String uploader) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploader = uploader;
        this.uploadTime = LocalDateTime.now();
        this.fileType = getFileExtension(fileName);
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "unknown";
    }
    
    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public String getFormattedUploadTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return uploadTime.format(formatter);
    }
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.fileType = getFileExtension(fileName);
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getUploader() {
        return uploader;
    }
    
    public void setUploader(String uploader) {
        this.uploader = uploader;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Uploaded by %s on %s", 
            fileName, getFormattedSize(), uploader, getFormattedUploadTime());
    }
}
