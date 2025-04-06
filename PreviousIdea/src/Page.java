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

public class Page {
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

    private static final int pageSize = 4096;
    private static final short metaInfoSize = 8;
    private final int pageNumber;
    private short dataSize;
    private ByteBuffer data;

    /**
     * Getters
     */
    public short getType(){return this.data.getShort(0);}
    public int getPageNumber(){return this.pageNumber;}
    public int getNextPage(){return this.data.getInt(2);}
    private short getFirstFreeInternal(){return this.data.getShort(6);}
    protected short getFirstFree(){return (short)(this.data.getShort(6) - Page.metaInfoSize);}

    protected int getIndexByOffset(int offset){return offset/this.dataSize;}
    protected int getOnPageObjectNumber(){return (Page.pageSize - Page.metaInfoSize)/this.dataSize;}

    /**
     * Setters
     */
    private void setFirstFree(short firstFree){this.data.putShort(6, (short)(firstFree));}
    public void setNextPage(int page){this.data.putInt(2, page);}

    protected void setCursor(int cursor){
        if (cursor + Page.metaInfoSize <= Page.pageSize)
            this.data.position(cursor + Page.metaInfoSize);
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

    /**
     * Write
     */
    protected void writeInteger(int value){this.data.putInt(value);}
    protected void writeLong(long value){this.data.putLong(value);}
    protected void writeShort(short value){this.data.putShort(value);}
    protected void writeBytes(byte[] value){this.data.put(value);}

    /**
     * Constructors
     */
    public Page(short type, short dataSize, int pageNumber){
        this.pageNumber = pageNumber;
        this.data = ByteBuffer.allocate(pageSize);
        this.dataSize = dataSize;
        this.data.putShort(type);               //type
        this.data.putInt(-1);             //nextPage (default -1)
        this.setFirstFree(Page.metaInfoSize);   //nextFree (default 8)
        for(int i = Page.metaInfoSize; i < Page.pageSize; i += this.dataSize){
            if(i + this.dataSize <= Page.pageSize) {
                short nextFree = (short)(i+this.dataSize);
                this.data.putShort(i, nextFree);
                this.data.putShort(i+2, this.dataSize);
            }else if (i + 4 <= Page.pageSize) {
                this.data.putShort(i, (short) -1);
                this.data.putShort(i+2, (short)(Page.pageSize-i));
            }else{
                i -= this.dataSize;
                this.data.putShort(i, (short) -1);
                this.data.putShort(i+2, (short)(Page.pageSize-i));
                return;
            }
        }
    }
    public Page(short type, int pageNumber){
        this(type, (short)(Page.pageSize - Page.metaInfoSize - 4), pageNumber);
    }
    public Page(ByteBuffer buffer, int pageNumber){
        this(buffer,(short)(Page.pageSize - Page.metaInfoSize - 4), pageNumber);
    }

    public Page(ByteBuffer data, short dataSize, int pageNumber){
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
        short freeOffset = getFirstFreeInternal();
        if (freeOffset < 0 || freeOffset + 4 > Page.pageSize) return -1;
        while (freeOffset != -1) {
            if (freeOffset + 4 > Page.pageSize) return -1;
            short size = this.data.getShort(freeOffset + 2);
            if (size >= sizeNeeded) {
                if(size >= sizeNeeded+4){
                    this.setCursor(freeOffset + sizeNeeded - Page.metaInfoSize);
                    this.writeShort(this.data.getShort(freeOffset));
                    this.writeShort((short) (size - sizeNeeded));
                    this.setFirstFree((short) (freeOffset + sizeNeeded));
                }
                else this.setFirstFree(this.data.getShort(freeOffset));
                this.setCursor(freeOffset - Page.metaInfoSize); //sets cursor automatically so you don't need to
                return (short)(freeOffset - Page.metaInfoSize);
            }
            freeOffset = data.getShort(freeOffset);
        }
        return -1;
    }

    protected void releaseOffset(short offset){
        this.setCursor(offset);
        short firstFree = this.getFirstFreeInternal();
        if (firstFree != offset + Page.metaInfoSize) {
            this.writeShort(firstFree);
            this.writeShort(this.dataSize);
            this.setFirstFree((short) (offset + Page.metaInfoSize));
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
        line.insert(0, "│ First free: " + this.getFirstFreeInternal());
        line.append(" ".repeat(length - line.length()));
        metaInfo.append(line).append("│\n│Main info:").append(" ".repeat(length - 11)).append("│\n");
        return metaInfo;
    }
    protected StringBuilder[] addingFreeSpace(int maxLength, StringBuilder[] rows) {
        this.setCursor(this.getFirstFree());
        for (int i = this.getFirstFree(); i > -1; this.setCursor(i), i = this.readShort() - Page.metaInfoSize) {
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
