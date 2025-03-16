import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FilePageSaver implements VirtualMemoryManager.PageSaver{
    private final String filePath;

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
}
