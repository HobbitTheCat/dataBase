package Pages.Interface;

public interface DataPage<Type>{
    Type get(short index);
    short size();
    void delete(short index);
    short getType();
    String toString();
}
