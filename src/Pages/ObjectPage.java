package Pages;
import Pages.Interface.DataPage;

import java.nio.ByteBuffer;

/**
 * Name of class: ObjectPage
 * <p>
 * Description: Contains links to set values.
 * <p>
 * Version: 1.0
 * <p>
 * Date 03/30
 * <p>
 * Copyright: Lemain Mathieu
 */

public class ObjectPage extends Page implements DataPage<Address[]> {


    private final short objectLength; //number of attributes
    private static final short linkSize = Address.ADDRESS_SIZE;
    private static final short type = 1;

    public short getObjectLength(){return this.objectLength;}

    public ObjectPage(short objectNumber, int pageNumber) {
        super(ObjectPage.type, (short)(objectNumber*ObjectPage.linkSize), pageNumber);
        System.out.println("On creation new but here it's objectNumber: " + objectNumber);
        this.objectLength = objectNumber;
    }
//  Object length is the number of attributes
    public ObjectPage(ByteBuffer buffer, short pageLength, int  pageNumber) {
        super(buffer, (short)(pageLength), pageNumber);
        System.out.println("On creation with ByteBuffer length: " + pageLength);
        this.objectLength = (short) (pageLength/ObjectPage.linkSize);
    }

    @Override
    public short size(){
        return this.getOnPageObjectNumber();
    }

    @Override
    public Address[] get(short index) {
        if(index > this.getOnPageObjectNumber()) throw new IndexOutOfBoundsException("index out of bounds");
        Address[] result = new Address[this.objectLength];
        this.setCursor(index*this.objectLength*ObjectPage.linkSize);
        for(int i = 0; i < this.objectLength; i++)
            result[i] = this.readAddress();
        return result;
    }

    public Address[] getAllObjectAddresses(){
        short[] indexes = this.getAllNotFreeOffsets();
        Address[] result = new Address[indexes.length];
        for(int i = 0; i < indexes.length; i++)
            result[i] = new Address(this.getPageNumber(), indexes[i]);
        return result;
    }

    public int add(Address[] objectLinks) {
        if(objectLinks.length > this.objectLength) throw new IllegalArgumentException("Array is too long");
        short freeAddress = this.getNextFreeOffset(this.objectLength*ObjectPage.linkSize);
        if (freeAddress == -1) return -1;
        for (Address objectLink : objectLinks) this.writeAddress(objectLink);
        return this.getIndexByOffset(freeAddress);
    }

    public int allocate(){
        short freeAddress = this.getNextFreeOffset(this.objectLength*ObjectPage.linkSize); // очень внимательно проверить все что связано с this.objectLength() и super.size или как оно там
        if (freeAddress == -1) return -1;
        return this.getIndexByOffset(freeAddress);
    }

    public void insertToIndex(Address[] objectLinks, int index) {
        short offset = (short) (this.objectLength*ObjectPage.linkSize*index);
        if(objectLinks.length > this.objectLength){
            this.releaseOffset(offset);
            throw new IllegalArgumentException("Array is too long");
        }
        this.setCursor(offset);
        for(Address objectLink : objectLinks) this.writeAddress(objectLink);
    }

    public void replaceAddress(int index, int addressIndex, Address newAddress){
        this.setCursor(index + addressIndex*ObjectPage.linkSize);
        this.writeAddress(newAddress);
    }

    @Override
    public void delete(short index) {
        if(index > this.getOnPageObjectNumber()) throw new IndexOutOfBoundsException("index out of bounds");
        this.releaseOffset((short) (index*(this.objectLength*ObjectPage.linkSize)));
    }

    @Override
    public String toString() {
        StringBuilder[] rows = new StringBuilder[this.getOnPageObjectNumber() + 1];
        int maxLength = 0;
        this.setCursor(0);
        for(short i = 0; i < rows.length - 1; i++){
            StringBuilder builder = new StringBuilder();
            builder.append("│ ");
            Address[] obj = this.get(i);
            for (int j = 0; j < obj.length; j++){
//                builder.append("(").append(obj[j].getPageNumber()).append(", ").append(obj[j].getOffset()).append(")");
                builder.append(obj[j].toString());
                if (j <  obj.length-1) builder.append(" ");
            }
            if (builder.length() > maxLength) maxLength = builder.length();
            rows[i] = builder;
        }
        rows = super.addingFreeSpace(maxLength, rows);


        for(StringBuilder row : rows){
            if(row != null)
                if(row.length() > maxLength) maxLength = row.length();
        }
        return(super.assemblyString(maxLength, rows));
    }
}
