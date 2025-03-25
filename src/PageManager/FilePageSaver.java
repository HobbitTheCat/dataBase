package PageManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
                file.setLength(expectedSize); // OS-level увеличение файла
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
