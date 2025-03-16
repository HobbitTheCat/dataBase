package Pages;

public class DataClass {
    private final int objectPage;
    private final String className;
    private final String[] attributesNames;
    private final int[] attributesPages;

    public String getName() {return this.className;}
    public String[] getAttributesNames() {return this.attributesNames;}
    public int[] getAttributesPages() {return this.attributesPages;}
    public int getObjectPage() {return this.objectPage;}

    public int getAttributePageByName(String attributeName) {
        for(int i = 0; i < this.attributesNames.length; i++){
            if(this.attributesNames[i].equals(attributeName)){
                return this.attributesPages[i];
            }
        }
        return -1;
    }

    public DataClass(int classPage, String className, String[] attributesNames, int[] attributesPages) {
        this.objectPage = classPage;
        if(attributesNames.length != attributesPages.length) throw new IllegalArgumentException("Attributes names length is not equal to attributes pages length");
        this.className = className;
        this.attributesNames = attributesNames;
        this.attributesPages = attributesPages;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("Class Name: " + this.className + "\nAttributes names: ");
        for(int i = 0; i < this.attributesNames.length; i++){
            sb.append("\n").append(this.attributesNames[i]).append(" start at page ").append(this.attributesPages[i]);
        }
        return sb.toString();
    }
}