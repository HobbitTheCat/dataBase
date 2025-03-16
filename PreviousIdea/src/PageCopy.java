/**
 * Name of class: Page
 * <p>
 * Description: parent class representing the interface for interacting with pages (minimum amount of information stored)
 * <p>
 * Version: 0.5
 * <p>
 * Date 03/10
 * <p>
 * Copyright: Semenov Egor
 */

import java.nio.ByteBuffer;

public class PageCopy {
    /**
     * Meta Info:<p>
     *  type - 2 bytes,
     *  nextPage - 4 bytes,
     *  firstFree - 2 bytes,
     * <p>
     * Free place: <p>
     *  nextFreeAddress - 2 bytes,
     *  currentFreeSize - 2 bytes;
     */

    private static final short metaInfoSize = 8;
    private static final int pageSize = 4096;
    private final int pageNumber;
    public short dataSize;
    private ByteBuffer data;

    /**
     * Getters
     */
    public short getType(){return this.data.getShort(0);}
    public int getPageNumber(){return this.pageNumber;}
    public int getNextPage(){return this.data.getInt(2);}
    protected short getFirstFree(){return (short)(this.data.getShort(6));}

    protected int getIndexByOffset(int offset){return offset/this.dataSize;}
    protected int getOnPageObjectNumber(){return (PageCopy.pageSize - PageCopy.metaInfoSize)/this.dataSize;}

    /**
     * Setters
     */
    private void setFirstFree(short firstFree){this.data.putShort(6, (short)(firstFree));}
    public void setNextPage(int page){this.data.putInt(2, page);}

    protected void setCursor(int cursor){
        if (cursor + PageCopy.metaInfoSize <= PageCopy.pageSize)
            this.data.position(cursor + PageCopy.metaInfoSize);
    }

    /**
     * Read
     */

    // with cursor (cursor move automatically)
    protected byte[] readBytes(short length){
        byte[] bytes = new byte[length];
        this.data.get(bytes);
        return bytes;
    }

    protected int readInteger(){return this.data.getInt();}
    protected long readLong(){return this.data.getLong();}
    protected short readShort(){return this.data.getShort();}
    protected Address readAddress(){return new Address(this.readInteger(), this.readShort());}

    /**
     * Write
     */
    protected void writeInteger(int value){this.data.putInt(value);}
    protected void writeLong(long value){this.data.putLong(value);}
    protected void writeShort(short value){this.data.putShort(value);}
    protected void writeBytes(byte[] value){this.data.put(value);}
    protected void writeAddress(Address address){
        this.data.putInt(address.getPageNumber());
        this.data.putShort(address.getOffset());
    }

    /**
     * Constructors
     */
    public PageCopy(short type, short dataSize, int pageNumber){
        this.pageNumber = pageNumber;
        this.data = ByteBuffer.allocate(pageSize);
        this.dataSize = dataSize;
        this.data.putShort(type);               //type
        this.data.putInt(-1);             //nextPage (default -1)
        this.setFirstFree((short) 0);           //nextFree (default 0)
        for(int i = 0; i < PageCopy.pageSize; i += this.dataSize){
            if(i + this.dataSize <= PageCopy.pageSize) {
                short nextFree = (short)(i+this.dataSize);
                this.setCursor(i);
                this.writeShort(nextFree);
                this.writeShort(this.dataSize);
            }else if (i + 4 <= PageCopy.pageSize) {
                this.setCursor(i);
                this.writeShort((short) -1);
                this.writeShort((short)(PageCopy.pageSize-i));
            }else{
                i -= this.dataSize;
                this.setCursor(i);
                this.writeShort((short) -1);
                this.writeShort((short)(PageCopy.pageSize-i));
                return;
            }
        }
    }
    public PageCopy(short type, int pageNumber){
        this(type, (short)(PageCopy.pageSize - 4), pageNumber);
    }
    public PageCopy(ByteBuffer buffer, int pageNumber){
        this(buffer,(short)(PageCopy.pageSize - 4), pageNumber);
    }

    public PageCopy(ByteBuffer data, short dataSize, int pageNumber){
        this.pageNumber = pageNumber;
        this.data = data;
        this.dataSize = dataSize;
        if (this.data.limit() == this.data.capacity()) {
            this.data.flip();
        }
    }

    /**
     * Main functions
     */
    protected short getNextFreeOffset(int sizeNeeded) {
        short freeOffset = getFirstFree();
        if (freeOffset < 0 || freeOffset + 4 > PageCopy.pageSize) return -1; //нужно везде добавить выброс ошибки Not Enough Space on Page
        while (freeOffset != -1) {
            if (freeOffset + 4 > PageCopy.pageSize) return -1;
            this.setCursor(freeOffset+2);
            short size = this.readShort();
            if (size >= sizeNeeded) {
                if(size >= sizeNeeded+4){
                    System.out.println("\nAllocation phase");
                    this.setCursor(freeOffset);
                    short nextFree = this.readShort();
                    this.setCursor(freeOffset + sizeNeeded);
                    System.out.println("Cursor set on: " +  (freeOffset + sizeNeeded));
                    this.writeShort(nextFree);
                    System.out.println("Next free written: " + nextFree);
                    this.writeShort((short) (size - sizeNeeded));
                    System.out.println("Size written: " + (size-sizeNeeded));
                    this.setFirstFree((short) (freeOffset + sizeNeeded));
                    System.out.println("First free written: " + (freeOffset + sizeNeeded) + "\nEnd of allocation\n");
                }
                else {
                    this.setCursor(freeOffset);
                    this.setFirstFree(this.readShort());
                }
                this.setCursor(freeOffset); //sets cursor automatically so you don't need to
                return (short)(freeOffset);
            }
            this.setCursor(freeOffset);
            freeOffset = this.readShort();
        }
        return -1;
    }

    public void test(){
        this.setCursor(4094);
        System.out.println("Test " + this.readShort());
    }

    protected void releaseOffset(short offset){
        this.setCursor(offset);
        short firstFree = this.getFirstFree();
        if (firstFree != offset) {
            this.writeShort(firstFree);
            this.writeShort(this.dataSize);
            this.setFirstFree((short) (offset));
        } else this.setCursor(offset + 4);
    }
    protected void releaseOffset(short offset, short customDataSize){
        this.dataSize = customDataSize;
        this.releaseOffset(offset);
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
            row.append(" ".repeat(maxLength - row.length())).append("│\n");
            pageString.append(row);
        }
        pageString.append("└").append("─".repeat(maxLength-1)).append("┘");
        return pageString.toString();
    }

}
