package Interface;

public interface DataPage<Type>{
    Type get(short index);
    short size();
    void delete(short index);
    String toString();
}
