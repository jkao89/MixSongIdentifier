package main.model;

import com.acrcloud.utils.ACRCloudExtrTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MixObj {
    private File file;
    private byte[] fileBuffer;
    private int fileBufferLen;
    private int fileDurationMS;

    public MixObj(File file) {
        this.file = file;
        loadFileIntoBuffer();
        fileDurationMS = ACRCloudExtrTool.getDurationMSByFile(file.getAbsolutePath());
    }

    private void loadFileIntoBuffer() {
        fileBuffer = new byte[(int)file.length()];
        fileBufferLen = 0;
        try (FileInputStream fin = new FileInputStream(file)) {
            fileBufferLen = fin.read(fileBuffer, 0, fileBuffer.length);
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    public byte[] getFileBuffer() {
        return fileBuffer;
    }

    public int getFileBufferLen() {
        return fileBufferLen;
    }

    public int getFileDurationMS() {
        return fileDurationMS;
    }
}
