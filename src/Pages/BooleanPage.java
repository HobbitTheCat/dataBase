package Pages;

import Exceptions.StorageOperationException;
import NewQuery.Condition;
import Pages.Interface.BackLinkPage;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BooleanPage extends Page implements BackLinkPage<Boolean> {
    private static final short booleanSize = 1;
    private static final short metaInfoSize = Address.ADDRESS_SIZE;
    private static final short totalSize = booleanSize + booleanSize;
    private static final short type = 4;

    public BooleanPage(int pageNumber) {super(BooleanPage.type, BooleanPage.totalSize, pageNumber);}
    public BooleanPage(ByteBuffer data, int pageNumber) {super(data, BooleanPage.totalSize, pageNumber);}

    private void validateIndex(int index){
        if(index > this.getOnPageObjectNumber() || index < 0){
            throw new StorageOperationException("Index out of bounds: " + index + " >  " + this.getOnPageObjectNumber());
        }
    }

    @Override
    public short size(){return this.getOnPageObjectNumber();}

    @Override
    public Boolean get(short index){
        this.validateIndex(index);
        this.setCursor(index*(BooleanPage.totalSize)+ BooleanPage.metaInfoSize);
        return this.readBoolean();
    }

    private Object[] getBooleanByIndexWithMeta(int index){
        this.validateIndex(index);
        this.setCursor(index*(BooleanPage.totalSize));
        Address address = this.readAddress();
        return new Object[]{address, this.readBoolean()};
    }

    @Override
    public ArrayList<Address> search(Condition condition){
        ArrayList<Address> returnAddressesList = new ArrayList<>();
        String operator = condition.operator();
        Boolean value = (Boolean) condition.value();

        for(int i = 0; i < this.getOnPageObjectNumber(); i++){
            Object[] boolWithMetaInfo = this.getBooleanByIndexWithMeta(i);
            Boolean data = (Boolean) boolWithMetaInfo[1];
            Address address = (Address) boolWithMetaInfo[0];

            if(data == null || value == null) continue;
            if(BooleanPage.applyCondition(data, operator, value)){
                returnAddressesList.add(address);
            }
        }
        return returnAddressesList;
    }

    public static boolean applyCondition(Boolean data, String operator, Boolean value){
        return switch (operator){
            case "=="-> data.equals(value);
            case "!="-> !data.equals(value);
            default -> throw new StorageOperationException("Unknown operator for LongPage: " + operator);
        };
    }

    @Override
    public short add(Boolean value, Address objectAddress){
        short freeAddress = super.getNextFreeOffset(BooleanPage.totalSize);
        if(freeAddress < 0) return -1;
        this.writeAddress(objectAddress);
        this.writeBoolean(value);
        return this.getIndexByOffset(freeAddress);
    }

    @Override
    public void delete(short index){
        this.validateIndex(index);
        this.releaseOffset((short) (index*BooleanPage.totalSize));
    }

    @Override
    public short replace(short index, Boolean value, Address objectAddress){
        this.delete(index);
        return this.add(value,  objectAddress);
    }

    @Override
    public void replaceSamePlace(short index, Boolean value){
        this.validateIndex(index);
        Address address = (Address) this.getBooleanByIndexWithMeta(index)[0];
        this.setCursor(index*BooleanPage.totalSize);
        this.writeAddress(address);
        this.writeBoolean(value);
    }

    @Override
    public String toString(){
        StringBuilder[] rows = new StringBuilder[this.getOnPageObjectNumber() + 1];
        int maxLength = 0;
        this.setCursor(0);
        for(short i = 0; i < rows.length; i++){
            StringBuilder builder = new StringBuilder();
            builder.append("│ ");
            builder.append(this.readAddress()).append(" ");
            builder.append(this.get(i));
            if(builder.length() > maxLength) maxLength = builder.length();
            rows[i] = builder;
        }
        rows = super.addingFreeSpace(maxLength, rows);
        return super.assemblyString(maxLength, rows);
    }
}
