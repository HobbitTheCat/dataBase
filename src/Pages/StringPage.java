package Pages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import Exceptions.StorageOperationException;
import Interface.*;
import NewQuery.Condition;

public class StringPage extends Page implements BackLinkPage<String> {
    private static final short stringSize = 64;
    private static final short metaInfoSize = 6;
    private static final short totalSize = stringSize + metaInfoSize;
    private static final short type = 2;

    public StringPage(int pageNumber) {
        super(StringPage.type, StringPage.totalSize, pageNumber);
    }
    public StringPage(ByteBuffer data, int pageNumber){super(data,StringPage.totalSize, pageNumber);}

    private void validateIndex(int index){
        if(index > this.getOnPageObjectNumber() || index < 0){
            throw new StorageOperationException("Index out of bounds: " + index + " >  " + this.getOnPageObjectNumber());
        }
    }
    private void validateSize(byte[] bytes){
        if(bytes.length > StringPage.stringSize){
            throw new StorageOperationException("String is too long: " + bytes.length + " > " + StringPage.stringSize);
        }
    }
    @Override
    public short size(){return this.getOnPageObjectNumber();}

    @Override
    public String get(short index) {
        this.validateIndex(index);
        this.setCursor(index*(StringPage.totalSize)+ StringPage.metaInfoSize);
        byte[] bytes = this.readBytes(StringPage.stringSize);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    public Object[] getStringByIndexWithMeta(int index) {
        this.validateIndex(index);
        this.setCursor(index*(StringPage.totalSize));
        Address address = this.readAddress();
        byte[] bytes = this.readBytes(StringPage.stringSize);
        return new Object[] {address, new String(bytes, StandardCharsets.UTF_8).trim()}; // 0 element - address, 1 element String value
    }

    public Address[] searchString(String value){
        ArrayList<Address> returnAddressesList = new ArrayList<>();
        for(int i = 0; i < this.getOnPageObjectNumber(); i++){
            Object[] stringWithMetaInfo = this.getStringByIndexWithMeta(i);
            if(((String) stringWithMetaInfo[1]).equals(value))
                returnAddressesList.add((Address) stringWithMetaInfo[0]);
        }
        return returnAddressesList.toArray(new Address[0]);
    }

    @Override
    public ArrayList<Address> search(Condition condition) {
        ArrayList<Address> returnAddressesList = new ArrayList<>();
        String operator = condition.operator();
        String value = (String) condition.value();

        for (int i = 0; i < this.getOnPageObjectNumber(); i++) {
            Object[] stringWithMetaInfo = this.getStringByIndexWithMeta(i);
            String data = (String) stringWithMetaInfo[1];
            Address address = (Address) stringWithMetaInfo[0];
            if(StringPage.applyCondition(data, operator, value))
                returnAddressesList.add(address);

        }return returnAddressesList;
    }

    public static boolean applyCondition(String data, String operator, String value){
        return switch (operator) {
            case "==" -> data.equals(value);
            case "=" -> data.equalsIgnoreCase(value);
            case "!=" -> !data.equals(value);
            case "contains" -> data.contains(value);
            default -> throw new StorageOperationException("Unknown operator for StringPage: " + operator);
        };
    }


    @Override
    public short add(String string, Address objectAddress) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        this.validateSize(bytes);
        short freeAddress = this.getNextFreeOffset(StringPage.totalSize);
        if (freeAddress < 0) return -1;
        this.writeAddress(objectAddress);
        this.writeBytes(bytes);
        return getIndexByOffset(freeAddress);
    }

    @Override
    public void delete(short index) {
        this.validateIndex(index);
        this.releaseOffset((short) (index*(StringPage.totalSize)));
        byte[] bytesToWrite = new byte[StringPage.stringSize];
        Arrays.fill(bytesToWrite, (byte) 0x20);
        this.writeBytes(bytesToWrite);
    }

    @Override
    public short replace(short index, String string, Address objectAddress) {
        this.delete(index);
        return this.add(string, objectAddress);
    }

    @Override
    public void replaceSamePlace(short index, String string, Address objectAddress) {
        this.validateIndex(index);
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        this.validateSize(bytes);
        this.setCursor(index*(StringPage.totalSize));
        this.writeAddress(objectAddress);
        this.writeBytes(bytes);
    }

    @Override
    public String toString() {
        StringBuilder[] rows = new StringBuilder[this.getOnPageObjectNumber() + 1];
        int maxLength = 0;
        this.setCursor(0);
        for(short i = 0; i < rows.length-1; i++){
            StringBuilder builder = new StringBuilder();
            builder.append("│ ");
            builder.append(this.readAddress()).append(" ");
            builder.append(this.get(i));
            if(builder.length() > maxLength) maxLength = builder.length();
            rows[i] = builder;
        }
        rows = super.addingFreeSpace(maxLength, rows);
        return(super.assemblyString(maxLength, rows));
    }

}