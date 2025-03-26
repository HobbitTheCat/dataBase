package NewQuery;

public interface Update{
    Update column(String... columnNames);
    Update values(Object... values); //need to check if length of column == length of values
    Update object(Object object); //Reflexion
}
