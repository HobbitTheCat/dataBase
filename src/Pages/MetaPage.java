package Pages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import Pages.Interface.MetaDataPage;
import TableManager.TableDescription;

/**
 * Name of class: MetaPage
 * <p>
 * Description: Type for containing data about the stored classes.
 * <p>
 * Version: 2.0
 * <p>
 * Date 03/20
 * <p>
 * Copyright: Semenov Egor
 */

public class MetaPage extends Page implements MetaDataPage {
    /**
     * Meta info object (before parameters):
     * <ul>
     *     <li>length: 2 bytes
     *     <li>parameter number: 2 bytes
     *     <li>objectPage: 4 bytes
     *     <li>className: 64 bytes
     * </ul>
     * Total: 72 bytes <p>
     * Main part:
     * <ul>
     *     <li>parameter name: 64 bytes
     *     <li>pageNumber:  4 bytes
     *     <li>...
     *     <li>...
     * </ul>
     */

    private static final short stringSize = 64;
    private static final short metaInfoSize = 8 +  stringSize;
    private static final short type = 0;
    private static final short linkSize = 4;

    public MetaPage(ByteBuffer buffer, int pageNumber) {super(buffer, pageNumber);}
    public MetaPage(int pageNumber){super(MetaPage.type, (short) -1, pageNumber);}
    public void format(MetaPage previousPage){
        super.reformatPage(MetaPage.type, (short) -1);
        previousPage.setNextPage(this.getPageNumber());
    }


    public short add(TableDescription dataClass){
        if(this.searchTableByName(dataClass.getName()) != -1) throw new IllegalArgumentException("Table " + dataClass.getName() + " already exists");
        byte[] name = dataClass.getName().getBytes(StandardCharsets.UTF_8);
        if(name.length > MetaPage.stringSize) throw new IllegalArgumentException("Table name is too long");
        byte[] bytes = new byte[MetaPage.stringSize];
        Arrays.fill(bytes, (byte)' ');
        System.arraycopy(name, 0, bytes, 0, name.length);
        byte[][]  attriBytes = new byte[dataClass.getAttributesNames().length][];
        for(int i = 0; i < dataClass.getAttributesNames().length; i++){
            byte[] nameBytes = dataClass.getAttributesNames()[i].getBytes(StandardCharsets.UTF_8);
            if(nameBytes.length > MetaPage.stringSize) throw new IllegalArgumentException("Attribute " + dataClass.getAttributesNames()[i] + "name is too long");
            attriBytes[i] = new byte[MetaPage.stringSize];
            Arrays.fill(attriBytes[i], (byte) ' ');
            System.arraycopy(nameBytes, 0, attriBytes[i], 0, nameBytes.length);
        }
        short classLength = (short)(MetaPage.metaInfoSize + dataClass.getAttributesNames().length * (MetaPage.stringSize + MetaPage.linkSize));
        if(classLength > Page.freePageSize) throw new IllegalArgumentException("Table is too long");
        short offset = this.getNextFreeOffset(classLength);
        if(offset < 0) return -1;
        this.writeShort(classLength); // length
        this.writeShort((short)(dataClass.getAttributesNames().length)); // parameterNumber
        this.writeInteger(dataClass.getObjectPage()); // objectPage
        this.writeBytes(bytes);
        for(int i = 0; i < dataClass.getAttributesNames().length; i++){
            this.writeBytes(attriBytes[i]);
            this.writeInteger(dataClass.getAttributesPages()[i]);
        }
        return offset;
    }

    public TableDescription getClassByName(String className){
        short offset = this.searchTableByName(className);
        if(offset == -1) return null;
        return this.getClassByOffset(offset);
    }

    public TableDescription getClassByOffset(short offset){
        this.setCursor(offset);
        this.readShort();
        short paramNumber = this.readShort();
        int page = this.readInteger();
        byte[] bytes = this.readBytes(MetaPage.stringSize);
        String className = new String(bytes, StandardCharsets.UTF_8).trim();
        String[] attributeNames = new String[paramNumber];
        int[] attributesPages = new int[paramNumber];
        for(int i = 0; i < paramNumber; i++){
            attributeNames[i] = new String(this.readBytes(MetaPage.stringSize), StandardCharsets.UTF_8).trim();
            attributesPages[i] = this.readInteger();
        }
        return new TableDescription(page, className, attributeNames, attributesPages);
    }

    public boolean deleteClassByName(String className){
        short offset = this.searchTableByName(className);
        if(offset == -1) return false; // do while delete == false or getNextPage = -1
        this.setCursor(offset + 2); //there rearrange to read the number of attributes of the class
        this.releaseOffset(offset, (short)(MetaPage.metaInfoSize + this.readShort() * (MetaPage.stringSize + MetaPage.linkSize)));
        return true;
    }

//    private void defragmentation(){
//        Map<Integer, Integer> pageMap = this.getPageMap();
//        int cursor = 0;
//        while(cursor != Page.freePageSize){
//
//        }
//    }

    private Map<Integer, Integer> getPageMap(){
        Map<Integer,Integer> pageMap = new HashMap<>();
        short freeAddress = this.getFirstFree();
        while (freeAddress != -1){
            this.setCursor(freeAddress);
            short nextFreeAddress = this.readShort();
            short sizeOfFreeSpace = this.readShort();
            pageMap.put((int) freeAddress, freeAddress+sizeOfFreeSpace);
            freeAddress = nextFreeAddress;
        }
        return pageMap;
    }
    public short searchTableByName(String tableName){
        byte[] tableNameBytes = tableName.getBytes(StandardCharsets.UTF_8);
        int cursor = 0;
        Map<Integer, Integer> pageMap = this.getPageMap();

        while (cursor != Page.freePageSize){
            Integer nextStep = pageMap.get(cursor);
            while(nextStep == null){
                this.setCursor(cursor);
                short currentTableDescriptionLength = this.readShort();
                this.setCursor(cursor + 8);
                byte[] readBytes = this.readBytes((short) tableNameBytes.length);
                if (readBytes.length == tableNameBytes.length && Arrays.equals(tableNameBytes, readBytes)) {
                    return  (short) cursor;
                }
                cursor += currentTableDescriptionLength;
                nextStep = pageMap.get(cursor);
            }
            cursor = nextStep;
        }
        return -1;
    }

    /**
     * Additional functions
     */
    public String toString(){
        Map<Integer,Integer> pageMap = new HashMap<>();

        ArrayList<StringBuilder> rows = new ArrayList<>();
        short freeAddress = this.getFirstFree();
        while (freeAddress != -1){
            this.setCursor(freeAddress);
            short nextFreeAddress = this.readShort();
            short sizeOfFreeSpace = this.readShort();
            StringBuilder row = new StringBuilder("│ Free index: ").append(freeAddress).append(", Next ");
            row.append(nextFreeAddress);
            row.append(", Size ").append(sizeOfFreeSpace);
            rows.add(row);
            pageMap.put((int) freeAddress, freeAddress+sizeOfFreeSpace);
            freeAddress = nextFreeAddress;
        }

        int cursor = 0;
        while (cursor != Page.freePageSize){
            Integer nextStep = pageMap.get(cursor);
            while(nextStep == null){
                this.setCursor(cursor);
                short currentTableDescriptionLength = this.readShort();
                rows.add(this.resolveMetaInfo((short) cursor));
                cursor += currentTableDescriptionLength;
                nextStep = pageMap.get(cursor);
            }
            cursor = nextStep;
        }
        int maxLength = 0;
        for (StringBuilder row : rows){
            if(row.length() > maxLength) maxLength = row.length();
        }

        return super.assemblyString(maxLength, rows.toArray(new StringBuilder[0]));
    }

    private StringBuilder resolveMetaInfo(short offset){
        StringBuilder result = new StringBuilder("│ ");
        this.setCursor(offset);
        short lengthHere = this.readShort();
        result.append(lengthHere).append(" "); //lengthHere
        result.append(this.readShort()).append(" "); //attributesNumber
        result.append(this.readInteger()).append(" "); //pageNumber
        result.append(new String(this.readBytes(MetaPage.stringSize), StandardCharsets.UTF_8).trim()).append(" "); //class name
        while (lengthHere - MetaPage.metaInfoSize > 0){ //for reading attributes names and addresses
            result.append(new String(this.readBytes(MetaPage.stringSize), StandardCharsets.UTF_8).trim());
            result.append(" : ").append(this.readInteger()).append(" ");
            lengthHere -= (MetaPage.stringSize + MetaPage.linkSize);
        }
        return result;
    }

}

