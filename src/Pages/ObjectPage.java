package Pages;
import Interface.*;
import java.nio.ByteBuffer;

public class ObjectPage extends Page implements DataPage<Address[]>{
    private final short objectLength; //number of attributes
    private static final short linkSize = 6;
    private static final short type = 1;

    public ObjectPage(short objectNumber, int pageNumber) {
        super(ObjectPage.type, (short)(objectNumber*ObjectPage.linkSize), pageNumber);
        this.objectLength = objectNumber;
    }
//  Object length is the number of attributes
    public ObjectPage(ByteBuffer buffer, short objectNumber, int  pageNumber) {
        super(buffer, (short)(objectNumber*ObjectPage.linkSize), pageNumber);
        this.objectLength = objectNumber;
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

    public int append(Address[] objectLinks) {
        if(objectLinks.length > this.objectLength) throw new IllegalArgumentException("Array is too long");
        short freeAddress = this.getNextFreeOffset(this.objectLength*ObjectPage.linkSize);
        if (freeAddress == -1) throw new IndexOutOfBoundsException("Free address not found");
        for (Address objectLink : objectLinks) this.writeAddress(objectLink);
        return this.getIndexByOffset(freeAddress);
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
                builder.append("(").append(obj[j].getPageNumber()).append(", ").append(obj[j].getOffset()).append(")");
                if (j <  obj.length-1) builder.append(" ");
            }
            if (builder.length() > maxLength) maxLength = builder.length();
            rows[i] = builder;
        }
        rows = super.addingFreeSpace(maxLength, rows);
        return(super.assemblyString(maxLength, rows));
    }
}
