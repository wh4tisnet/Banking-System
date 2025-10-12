# 🏦 Banking System in Java

The **Banking System in Java** is a console-based application that allows users to manage bank accounts, perform financial operations, and maintain a detailed transaction history.
It showcases strong **OOP design**, with the use of **interfaces**, **enums**, **custom exceptions**, and **object serialization** to build a modular and maintainable system.

---

## ⚙️ Key Features

✅ **Account Management**

* Create, activate, block, suspend, or close accounts.
* Associate accounts with different client types: Regular, Premium, or VIP.

💰 **Banking Operations**

* Deposit, withdraw, and transfer funds between accounts.
* Automatic calculation of interests and commissions.
* Enforced transaction limits and balance validation.

📜 **Transaction History & Auditing**

* Complete transaction log with timestamps, types, and resulting balances.
* Report generation directly from the console.
* Data persistence through object serialization.

⚠️ **Custom Exception Handling**

* `SaldoInsuficienteException` – prevents overdrafts.
* `CuentaBloqueadaException` – restricts operations on blocked accounts.
* `LimiteTransaccionException` – enforces maximum transaction limits.

---

## 🧩 Project Structure

```
src/
├── Main.java                # Entry point of the program
├── Transaccion.java         # Transaction model
├── Cuenta.java              # Account logic (deposit, withdraw, etc.)
├── Cliente.java             # Client data and account type
├── interfaces/
│   ├── Transaccionable.java # Basic banking operations interface
│   └── Auditable.java       # Reporting and transaction history
└── excepciones/
    ├── SaldoInsuficienteException.java
    ├── CuentaBloqueadaException.java
    └── LimiteTransaccionException.java
```

---

## 🛠️ Technologies Used

* **Java 11+**
* **Collections API (`List`, `Map`)**
* **`LocalDateTime` & `DateTimeFormatter`** for time management
* **Serialization (`Serializable`)**
* **Object-Oriented Programming concepts:**

  * Interfaces
  * Enums
  * Inheritance
  * Custom Exceptions

---

## 🚀 Installation & Execution

### 1️⃣ Clone the repository

```bash
git clone https://github.com/username/banking-system-java.git
cd banking-system-java
```

### 2️⃣ Compile the program

```bash
javac Main.java
```

### 3️⃣ Run the program

```bash
java Main
```

---

## 💡 Example Usage

```
=== Bienvenido al Sistema Bancario ===
1. Crear cliente y cuenta
2. Depositar dinero
3. Retirar dinero
4. Transferir fondos
5. Consultar saldo
6. Ver historial de transacciones
7. Salir

Seleccione una opción: 1
>> Cuenta creada exitosamente para el cliente 'Juan Pérez'.
```

> Although the program runs in Spanish, its logic and structure follow best practices in software engineering and object-oriented design.

---

## 🧠 Concepts Demonstrated

* **Encapsulation & Inheritance**
* **Polymorphism via Interfaces**
* **Abstraction & Modularity**
* **Robust Exception Handling**
* **Data Persistence (Serialization)**
* **Date & Time Handling**

---

## 🧾 License

This project is open for **educational and personal use**.
You may freely modify or extend it to explore Java OOP principles or build more complex financial systems.

---

Would you like me to **generate this as a downloadable `README.md` file** (ready to upload to GitHub)?
