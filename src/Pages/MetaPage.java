package Pages;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import Interface.*;
import TableManager.TableDescription;

//похоже нужно каждому листу присваивать его адрес, что бы можно было бы передать лист в качестве параметра
//при чем хранить индекс нужно в супер классе, что бы можно было в Address сменить pageNumber просто на Page, тогда можно будет еще и ти запрашивать
//вопрос: не приведет ли это к конфликтам при создании объектов?
//При создании класса в metaPage все остальные страницы уже созданы, но пустые, так что ок

//При создании объекта objectPage нужна инфа про страницы с аттрибутами
//При создании строки нужно дать обратную ссылку на объект - конфликт
//Варианты решения:
//Сначала создаем объект, в ObjectPage, а затем добавляем атрибуты.

public class MetaPage extends Page implements MetaDataPage{
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
        if(this.getClassOffsetByName(dataClass.getName()) != -1) throw new IllegalArgumentException("Table " + dataClass.getName() + " already exists");
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
        short offset = this.getClassOffsetByName(className);
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
        short offset = this.getClassOffsetByName(className);
        if(offset == -1) return false; // do while delete == false or getNextPage = -1
        this.setCursor(offset + 2);
        this.releaseOffset(offset, (short)(MetaPage.metaInfoSize + this.readShort() * (MetaPage.stringSize + MetaPage.linkSize)));
        return true;
    }

    public short getClassOffsetByName(String className){
        short nextFree = this.getFirstFree(); //276
        short sum = 0;
        while (nextFree != -1){
            while(sum < nextFree){
                this.setCursor(sum); //0
                short add = this.readShort();
                this.setCursor(sum + 8);
                if(className.equals(new String(this.readBytes(MetaPage.stringSize), StandardCharsets.UTF_8).trim()))
                    return sum;
                sum += add;
            }
            this.setCursor(nextFree);
            nextFree = this.readShort();
            sum += this.readShort();
        }
        return -1;
    }
    /**
     * Additional functions
     */
    public String toString(){
        short nextFree = this.getFirstFree();
        int maxLength = 0;
        ArrayList<StringBuilder> rows = new ArrayList<>();

        short sum = 0;

        int count = 0;
        while (nextFree != -1 && count < 5){
            while(sum < nextFree){ //скорее всего ломается, если между занятым и следующим блоком есть небольшой зазор
                this.setCursor(sum);
                short length = this.readShort();
                StringBuilder builder = this.resolveMetaInfo(sum);
                if(builder.length() > maxLength) maxLength = builder.length();
                rows.add(builder);
                sum += length;
            }

            StringBuilder builder = new StringBuilder();
            this.setCursor(nextFree);
            nextFree = this.readShort();
            short currentFreeLength = this.readShort();
            sum += currentFreeLength;
            builder.append("│ Free: Next ").append(nextFree);
            builder.append(" Size ").append(currentFreeLength);
            rows.add(builder);
            if(builder.length() > maxLength) maxLength = builder.length();
            count++;
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

