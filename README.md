# GrupoTP-37 - Fast-Food Restaurant Chain Management System

Java command-line application developed for the DSS course, focused on managing a fast-food restaurant chain. The system follows a layered architecture (`UI / Business Logic / Data`) and uses MySQL persistence through DAO classes.

## Main features

- Customer self-service kiosk for creating orders.
- Menu browsing with menus, burgers, sides, drinks and desserts.
- Order customization with ingredient removal/extras, allergy filtering and notes.
- Payment flow with money/card/MB Way simulation and a loyalty points system.
- Kitchen/staff workflow for viewing active orders, updating order states and delaying orders when stock is missing.
- Stock consultation and replenishment per restaurant.
- COO and manager dashboards for staff management, HR requests, restaurant communication and performance reports.
- Reports with restaurant sales and average waiting time indicators.

## Architecture

```text
src/main/java/project
├── ui        # Text-based user interface
├── business  # Facade and domain logic: Operations, RH and Communication
└── data      # MySQL persistence using DAO classes
```

The application entry point is `project.Main`, which starts the text UI.

## Technologies

- Java 21
- Maven
- MySQL
- JDBC / MySQL Connector/J
- Python seed script with `pymysql`

## Database setup

The database connection is configured in `project.data.ConexaoDB`:

```text
Database: CadeiaRestaurantesDB
Host: localhost
Port: 3307
User: root
Password: root
```

To create and populate the database, run:

```bash
pip install pymysql
python src/main/java/project/seed.py
```

## Run

```bash
mvn package
java -jar target/GrupoTP-37-1.0-SNAPSHOT.jar
```

Alternative Maven run:

```bash
mvn exec:java
```

## Example seeded credentials

- COO: `1 / admin123`
- Managers: `10-15 / GERENTE<ID>`
- Workers: `200-239 / TRABALHADOR<ID>`

## Course context

This project was developed for Desenvolvimento de Sistemas de Software at Universidade do Minho. It models the operation of a fast-food restaurant chain, from customer ordering to kitchen execution, stock management, staff coordination and management reporting.
