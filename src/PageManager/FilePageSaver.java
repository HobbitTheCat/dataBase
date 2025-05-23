package PageManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Name of class: FilePageSaver
 * <p>
 * Description: Saves a page (the page's byte buffer) inside a file
 * <p>
 * Version: 2.0
 * <p>
 * Date 03/16
 * <p>
 * Copyright: Lemain Mathieu
 */

public class FilePageSaver implements MemoryManager.PageSaver{
    private final String filePath;
    private final int pageSize = 4096;

    public FilePageSaver(String filePath){
        this.filePath = filePath;
    }

    @Override
    public void save(int pageIndex, ByteBuffer data){
        try(RandomAccessFile file = new RandomAccessFile(filePath, "rw")){
            FileChannel channel = file.getChannel();

            channel.position((long) pageIndex * data.capacity());
            data.rewind();
            channel.write(data);
        }catch(Exception e){ throw new RuntimeException("Error saving file page: ",e); }
    }

    public void expandFileIfNeeded(int totalPage) {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
            long expectedSize = ((long) totalPage + 1) * pageSize;
            if (file.length() < expectedSize) {
                file.setLength(expectedSize); // OS-level file expanding
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
