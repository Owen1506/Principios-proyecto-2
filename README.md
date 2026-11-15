# Mini PC — Simulador de Sistemas Operativos

Simulador de una computadora y de la administración de procesos, desarrollado en
Java con una interfaz gráfica Swing para el curso **IC-6600 Principios de Sistemas
Operativos**.

Permite cargar programas en ensamblador, ejecutarlos manual o automáticamente,
observar el estado de los procesos, la memoria principal, el almacenamiento y las
estadísticas de ejecución.

---

## Integrantes

- **Owen Caleb Smith Cerdas** — Carnet **2024083328**.
- **Elder Leon Perez** — Carnet **2023166120**.

---

## Estado de los requisitos

| Requisito | Estado |
|---|---|
| Ejecución de programas ASM | ✅ Logrado |
| Ejecución manual y automática | ✅ Logrado |
| Hasta 5 CPU | ✅ Logrado |
| Algoritmos de planificación de CPU (10) | ✅ Logrado |
| Tiempos de llegada y ráfaga | ✅ Logrado |
| Carga múltiple y validación de errores | ✅ Logrado |
| Finalización de procesos | ✅ Logrado |
| Estadísticas por proceso | ✅ Logrado |
| Interrupciones | ✅ Logrado |
| Reinicio coordinado | ✅ Logrado |
| Servicios de archivos | ✅ Logrado |
| Entrada y salida | ✅ Logrado |
| Almacenamiento simulado | ✅ Logrado |
| Memoria principal y kernel | ✅ Logrado |
| Memoria virtual (intercambio completo) | ✅ Logrado |
| Particiones dinámicas (First/Next/Best Fit) | ✅ Logrado |
| Particiones fijas iguales | ✅ Logrado |
| Particiones fijas diferentes | ✅ Logrado |
| Paginación simple | ✅ Logrado |
| Paginación virtual | ✅ Logrado |
| Visualización de RAM por bloques | ✅ Logrado |
| Valoración del proyecto | ✅ Logrado |
| Video de la aplicación | ⚠️ Pendiente de verificación |
| Verificación integral desde la interfaz | ⚠️ Pendiente |
| Pruebas completas de archivos y estados | ⚠️ Pendiente |

## Características principales

- **Ejecución de programas ASM:** lectura, validación sintáctica, interpretación y ejecución de instrucciones.
- **Ejecución manual y automática:** el botón **Siguiente** avanza un tick de ejecución; **Ejecutar** avanza automáticamente cada segundo real, con posibilidad de pausar.
- **Hasta 5 CPU:** configurables desde la interfaz, con hasta 5 procesos por CPU.
- **Administración de procesos:** identificación por PID, BCP, estados del proceso, planificación y almacenamiento/restauración del contexto de CPU.
- **Memoria principal y kernel:** división configurable al inicio, con visualización de atributos del BCP, uno por posición del kernel.
- **Almacenamiento simulado:** copia de los programas en disco, control de capacidad e índice de nombres y direcciones.
- **Servicios de archivos:** crear, abrir, leer, escribir y eliminar mediante `INT 21H`, con control de archivos abiertos por proceso.
- **Entrada y salida:** teclado mediante `INT 09H`, pantalla mediante `INT 10H` y finalización mediante `INT 20H`.
- **Estadísticas por proceso:** inicio y fin, tiempo simulado de CPU, duración real, contexto y resultado de la ejecución.
- **Protección y errores de ejecución:** los saltos fuera de rango y los errores de pila o servicios de archivos finalizan el proceso afectado, registran el motivo, liberan sus recursos y permiten continuar con el siguiente proceso.
- **Reinicio coordinado:** limpieza de CPU, RAM, procesos, colas, memoria virtual, entradas pendientes y errores.

---

## Algoritmos de planificación de CPU

Disponibles desde la ventana **Configuración**:

- **FCFS** — First Come, First Served
- **SPN** — Shortest Process Next
- **SRT** — Shortest Remaining Time
- **HRRN** — Highest Response Ratio Next
- **RR** — Round Robin
- **PRIORIDAD** — con o sin expropiación
- **SELFISH ROUND ROBIN (SRR)**
- **LOTTERY**
- **MULTILEVEL FEEDBACK QUEUE (MLFQ)**
- **GUARANTEED SCHEDULING**

Los algoritmos de CPU no se alteran al cambiar el método de memoria.

---

## Administración de memoria

El simulador permite seleccionar un **método de administración de memoria** independiente del algoritmo de planificación de CPU. Las opciones disponibles son:

### 1. Particiones dinámicas

Las particiones se crean según la memoria que necesita cada proceso. Al liberar, los huecos contiguos se fusionan. Permite seleccionar uno de los siguientes algoritmos de asignación:

- **First Fit:** primer hueco libre suficientemente grande.
- **Next Fit:** continúa la búsqueda desde la última asignación y vuelve al inicio si es necesario.
- **Best Fit:** hueco libre más pequeño donde quepa el proceso.

Puede generar **fragmentación externa**.

### 2. Particiones fijas iguales

El usuario configura el tamaño de cada partición. El sistema calcula cuántas caben en el área de usuario y muestra el sobrante como memoria no particionada. Cada proceso recibe una partición completa. Genera **fragmentación interna**.

### 3. Particiones fijas de diferentes tamaños

El usuario introduce una lista de tamaños. El sistema asigna automáticamente la **partición libre más pequeña donde quepa** el proceso. También genera **fragmentación interna**.

### 4. Paginación

El programa se divide en páginas y la RAM en marcos del mismo tamaño. Las páginas de un mismo proceso pueden residir en marcos no contiguos.

- **Paginación simple:** todas las páginas del proceso deben estar presentes en RAM.
- **Paginación virtual:** las páginas se cargan bajo demanda desde el almacenamiento. Si no hay marcos libres, se reemplaza la página más antigua mediante **FIFO**. Permite ejecutar programas mayores que la RAM disponible.

### Visualización de RAM

La tabla de RAM identifica:

- El área del kernel.
- Cada proceso por PID y su rango de direcciones.
- Los huecos o particiones libres.
- La fragmentación interna y el espacio no particionado, según el método configurado.
- La instrucción del último tick ejecutado en la CPU seleccionada.

---

## Configuración

La RAM, el almacenamiento, la cantidad de CPU, el algoritmo, el método de memoria y sus parámetros se editan en la ventana **Configuración**.

| Parámetro              |         Mínimo | Valor por defecto |
| ---------------------- | -------------: | ----------------: |
| RAM                    | 200 posiciones |               500 |
| Almacenamiento         | 500 posiciones |               500 |
| CPU                    |              1 |                 1 |
| Quantum RR/SRR/Lottery |              1 |                 3 |

El kernel se calcula automáticamente:

```text
BCP                   = 24 posiciones
reserva instrucciones = 128 posiciones
bcpDeseados           = 5 × CPU
bcpPorRam             = (RAM − 128) / 24
bcpReales             = min(bcpDeseados, bcpPorRam)
kernel                = bcpReales × 24
```

Siempre debe caber al menos un BCP por CPU. La RAM restante se destina a las instrucciones de los procesos.

### Archivo de configuración

El archivo `config/minipc.txt` se conserva por compatibilidad con `Controlador(Path)`. Claves relevantes:

```properties
algoritmo=FCFS
metodoMemoria=DINAMICAS
estrategiaMemoria=FIRST_FIT
ram=256
almacenamiento=512
memoriaVirtual=64
indice=10
inicioUsuario=128
maxProcesosEnRam=5
maxProcesos=20
conservarArchivosAlReiniciar=true
```

Para particiones fijas:

```properties
metodoMemoria=FIJAS_IGUALES
tamañoParticion=32
```

```properties
metodoMemoria=FIJAS_DIFERENTES
tamañosParticiones=20,30,40
```

Para paginación:

```properties
metodoMemoria=PAGINACION
tamañoPagina=16
paginacionVirtual=true
```

La unidad interna es la **posición simulada**. Si la interfaz muestra KB, se usa la convención de visualización 1 posición = 1 KB simulado.

---

## Cómo ejecutar

Se requiere **JDK 25**.

1. Abrir el proyecto en NetBeans y ejecutarlo mediante `minipc.Main`.
2. Usar **Configuración** para elegir CPU, algoritmo, quantum, RAM, almacenamiento y método de memoria.
3. Pulsar **Cargar ASM** para importar uno o varios programas de la carpeta [`codigo`](codigo). **Ejecutar desde disco** permite ejecutar una copia ya guardada.
4. Pulsar **Siguiente** para avanzar manualmente o **Ejecutar** para iniciar el modo automático. **Pausar** detiene el avance automático.
5. Introducir un entero de 0 a 255 cuando un proceso solicite entrada de teclado.
6. Consultar los procesos, BCP, registros, RAM, disco, archivos y estadísticas.

---

## Estructura del proyecto

```text
src/minipc/
├── config/           Configuración y carga de archivos
├── controller/       Controlador principal
├── model/            Modelos de memoria, CPU, BCP y configuración
├── parser/           Validación y parseo de ASM
├── services/         Planificadores, gestión de procesos y memoria
│   └── memoria/
│       ├── dinamica/ First Fit, Next Fit y Best Fit
│       ├── fija/     Particiones fijas iguales y diferentes
│       ├── paginacion/ Paginación simple y virtual
│       └── seleccion/ Políticas de selección compartidas
└── ui/               Interfaz gráfica Swing

tests/                Pruebas sin dependencias externas
codigo/               Programas ASM de prueba
config/               Archivo de configuración por defecto
```

---

## Pruebas

El proyecto incluye pruebas ejecutables sin dependencias externas en la carpeta `tests/`:

- `PruebaHuecos.java` — huecos, fusión y First Fit.
- `PruebaEstrategiasMemoria.java` — Next Fit, Best Fit y configuración.
- `PruebaParticionesFijas.java` — particiones fijas, fragmentación, reutilización e intercambio.
- `PruebaAlmacenamientoPaginas.java` — respaldo por páginas, reversión y liberación.
- `PruebaPaginasVirtuales.java` — fallos, FIFO, recarga y liberación.

Compilación desde PowerShell:

```powershell
$clases = Join-Path $env:TEMP 'minipc-classes'
$fuentes = @(Get-ChildItem src,tests -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -d $clases @fuentes
```

Ejecución de una prueba:

```powershell
java -cp $clases PruebaPaginasVirtuales
```

---

## Programas de prueba

La carpeta [`codigo`](codigo) contiene programas ASM para probar la aplicación. Incluye `GUIA_PRUEBAS_RAM.md` con instrucciones para comparar las estrategias de asignación de memoria.

---

## Valoración del proyecto

**Valoración:** 5/5.

---

## Video de la aplicación en ejecución

**Enlace de YouTube:** https://www.youtube.com/watch?v=ljSP0X_8USQ

---

## Referencia teórica

William Stallings, _Operating Systems: Internals and Design Principles_, 9.ª edición, capítulos 7 y 8.
