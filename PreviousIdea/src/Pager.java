import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Pager {
    private final String filePath;
    private final int pageSize = 4096;
    private boolean isValid;
    private RandomAccessFile file;

    public Pager(String filePath) {
        this.filePath = filePath;
        try {
            this.file = new RandomAccessFile(this.filePath, "rw");
            System.out.println("Length: " + this.file.length());
            this.isValid = true;
        } catch (IOException e) {
            this.isValid = false;
            e.printStackTrace();
        }
    }

    public ByteBuffer getPage(int pageNumber) {
        if (!isValid) {
            System.err.println("Pager is not valid. File could not be opened.");
            return null;
        }
        try {
            ByteBuffer page = ByteBuffer.allocate(this.pageSize);
            int offset = pageNumber * this.pageSize;

            if (file.length() < offset + this.pageSize) {
                System.err.println("File is too small");
                return page;
            }

            file.seek(offset);
            int bytesRead = file.read(page.array());

            if (bytesRead == -1) {
                System.err.println("Nothing to read");
                return page;
            }
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writePage(int pageNumber, ByteBuffer page) {
        if (!isValid) {
            System.err.println("Pager is not valid. File could not be opened.");
            return;
        }

        try {
            long offset = (long) pageNumber * this.pageSize;
            file.seek(offset);
            file.write(page.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}