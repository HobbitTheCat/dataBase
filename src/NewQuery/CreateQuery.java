package NewQuery;

public class CreateQuery extends Query implements Update{
    @Override
    public Update column(String... columnNames) {
        return null;
    }

    @Override
    public Update values(String... values) {
        return null;
    }

    @Override
    public Update object(Object object) {
        return null;
    }
}
