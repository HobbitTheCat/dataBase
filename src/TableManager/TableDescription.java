package TableManager;

import java.util.Map;

public class TableDescription {
    private final int attributeNumber;
    private int objectPage;
    private final String tableName;
    private final String[] attributesNames; //maybe Map<String, Integer> for attributeNames <-> attributePages has sens
//    private final Map<String, String> attributesTypes;
    private final String[] attributesTypes;
    private final int[] attributesPages;

    public String getName() {return this.tableName;}
    public int getAttributeNumber() {return this.attributeNumber;}
    public String[] getAttributesNames() {return this.attributesNames;}
    public String getAttributeName(int index) {return this.attributesNames[index];}
    public int[] getAttributesPages() {return this.attributesPages;}
    public String[] getAttributesTypes() {return this.attributesTypes;}
    public String getAttributeType(int index) {return this.attributesTypes[index];}
    public int getObjectPage() {return this.objectPage;}

    public void setObjectPage(int objectPage) {this.objectPage = objectPage;}
    public void setAttributesPage(int attributeNumber,  int attributePage) {this.attributesPages[attributeNumber] = attributePage;}

    public int getAttributePageByName(String attributeName) {
        for(int i = 0; i < this.attributesNames.length; i++)
            if(this.attributesNames[i].equals(attributeName)) return this.attributesPages[i];
        return -1;
    }
    public int getAttributeInternalIndexByName(String attributeName) {
        for(int i = 0; i < this.attributesNames.length; i++)
            if(this.attributesNames[i].equals(attributeName)) return i;
        return -1;
    }

    public TableDescription(int tablePage, String tableName, String[] attributesNames, int[] attributesPages) {
        this.objectPage = tablePage;
        if(attributesNames.length != attributesPages.length) throw new IllegalArgumentException("Attributes names length is not equal to attributes pages length");
        this.tableName = tableName;
        this.attributesNames = attributesNames;
        this.attributesPages = attributesPages;
        this.attributeNumber = attributesNames.length;
        this.attributesTypes = new String[this.attributeNumber];
    }
    public TableDescription(String tableName, String[] attributesNames, Map<String, String> attributesTypes) {
        this.objectPage = -1;
        this.tableName = tableName;

        this.attributesNames = attributesNames;
        this.attributesTypes = new String[this.attributesNames.length];
        for(int i = 0; i < this.attributesNames.length; i++)
            this.attributesTypes[i] = attributesTypes.get(this.attributesNames[i]);
        this.attributeNumber = attributesNames.length;
        this.attributesPages = new int[this.attributeNumber];
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("Table Name: " + this.tableName + " Object page:" + this.objectPage + "\nAttributes names: ");
        for(int i = 0; i < this.attributesNames.length; i++){
            sb.append("\n").append(this.attributesNames[i]).append(" start at page ").append(this.attributesPages[i]);
        }
        return sb.toString();
    }
}