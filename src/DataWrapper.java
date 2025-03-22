import java.nio.ByteBuffer;

public class DataWrapper {
    private ByteBuffer page;
    private final int version;

    public DataWrapper(ByteBuffer page, int version) {
        this.page = page;
        this.version = version;
    }

    public ByteBuffer getPage() {
        return page;
    }
    public int getVersion() {
        return version;
    }

    public void replacePage(ByteBuffer newPage) {
        this.page = newPage;
    }
}
