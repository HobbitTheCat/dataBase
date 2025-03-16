import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FilePageLoader implements VirtualMemoryManager.PageLoader{
    private final String filePath;
    private final int pageSize = 4096;

    public FilePageLoader(String filePath){
        this.filePath = filePath;
    }

    @Override
    public ByteBuffer load(int pageIndex) {
        try(RandomAccessFile file = new RandomAccessFile(filePath,"r");
            FileChannel channel = file.getChannel()){
            ByteBuffer buffer = ByteBuffer.allocate(pageSize);
            channel.position((long) pageIndex * pageSize);
            channel.read(buffer);
            return buffer;
        }catch(Exception e){ throw new RuntimeException("Error saving file page: ",e); }
    }
}
