# Object-Oriented Database in Java

## 📌 About the Project
This project implements a custom **object-oriented database management system (DBMS)** in Java.  
It was developed as a university project and demonstrates knowledge of **low-level data storage**, **multithreading**, **page caching**, and **client-server architecture**.

Key features:
- Page-based storage (4KB fixed-size pages).
- Object-oriented data model with support for **CRUD operations**.
- Multithreaded transaction server with deadlock prevention.
- Client-server architecture with a custom DSL (inspired by SQL and ORM frameworks).
- Efficient caching and concurrency mechanisms.

For a detailed technical description, see the [full report](https://github.com/HobbitTheCat/dataBase/blob/master/Rapport_Projet_SemenovEgor_MathieuLemain.pdf).

---

## 🏗 Architecture

### Storage
- Data is organized into **fixed-size pages (4KB)**.
- Specialized page types:
  - **MetaPage** — stores class definitions.
  - **ObjectPage** — stores object references.
  - **StringPage / LongPage / BooleanPage** — store attribute values with backlinks.
  - **HeaderPage** — database metadata and free page list.
  - **FreePage** — available space tracking.

### Memory Management
- **LFU (Least Frequently Used) cache** for recently accessed pages.
- **Dirty page tracking** for efficient write-back.
- Pages are synchronized using locks.

### Concurrency
- Deadlock prevention using:
  - Resource ordering,
  - Timeout & retry,
  - Wait-for graph cycle detection.

### System Layers
1. **ResourceManager** — caching and resource allocation.
2. **PageManager** — low-level page handling.
3. **ObjectManager** — logical object reconstruction.
4. **TableManager** — high-level CRUD operations.

### Client-Server
- **Server:** multithreaded transaction processor with thread pools and asynchronous result delivery (`CompletableFuture`).
- **Client:** query DSL allowing intuitive CRUD operations.

---

## 🚀 Installation & Running

### Requirements
- Java 17+
- Maven or Gradle
- Git

### Build
```bash
git clone https://github.com/HobbitTheCat/dataBase.git
cd dataBase
mvn package
```
### Run the server
```bash
java -jar target/database-server.jar
```
### Run the client
```bash
java -jar target/database-client.jar
```
---

## 📖 Usage Example
```java
import NewQuery.Query;
import NewQuery.Result;
import NewQuery.Session;
import NewQuery.Transaction;

public class UsageExample {
  public static void main(String[] args) {
    Hero hero = new Hero("Peter Parker", "Spider-Man", 19);
    try(Session session = new Session("localhost", 8080)){
      Transaction transaction = new Transaction();
    
      transaction.add(Query.select(Hero.class));
      transaction.add(Query.select(Hero.class).where("name", "==", "tony").where("age", ">", 35).all());
      transaction.add(Query.create(Hero.class).object(hero));
    
      List<Result> results = session.execute(transaction);
      System.out.println(results);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class Hero {
  String name;
  String secretName;
  Integer age;

  public Hero(String name, String secretName, int age) {
    this.name = name;
    this.secretName = secretName;
    this.age = age;
  }
}
```
---

## ⚠️ Limitations
- Attribute UPDATE is only partially implemented.
- Command-line client (CLI) is in an early draft.
- User authentication is not yet integrated.

---

## 📈 Future Improvement
- Support for new data types (e.g., `TimePage` for timestamps).
- Advanced string operators (`startsWith`, `endsWith`, `fuzzy match`).
- Nested objects and relationships between classes.
- Full ACID transaction guarantees.
- Rewrite DSL in Kotlin for greater convenience

---

## 👥 Authors
- Egor Semenov-Tyan-Shanskiy — Backend, Frontend, Documentation
- Mathieu Lemain — Interpreter, Frontend, Documentation

## 📂 [GitHub](https://github.com/HobbitTheCat/dataBase/tree/master)

