package Pages;

import Exceptions.StorageOperationException;
import Pages.Interface.BackLinkPage;
import NewQuery.Condition;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class LongPage extends Page implements BackLinkPage<Long> {
    private static final short integerSize = 8;
    private static final short metaInfoSize = Address.ADDRESS_SIZE;
    private static final short totalSize = integerSize + metaInfoSize;
    private static final short type = 3;

    public LongPage(int pageNumber) {super(LongPage.type, LongPage.totalSize, pageNumber);}
    public LongPage(ByteBuffer data, int  pageNumber) {super(data, LongPage.totalSize, pageNumber);}

    private void validateIndex(int index){
        if(index > this.getOnPageObjectNumber() || index < 0){
            throw new StorageOperationException("Index out of bounds: " + index + " >  " + this.getOnPageObjectNumber());
        }
    }

    @Override
    public short size(){return this.getOnPageObjectNumber();}

    @Override
    public Long get(short index) {
        this.validateIndex(index);
        this.setCursor(index*(LongPage.totalSize)+ LongPage.metaInfoSize);
        return this.readLong();
    }

    private Object[] getLongByIndexWithMeta(int index){
        this.validateIndex(index);
        this.setCursor(index*(LongPage.totalSize));
        Address address = this.readAddress();
        return new Object[]{address, this.readLong()};
    }

    @Override
    public ArrayList<Address> search(Condition condition) {
        ArrayList<Address> returnAddressesList = new ArrayList<>();
        String operator = condition.operator();
        Long value = (Long) condition.value();

        for (int i = 0; i < this.getOnPageObjectNumber(); i++){
            Object[] longWithMetaInfo = this.getLongByIndexWithMeta(i);
            Long data = (Long) longWithMetaInfo[1];
            Address address = (Address)  longWithMetaInfo[0];

            if (data == null || value == null) continue;
            if (LongPage.applyCondition(data, operator, value))
                returnAddressesList.add(address);

        } return  returnAddressesList;
    }

    public static boolean applyCondition(Long data, String operator, Long value){
        return switch (operator){
            case "==" -> data.equals(value);
            case ">"-> data > value;
            case "<"-> data < value;
            case "<="-> data <= value;
            case ">="-> data >= value;
            case "!="-> !data.equals(value);
            default -> throw new StorageOperationException("Unknown operator for LongPage: " + operator);
        };
    }

    @Override
    public short add(Long value, Address objectAddress) {
        short freeAddress = super.getNextFreeOffset(LongPage.totalSize);
        if (freeAddress < 0) return -1;
        this.writeAddress(objectAddress);
        this.writeLong(value);
        return this.getIndexByOffset(freeAddress);
    }

    @Override
    public void delete(short index) {
        this.validateIndex(index);
        this.releaseOffset((short) (index*LongPage.totalSize));
    }

    @Override
    public short replace(short index, Long value, Address objectAddress) {
        this.delete(index);
        return this.add(value, objectAddress);
    }

    @Override
    public void replaceSamePlace(short index, Long value) {
        this.validateIndex(index);
        Address objectAddress = (Address) this.getLongByIndexWithMeta(index)[0];
        this.setCursor(index*LongPage.totalSize);
        this.writeAddress(objectAddress);
        this.writeLong(value);
    }

    @Override
    public String toString() {
        StringBuilder[] rows = new StringBuilder[this.getOnPageObjectNumber() + 1];
        int maxLength = 0;
        this.setCursor(0);
        for(short i = 0; i < rows.length - 1; i ++) {
            StringBuilder builder = new StringBuilder();
            builder.append("│ ");
            builder.append(this.readAddress()).append(" ");
            builder.append(this.get(i));
            if(builder.length() > maxLength) maxLength = builder.length();
            rows[i] = builder;
        }
        rows = super.addingFreeSpace(maxLength, rows);
        return (super.assemblyString(maxLength, rows));
    }
}
