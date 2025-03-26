package NewQuery;

public class UsageExample {
    public static void main(String[] args) {
        Session session = new Session();
        Transaction transaction = session.createNewTransaction();
        transaction.add(Query.select(Hero.class).where("name", "==", "Rusty-Man").where("age", ">", "35"));
        Result[] results = session.execute(transaction);
    }
}

class Hero{
    String name;
    String secretName;
    Integer age;

    public Hero(String name, String secretName, int age) {
        this.name = name;
        this.secretName = secretName;
        this.age = age;
    }
}
