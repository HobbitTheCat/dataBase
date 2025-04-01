package Pages;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Name of class: Page
 * <p>
 * Description: parent class representing the interface for interacting with pages (minimum amount of information stored)
 * <p>
 * Version: 2.5
 * <p>
 * Date 03/17
 * <p>
 * Copyright: Semenov Egor
 */
public abstract class Page {
    /**
     * Meta Info:<p>
     *  type - 2 bytes,
     *  nextPage - 4 bytes,
     *  firstFree - 2 bytes,
     *  dataLength - 2 bytes, (-1 - not defined)
     * <p>
     * Free place: <p>
     *  nextFreeAddress - 2 bytes,
     *  currentFreeSize - 2 bytes;
     */

    //Resource
    private final int pageNumber;
    private volatile Thread owner = null;
    private final Lock lock = new ReentrantLock();
    private boolean isDirty = false;

    public void lock(){this.lock.lock();}
    public void unlock(){this.lock.unlock();}
    public Thread getOwner(){return this.owner;}
    public void setOwner(Thread owner){this.owner = owner;}
    private void setDirty(){this.isDirty = true;}
    public boolean isDirty(){return this.isDirty;}


    private static final short metaInfoSize = 10;
    private static final short pageSize = 4096;
    protected static final short freePageSize =  Page.pageSize - metaInfoSize; //4088
    public short dataSize;
    private final ByteBuffer data;

    /**
     *Getters
     */
    public short getType(){return this.data.getShort(0);}
    public int getPageNumber(){return this.pageNumber;}
    public int getNextPage(){return this.data.getInt(2);}
    protected short getFirstFree(){return this.data.getShort(6);}

    protected short getIndexByOffset(int offset){return (short)(offset/this.dataSize);}
    protected short getOnPageObjectNumber(){return (short) ((Page.freePageSize + 1)/this.dataSize);}

    private void setFirstFree(short firstFreeOffset){this.data.putShort(6, firstFreeOffset);}
    public void setNextPage(int page){this.data.putInt(2, page);}
    private void setDataSize(short dataSize){this.data.putShort(8, dataSize);}
    public ByteBuffer getData(){return this.data;}

    /**
     *This is a very important section,
     *it is the method because of which I rewrote this class,
     * it allows not to take into account the meta information of the page when writing links,
     * which saves the code from magic numbers
     */
    protected void setCursor(int cursor){
        if(cursor + Page.metaInfoSize <= Page.pageSize){
            this.data.position(cursor + Page.metaInfoSize);
        } else throw new IndexOutOfBoundsException("Tried to put cursor on " + cursor + " + " + Page.metaInfoSize);
    }
    public void setData(ByteBuffer data){this.data.put(data);}
    /**
     *Reading data from ByteBuffer with automatic cursor movement
     */
    protected byte[] readBytes(short length){
        byte[] result = new byte[length];
        this.data.get(result);
        return result;
    }
    protected int readInteger(){return this.data.getInt();}
    protected long readLong(){return this.data.getLong();}
    protected short readShort(){return this.data.getShort();}
    protected Address readAddress(){return new Address(this.readInteger(), this.readShort());}

    /**
     * Writing data to ByteBuffer with automatic cursor movement
     */
    protected void writeInteger(int value){this.data.putInt(value); this.setDirty();}
    protected void writeLong(long value){this.data.putLong(value); this.setDirty();}
    protected void writeShort(short value){this.data.putShort(value); this.setDirty();}
    protected void writeBytes(byte[] value){this.data.put(value); this.setDirty();}
    protected void writeAddress(Address address){
        this.data.putInt(address.getPageNumber());
        this.data.putShort(address.getOffset());
        this.setDirty();
    }

    /**
     *Constructors
     * This constructor is needed to create an empty page with initial initialization of links to free spaces
     *
     * @param dataSize if -1 -> dynamic data size
     */
    public Page(short type, short dataSize, int pageNumber){
        this.pageNumber = pageNumber;
        this.data = ByteBuffer.allocate(Page.pageSize);
        this.reformatPage(type, dataSize);
    }

    /**
     * Creating a page from ByteBuffer (as in case of reading from disk)
     */
    public Page (ByteBuffer data, short dataSize, int pageNumber){
        this.pageNumber = pageNumber;
        this.dataSize = dataSize;
        this.data = data;
    }

    /**
     * Same, but for page with dynamic size of elements.
     */
    public Page (ByteBuffer data, int pageNumber){
        this(data, (short) (Page.freePageSize - 4), pageNumber);
    }

    protected void reformatPage(short type, short dataSize){
        this.setDataSize(dataSize);
        if(dataSize == -1) this.dataSize = (short) (Page.freePageSize - 4);
        else this.dataSize = dataSize;
        this.writeShort(type);
        this.setNextPage(-1);
        this.setFirstFree((short) 0);
        for(int i = 0; i < Page.freePageSize; i+=this.dataSize){
            if(i + this.dataSize < Page.freePageSize){
                short nextFree = (short)  (i + this.dataSize);
                this.setCursor(i);
                this.writeShort(nextFree);
                this.writeShort(this.dataSize);
            }else if(i + 4 <= Page.freePageSize){
                this.setCursor(i);
                this.writeShort((short) -1);
                this.writeShort((short) (Page.freePageSize - i));
                return;
            }else{
                i -= this.dataSize;
                this.setCursor(i);
                this.writeShort((short) -1);
                this.writeShort((short) (Page.freePageSize - i));
                return;
            }
        }
    }


    /**
     * Main functions
     * Function for free space allocation (searches for the first free space and rewrites pointers)
     * @param sizeNeeded the size of the required area for data placement
     *
     */
    protected short getNextFreeOffset(int sizeNeeded){
        short freeOffset = this.getFirstFree(); //0
        if(freeOffset < 0 || freeOffset + 4 > Page.freePageSize) return -1;
        while (freeOffset != -1){
            if(freeOffset + 4 > Page.freePageSize) return -1;
            this.setCursor(freeOffset);
            short nextFree = this.readShort();      //70
            short thisFreeSize = this.readShort();  //70
            if (thisFreeSize >=  sizeNeeded){       //true
                if(thisFreeSize >= sizeNeeded + 4){
                    this.setFirstFree((short) (freeOffset + sizeNeeded));
                    this.setCursor(freeOffset + sizeNeeded);
                    this.writeShort(nextFree);
                    this.writeShort((short) (thisFreeSize - sizeNeeded));
                }else{
                    this.setFirstFree(nextFree);
                }
                this.setCursor(freeOffset);
                return freeOffset;
            }
            freeOffset = nextFree;
        }
        return -1;
    }

    /**
     * Function for freeing up space (in case of deleting information)
     * @param offset offset of space that need to be free.
     */
    protected void releaseOffset(short offset){
        this.setCursor(offset);
        short firstFree = this.getFirstFree();
        if (firstFree != offset){
            this.writeShort(firstFree);
            this.writeShort(this.dataSize);
            this.setFirstFree(offset);
        }else this.setCursor(offset + 4);
    }
    protected void releaseOffset(short offset, short customDataSize){
        this.dataSize = customDataSize;
        this.releaseOffset(offset);
    }

    /**
     * Function for finding all offsets that are in use
     * @return
     */
    protected short[] getAllNotFreeOffsets(){
        boolean[] freeOffsets = new boolean[this.getOnPageObjectNumber()];
        for(int i = this.getFirstFree(); i > -1; this.setCursor(i), i = this.readShort()) {
            this.setCursor(i + 2);
            if (this.readShort() == this.dataSize) {
                freeOffsets[i / this.dataSize] = true;
            }
        }
        int notFreeCount = 0;
        for (boolean freeOffset : freeOffsets) {
            if (!freeOffset) {
                notFreeCount++;
            }
        }
        short[] result = new short[notFreeCount];
        int count = 0;
        for(short i = 0; i < freeOffsets.length; i++) {
            if (!freeOffsets[i]) {
                result[count++] = i;
            }
        }
        return result;
    }

    /**
     * Additional functions
     */

    protected StringBuilder metaInfoString(int length) {
        StringBuilder metaInfo = new StringBuilder();
        metaInfo.append("┌").append("─".repeat(length-1)).append("┐\n");
        metaInfo.append("│Meta info:").append(" ".repeat(length-11)).append("│\n");
        StringBuilder line = new StringBuilder("│ Type: " + this.getType());
        line.append(" ".repeat(length - line.length()));
        metaInfo.append(line).append("│\n");
        line.delete(0, line.length());
        line.insert(0, "│ Next page: " + this.getNextPage());
        line.append(" ".repeat(length - line.length()));
        metaInfo.append(line).append("│\n");
        line.delete(0, line.length());
        line.insert(0, "│ First free: " + this.getFirstFree());
        line.append(" ".repeat(length - line.length()));
        metaInfo.append(line).append("│\n│Main info:").append(" ".repeat(length - 11)).append("│\n");
        return metaInfo;
    }
    protected StringBuilder[] addingFreeSpace(int maxLength, StringBuilder[] rows) {
        for (int i = this.getFirstFree(); i > -1; this.setCursor(i), i = this.readShort()) {
            this.setCursor(i);
            StringBuilder builder = new StringBuilder();
            builder.append("│ Free: Next ").append(this.readShort());
            builder.append(" Size ").append(this.readShort()).append(" ");
            if (builder.length() > maxLength) maxLength = builder.length();
            rows[this.getIndexByOffset(i)] = builder;
        }
        return  rows;
    }

    protected String assemblyString(int maxLength, StringBuilder[] rows) {
        maxLength += 1;
        StringBuilder pageString = this.metaInfoString(maxLength);
        for(StringBuilder row : rows){
            if(row != null) {
                row.append(" ".repeat(maxLength - row.length())).append("│\n");
                pageString.append(row);
            }
        }
        pageString.append("└").append("─".repeat(maxLength-1)).append("┘");
        return pageString.toString();
    }
}