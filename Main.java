import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;

// ============ EXCEPCIONES PERSONALIZADAS ============
class SaldoInsuficienteException extends Exception {
    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}

class CuentaBloqueadaException extends Exception {
    public CuentaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}

class LimiteTransaccionException extends Exception {
    public LimiteTransaccionException(String mensaje) {
        super(mensaje);
    }
}

// ============ INTERFACES ============
interface Transaccionable {
    void depositar(double monto) throws CuentaBloqueadaException;
    void retirar(double monto) throws SaldoInsuficienteException, CuentaBloqueadaException;
    double consultarSaldo();
}

interface Auditable {
    List<Transaccion> obtenerHistorial();
    void generarReporte();
}

// ============ ENUMERACIONES ============
enum TipoTransaccion {
    DEPOSITO, RETIRO, TRANSFERENCIA, INTERES, COMISION
}

enum EstadoCuenta {
    ACTIVA, BLOQUEADA, SUSPENDIDA, CERRADA
}

enum TipoCliente {
    REGULAR, PREMIUM, VIP
}

// ============ CLASE TRANSACCION ============
class Transaccion implements Serializable {
    private static int contadorId = 1;
    private int id;
    private TipoTransaccion tipo;
    private double monto;
    private LocalDateTime fecha;
    private String descripcion;
    private double saldoResultante;
    
    public Transaccion(TipoTransaccion tipo, double monto, String descripcion, double saldoResultante) {
        this.id = contadorId++;
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = LocalDateTime.now();
        this.descripcion = descripcion;
        this.saldoResultante = saldoResultante;
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return String.format("[%s] %s: %.2f€ - %s | Saldo: %.2f€", 
            fecha.format(formatter), tipo, monto, descripcion, saldoResultante);
    }
    
    public TipoTransaccion getTipo() { return tipo; }
    public double getMonto() { return monto; }
    public LocalDateTime getFecha() { return fecha; }
}

// ============ CLASE ABSTRACTA CUENTA ============
abstract class Cuenta implements Transaccionable, Auditable, Serializable {
    protected String numeroCuenta;
    protected Cliente titular;
    protected double saldo;
    protected EstadoCuenta estado;
    protected List<Transaccion> historial;
    protected double limiteRetiroDiario;
    protected double retirosHoy;
    protected LocalDateTime ultimoRetiro;
    
    public Cuenta(String numeroCuenta, Cliente titular, double saldoInicial) {
        this.numeroCuenta = numeroCuenta;
        this.titular = titular;
        this.saldo = saldoInicial;
        this.estado = EstadoCuenta.ACTIVA;
        this.historial = new ArrayList<>();
        this.limiteRetiroDiario = 1000.0;
        this.retirosHoy = 0;
        this.ultimoRetiro = LocalDateTime.now().minusDays(1);
    }
    
    @Override
    public void depositar(double monto) throws CuentaBloqueadaException {
        verificarEstadoCuenta();
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        saldo += monto;
        registrarTransaccion(TipoTransaccion.DEPOSITO, monto, "Depósito en cuenta");
    }
    
    @Override
    public void retirar(double monto) throws SaldoInsuficienteException, CuentaBloqueadaException {
        verificarEstadoCuenta();
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        
        resetearLimiteDiario();
        
        if (retirosHoy + monto > limiteRetiroDiario) {
            throw new SaldoInsuficienteException("Excede el límite de retiro diario: " + limiteRetiroDiario + "€");
        }
        
        if (saldo < monto) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Disponible: " + saldo + "€");
        }
        
        saldo -= monto;
        retirosHoy += monto;
        ultimoRetiro = LocalDateTime.now();
        registrarTransaccion(TipoTransaccion.RETIRO, monto, "Retiro de efectivo");
    }
    
    @Override
    public double consultarSaldo() {
        return saldo;
    }
    
    @Override
    public List<Transaccion> obtenerHistorial() {
        return new ArrayList<>(historial);
    }
    
    @Override
    public void generarReporte() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              REPORTE DE CUENTA BANCARIA                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("Número de Cuenta: " + numeroCuenta);
        System.out.println("Titular: " + titular.getNombre());
        System.out.println("Tipo: " + this.getClass().getSimpleName());
        System.out.println("Estado: " + estado);
        System.out.println("Saldo Actual: " + String.format("%.2f€", saldo));
        System.out.println("\n--- ÚLTIMAS 10 TRANSACCIONES ---");
        
        int limite = Math.min(10, historial.size());
        for (int i = historial.size() - 1; i >= historial.size() - limite; i--) {
            System.out.println(historial.get(i));
        }
        
        calcularEstadisticas();
    }
    
    protected void registrarTransaccion(TipoTransaccion tipo, double monto, String descripcion) {
        Transaccion t = new Transaccion(tipo, monto, descripcion, saldo);
        historial.add(t);
    }
    
    protected void verificarEstadoCuenta() throws CuentaBloqueadaException {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new CuentaBloqueadaException("La cuenta está " + estado);
        }
    }
    
    private void resetearLimiteDiario() {
        if (ultimoRetiro.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            retirosHoy = 0;
        }
    }
    
    private void calcularEstadisticas() {
        double totalDepositos = 0;
        double totalRetiros = 0;
        int numDepositos = 0;
        int numRetiros = 0;
        
        for (Transaccion t : historial) {
            if (t.getTipo() == TipoTransaccion.DEPOSITO) {
                totalDepositos += t.getMonto();
                numDepositos++;
            } else if (t.getTipo() == TipoTransaccion.RETIRO) {
                totalRetiros += t.getMonto();
                numRetiros++;
            }
        }
        
        System.out.println("\n--- ESTADÍSTICAS ---");
        System.out.println("Total depositado: " + String.format("%.2f€", totalDepositos));
        System.out.println("Total retirado: " + String.format("%.2f€", totalRetiros));
        System.out.println("Número de depósitos: " + numDepositos);
        System.out.println("Número de retiros: " + numRetiros);
    }
    
    public abstract void aplicarComision();
    public abstract void calcularIntereses();
    
    public String getNumeroCuenta() { return numeroCuenta; }
    public EstadoCuenta getEstado() { return estado; }
    public void setEstado(EstadoCuenta estado) { this.estado = estado; }
    public Cliente getTitular() { return titular; }
}

// ============ TIPOS DE CUENTAS ============
class CuentaAhorro extends Cuenta {
    private double tasaInteres;
    
    public CuentaAhorro(String numeroCuenta, Cliente titular, double saldoInicial) {
        super(numeroCuenta, titular, saldoInicial);
        this.tasaInteres = 0.03; // 3% anual
        this.limiteRetiroDiario = 500.0;
    }
    
    @Override
    public void aplicarComision() {
        if (saldo < 100) {
            double comision = 5.0;
            saldo -= comision;
            registrarTransaccion(TipoTransaccion.COMISION, comision, "Comisión por saldo bajo");
            System.out.println("⚠ Comisión aplicada: " + comision + "€");
        }
    }
    
    @Override
    public void calcularIntereses() {
        double interes = saldo * (tasaInteres / 12);
        saldo += interes;
        registrarTransaccion(TipoTransaccion.INTERES, interes, "Intereses mensuales");
        System.out.println("✓ Intereses aplicados: " + String.format("%.2f€", interes));
    }
}

class CuentaCorriente extends Cuenta {
    private double sobregiro;
    private double limiteSobregiro;
    
    public CuentaCorriente(String numeroCuenta, Cliente titular, double saldoInicial) {
        super(numeroCuenta, titular, saldoInicial);
        this.sobregiro = 0;
        this.limiteSobregiro = 500.0;
        this.limiteRetiroDiario = 2000.0;
    }
    
    @Override
    public void retirar(double monto) throws SaldoInsuficienteException, CuentaBloqueadaException {
        verificarEstadoCuenta();
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        
        resetearLimiteDiario();
        
        if (retirosHoy + monto > limiteRetiroDiario) {
            throw new SaldoInsuficienteException("Excede el límite de retiro diario");
        }
        
        double disponible = saldo + limiteSobregiro - sobregiro;
        if (monto > disponible) {
            throw new SaldoInsuficienteException("Fondos insuficientes. Disponible: " + disponible + "€");
        }
        
        if (monto <= saldo) {
            saldo -= monto;
        } else {
            double restante = monto - saldo;
            saldo = 0;
            sobregiro += restante;
        }
        
        retirosHoy += monto;
        ultimoRetiro = LocalDateTime.now();
        registrarTransaccion(TipoTransaccion.RETIRO, monto, "Retiro de efectivo");
    }
    
    @Override
    public void aplicarComision() {
        double comision = 10.0;
        saldo -= comision;
        registrarTransaccion(TipoTransaccion.COMISION, comision, "Comisión mensual de mantenimiento");
        
        if (sobregiro > 0) {
            double comisionSobregiro = sobregiro * 0.05;
            saldo -= comisionSobregiro;
            registrarTransaccion(TipoTransaccion.COMISION, comisionSobregiro, "Comisión por sobregiro");
            System.out.println("⚠ Comisión por sobregiro: " + String.format("%.2f€", comisionSobregiro));
        }
    }
    
    @Override
    public void calcularIntereses() {
        // Las cuentas corrientes no generan intereses
    }
    
    private void resetearLimiteDiario() {
        if (ultimoRetiro.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            retirosHoy = 0;
        }
    }
}

class CuentaInversion extends Cuenta {
    private double tasaInteres;
    private int mesesBloqueado;
    
    public CuentaInversion(String numeroCuenta, Cliente titular, double saldoInicial) {
        super(numeroCuenta, titular, saldoInicial);
        this.tasaInteres = 0.06; // 6% anual
        this.mesesBloqueado = 12;
        this.limiteRetiroDiario = 0; // No permite retiros
    }
    
    @Override
    public void retirar(double monto) throws SaldoInsuficienteException {
        throw new SaldoInsuficienteException("Cuenta de inversión bloqueada por " + mesesBloqueado + " meses");
    }
    
    @Override
    public void aplicarComision() {
        // Sin comisiones
    }
    
    @Override
    public void calcularIntereses() {
        double interes = saldo * (tasaInteres / 12);
        saldo += interes;
        registrarTransaccion(TipoTransaccion.INTERES, interes, "Intereses de inversión");
        System.out.println("✓ Intereses de inversión: " + String.format("%.2f€", interes));
    }
}

// ============ CLASE CLIENTE ============
class Cliente implements Serializable {
    private String id;
    private String nombre;
    private String email;
    private TipoCliente tipo;
    private List<Cuenta> cuentas;
    
    public Cliente(String id, String nombre, String email, TipoCliente tipo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.tipo = tipo;
        this.cuentas = new ArrayList<>();
    }
    
    public void agregarCuenta(Cuenta cuenta) {
        cuentas.add(cuenta);
    }
    
    public List<Cuenta> getCuentas() {
        return new ArrayList<>(cuentas);
    }
    
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public TipoCliente getTipo() { return tipo; }
}

// ============ CLASE BANCO (SINGLETON) ============
class Banco {
    private static Banco instancia;
    private Map<String, Cliente> clientes;
    private Map<String, Cuenta> cuentas;
    private int contadorCuentas;
    
    private Banco() {
        clientes = new HashMap<>();
        cuentas = new HashMap<>();
        contadorCuentas = 1000;
        cargarDatosIniciales();
    }
    
    public static Banco obtenerInstancia() {
        if (instancia == null) {
            instancia = new Banco();
        }
        return instancia;
    }
    
    private void cargarDatosIniciales() {
        Cliente c1 = new Cliente("DNI001", "Juan Pérez", "juan@email.com", TipoCliente.PREMIUM);
        Cliente c2 = new Cliente("DNI002", "María García", "maria@email.com", TipoCliente.REGULAR);
        
        clientes.put(c1.getId(), c1);
        clientes.put(c2.getId(), c2);
        
        crearCuenta(c1, "AHORRO", 1000);
        crearCuenta(c2, "CORRIENTE", 500);
    }
    
    public String crearCuenta(Cliente cliente, String tipo, double saldoInicial) {
        String numeroCuenta = "ES" + String.format("%08d", contadorCuentas++);
        Cuenta cuenta = null;
        
        switch (tipo.toUpperCase()) {
            case "AHORRO":
                cuenta = new CuentaAhorro(numeroCuenta, cliente, saldoInicial);
                break;
            case "CORRIENTE":
                cuenta = new CuentaCorriente(numeroCuenta, cliente, saldoInicial);
                break;
            case "INVERSION":
                cuenta = new CuentaInversion(numeroCuenta, cliente, saldoInicial);
                break;
            default:
                throw new IllegalArgumentException("Tipo de cuenta inválido");
        }
        
        cuentas.put(numeroCuenta, cuenta);
        cliente.agregarCuenta(cuenta);
        return numeroCuenta;
    }
    
    public void transferir(String cuentaOrigen, String cuentaDestino, double monto) 
            throws SaldoInsuficienteException, CuentaBloqueadaException {
        Cuenta origen = cuentas.get(cuentaOrigen);
        Cuenta destino = cuentas.get(cuentaDestino);
        
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Cuenta no encontrada");
        }
        
        origen.retirar(monto);
        destino.depositar(monto);
        
        origen.registrarTransaccion(TipoTransaccion.TRANSFERENCIA, monto, 
            "Transferencia a " + cuentaDestino);
        destino.registrarTransaccion(TipoTransaccion.TRANSFERENCIA, monto, 
            "Transferencia desde " + cuentaOrigen);
        
        System.out.println("✓ Transferencia exitosa: " + String.format("%.2f€", monto));
    }
    
    public void procesarComisionesMensuales() {
        System.out.println("\n--- PROCESANDO COMISIONES MENSUALES ---");
        for (Cuenta cuenta : cuentas.values()) {
            if (cuenta.getEstado() == EstadoCuenta.ACTIVA) {
                cuenta.aplicarComision();
                cuenta.calcularIntereses();
            }
        }
    }
    
    public Cliente obtenerCliente(String id) {
        return clientes.get(id);
    }
    
    public Cuenta obtenerCuenta(String numero) {
        return cuentas.get(numero);
    }
    
    public Collection<Cliente> obtenerTodosLosClientes() {
        return clientes.values();
    }
    
    public Collection<Cuenta> obtenerTodasLasCuentas() {
        return cuentas.values();
    }
    
    public Cliente registrarCliente(String id, String nombre, String email, TipoCliente tipo) {
        Cliente cliente = new Cliente(id, nombre, email, tipo);
        clientes.put(id, cliente);
        return cliente;
    }
}

// ============ SISTEMA PRINCIPAL ============
public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static Banco banco = Banco.obtenerInstancia();
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║     SISTEMA BANCARIO AVANZADO - BIENVENIDO           ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        
        mostrarMenuPrincipal();
    }
    
    private static void mostrarMenuPrincipal() {
        while (true) {
            System.out.println("\n┌────────────────────────────────────────┐");
            System.out.println("│          MENÚ PRINCIPAL                │");
            System.out.println("└────────────────────────────────────────┘");
            System.out.println("1. Gestión de Clientes");
            System.out.println("2. Gestión de Cuentas");
            System.out.println("3. Operaciones Bancarias");
            System.out.println("4. Reportes y Estadísticas");
            System.out.println("5. Procesos Automáticos");
            System.out.println("6. Salir");
            System.out.print("\nSeleccione opción: ");
            
            int opcion = leerEntero();
            
            switch (opcion) {
                case 1: menuClientes(); break;
                case 2: menuCuentas(); break;
                case 3: menuOperaciones(); break;
                case 4: menuReportes(); break;
                case 5: menuProcesos(); break;
                case 6:
                    System.out.println("\n¡Gracias por usar nuestro sistema!");
                    return;
                default:
                    System.out.println("❌ Opción inválida");
            }
        }
    }
    
    private static void menuClientes() {
        System.out.println("\n=== GESTIÓN DE CLIENTES ===");
        System.out.println("1. Registrar nuevo cliente");
        System.out.println("2. Ver todos los clientes");
        System.out.println("3. Buscar cliente");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero();
        
        switch (opcion) {
            case 1: registrarCliente(); break;
            case 2: verClientes(); break;
            case 3: buscarCliente(); break;
        }
    }
    
    private static void registrarCliente() {
        System.out.print("ID/DNI: ");
        String id = scanner.nextLine();
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.println("Tipo (1=REGULAR, 2=PREMIUM, 3=VIP): ");
        int tipo = leerEntero();
        
        TipoCliente tipoCliente = TipoCliente.REGULAR;
        if (tipo == 2) tipoCliente = TipoCliente.PREMIUM;
        if (tipo == 3) tipoCliente = TipoCliente.VIP;
        
        banco.registrarCliente(id, nombre, email, tipoCliente);
        System.out.println("✓ Cliente registrado exitosamente");
    }
    
    private static void verClientes() {
        System.out.println("\n=== LISTADO DE CLIENTES ===");
        for (Cliente c : banco.obtenerTodosLosClientes()) {
            System.out.println("• " + c.getNombre() + " [" + c.getId() + "] - " + c.getTipo());
            System.out.println("  Cuentas: " + c.getCuentas().size());
        }
    }
    
    private static void buscarCliente() {
        System.out.print("Ingrese ID del cliente: ");
        String id = scanner.nextLine();
        Cliente cliente = banco.obtenerCliente(id);
        
        if (cliente != null) {
            System.out.println("\n✓ Cliente encontrado:");
            System.out.println("Nombre: " + cliente.getNombre());
            System.out.println("Email: " + cliente.getEmail());
            System.out.println("Tipo: " + cliente.getTipo());
            System.out.println("Cuentas:");
            for (Cuenta c : cliente.getCuentas()) {
                System.out.println("  • " + c.getNumeroCuenta() + " - Saldo: " + c.consultarSaldo() + "€");
            }
        } else {
            System.out.println("Cliente no encontrado");
        }
    }
    
    private static void menuCuentas() {
        System.out.println("\n=== GESTIÓN DE CUENTAS ===");
        System.out.println("1. Crear nueva cuenta");
        System.out.println("2. Ver todas las cuentas");
        System.out.println("3. Consultar cuenta específica");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero();
        
        switch (opcion) {
            case 1: crearCuenta(); break;
            case 2: verCuentas(); break;
            case 3: consultarCuenta(); break;
        }
    }
    
    private static void crearCuenta() {
        System.out.print("ID del cliente: ");
        String idCliente = scanner.nextLine();
        Cliente cliente = banco.obtenerCliente(idCliente);
        
        if (cliente == null) {
            System.out.println("Cliente no encontrado");
            return;
        }
        
        System.out.println("Tipo de cuenta:");
        System.out.println("1. Ahorro (3% interés, límite 500€/día)");
        System.out.println("2. Corriente (sobregiro 500€, límite 2000€/día)");
        System.out.println("3. Inversión (6% interés, sin retiros)");
        System.out.print("Opción: ");
        int tipo = leerEntero();
        
        String tipoCuenta = "";
        switch (tipo) {
            case 1: tipoCuenta = "AHORRO"; break;
            case 2: tipoCuenta = "CORRIENTE"; break;
            case 3: tipoCuenta = "INVERSION"; break;
            default:
                System.out.println("Tipo inválido");
                return;
        }
        
        System.out.print("Saldo inicial: ");
        double saldo = leerDouble();
        
        String numero = banco.crearCuenta(cliente, tipoCuenta, saldo);
        System.out.println("✓ Cuenta creada: " + numero);
    }
    
    private static void verCuentas() {
        System.out.println("\n=== LISTADO DE CUENTAS ===");
        for (Cuenta c : banco.obtenerTodasLasCuentas()) {
            System.out.println("• " + c.getNumeroCuenta() + " - " + c.getClass().getSimpleName());
            System.out.println("  Titular: " + c.getTitular().getNombre());
            System.out.println("  Saldo: " + String.format("%.2f€", c.consultarSaldo()));
            System.out.println("  Estado: " + c.getEstado());
        }
    }
    
    private static void consultarCuenta() {
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine();
        Cuenta cuenta = banco.obtenerCuenta(numero);
        
        if (cuenta != null) {
            cuenta.generarReporte();
        } else {
            System.out.println("Cuenta no encontrada");
        }
    }
    
    private static void menuOperaciones() {
        System.out.println("\n=== OPERACIONES BANCARIAS ===");
        System.out.println("1. Depositar");
        System.out.println("2. Retirar");
        System.out.println("3. Transferir");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero();
        
        try {
            switch (opcion) {
                case 1: realizarDeposito(); break;
                case 2: realizarRetiro(); break;
                case 3: realizarTransferencia(); break;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void realizarDeposito() throws CuentaBloqueadaException {
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine();
        Cuenta cuenta = banco.obtenerCuenta(numero);
        
        if (cuenta == null) {
            System.out.println("Cuenta no encontrada");
            return;
        }
        
        System.out.print("Monto a depositar: ");
        double monto = leerDouble();
        
        cuenta.depositar(monto);
        System.out.println("✓ Depósito exitoso. Nuevo saldo: " + cuenta.consultarSaldo() + "€");
    }
    
    private static void realizarRetiro() throws SaldoInsuficienteException, CuentaBloqueadaException {
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine();
        Cuenta cuenta = banco.obtenerCuenta(numero);
        
        if (cuenta == null) {
            System.out.println("Cuenta no encontrada");
            return;
        }
        
        System.out.print("Monto a retirar: ");
        double monto = leerDouble();
        
        cuenta.retirar(monto);
        System.out.println("✓ Retiro exitoso. Nuevo saldo: " + cuenta.consultarSaldo() + "€");
    }
    
    private static void realizarTransferencia() throws SaldoInsuficienteException, CuentaBloqueadaException {
        System.out.print("Cuenta origen: ");
        String origen = scanner.nextLine();
        System.out.print("Cuenta destino: ");
        String destino = scanner.nextLine();
        System.out.print("Monto: ");
        double monto = leerDouble();
        
        banco.transferir(origen, destino, monto);
    }
    
    private static void menuReportes() {
        System.out.println("\n=== REPORTES Y ESTADÍSTICAS ===");
        System.out.println("1. Reporte de cuenta");
        System.out.println("2. Historial de transacciones");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero();
        
        if (opcion == 1 || opcion == 2) {
            System.out.print("Número de cuenta: ");
            String numero = scanner.nextLine();
            Cuenta cuenta = banco.obtenerCuenta(numero);
            
            if (cuenta != null) {
                cuenta.generarReporte();
            } else {
                System.out.println("Cuenta no encontrada");
            }
        }
    }
    
    private static void menuProcesos() {
        System.out.println("\n=== PROCESOS AUTOMÁTICOS ===");
        System.out.println("1. Aplicar comisiones e intereses mensuales");
        System.out.println("2. Bloquear/Desbloquear cuenta");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero();
        
        switch (opcion) {
            case 1:
                banco.procesarComisionesMensuales();
                System.out.println("✓ Proceso completado");
                break;
            case 2:
                System.out.print("Número de cuenta: ");
                String numero = scanner.nextLine();
                Cuenta cuenta = banco.obtenerCuenta(numero);
                
                if (cuenta != null) {
                    System.out.println("Estado actual: " + cuenta.getEstado());
                    System.out.println("1. ACTIVA");
                    System.out.println("2. BLOQUEADA");
                    System.out.println("3. SUSPENDIDA");
                    System.out.print("Nuevo estado: ");
                    int estado = leerEntero();
                    
                    EstadoCuenta nuevoEstado = EstadoCuenta.ACTIVA;
                    if (estado == 2) nuevoEstado = EstadoCuenta.BLOQUEADA;
                    if (estado == 3) nuevoEstado = EstadoCuenta.SUSPENDIDA;
                    
                    cuenta.setEstado(nuevoEstado);
                    System.out.println("Estado actualizado a: " + nuevoEstado);
                } else {
                    System.out.println(" Cuenta no encontrada");
                }
                break;
        }
    }
    
    private static int leerEntero() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private static double leerDouble() {
        try {
            return Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}