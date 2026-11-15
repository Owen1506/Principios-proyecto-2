# Mini PC — Proyecto 2

Simulador de una computadora y de la administración de procesos, desarrollado en
Java con una interfaz gráfica Swing para el curso **IC-6600 Principios de Sistemas
Operativos**.

## Integrantes

- **Owen Caleb Smith Cerdas** — Carnet **2024083328**.

## Objetivo del proyecto

Simular la ejecución de programas en ensamblador con hasta cinco CPU, algoritmos
de planificación, memoria principal y secundaria y servicios de entrada/salida.
La interfaz permite observar los procesos, sus estados, registros, memoria y
estadísticas de ejecución.

## Objetivos alcanzados

- **Ejecución de programas ASM:** lectura, validación sintáctica, interpretación y
  ejecución de las instrucciones implementadas.
- **Ejecución manual y automática:** el botón **Siguiente** avanza un tick de
  ejecución; cada tick consumido equivale a un segundo simulado de CPU. El botón
  **Ejecutar** avanza automáticamente cada segundo real, con posibilidad de pausar.
  Todas las instrucciones cargadas desde ASM tienen peso 1 y duran un tick.
- **Diagrama de Gantt:** el botón junto a los controles de ejecución abre el
  cronograma en una ventana independiente y redimensionable. Se actualiza durante
  la simulación y se puede cerrar y volver a abrir sin perder el historial.
- **Administración de procesos:** identificación por PID, BCP, estados del proceso,
  planificación FCFS y almacenamiento/restauración del contexto de CPU.
- **Límite de RAM:** los BCP se alojan según el kernel calculado; el objetivo es
  cinco procesos activos por CPU (máximo 25) y siempre se reservan 128 posiciones
  para las instrucciones.
- **Memoria principal y kernel:** división fija configurada al inicio y visualización
  de los atributos del BCP, uno por posición del kernel.
- **Memoria virtual:** admisión de programas en estado `PREPARADO_SUSPENDIDO`
  cuando no pueden ingresar a RAM y reactivación al liberarse espacio y un cupo.
- **Almacenamiento simulado:** copia de los programas en disco, control de capacidad
  e índice de nombres y direcciones en sus primeras posiciones. Los programas
  ocupan espacio según la suma de los pesos de sus instrucciones.
- **Servicios de archivos:** crear, abrir, leer, escribir y eliminar mediante
  `INT 21H`, con control de archivos abiertos por proceso.
- **Entrada y salida:** pantalla mediante `INT 10H` y finalización mediante
  `INT 20H`. El parser rechaza `INT 09H` y la entrada de teclado está deshabilitada.
- **Asignación de memoria:** particiones fijas iguales o diferentes, particiones
  dinámicas con First Fit, Next Fit y Best Fit, y paginación simple con marcos no
  contiguos y tablas de páginas por programa residente.
- **Configuración en la interfaz:** CPU, algoritmo, quantum, RAM y almacenamiento
  se editan en la ventana **Configuración**. La RAM tiene mínimo 200 y el
  almacenamiento 500; el kernel se calcula automáticamente. Los algoritmos
  disponibles son **FCFS**, **SPN**, **SRT**, **HRRN**, **RR**, **PRIORIDAD**,
  **SELFISH_ROUND_ROBIN**, **LOTTERY**, **GUARANTEED_SCHEDULING** y
  **MULTILEVEL_FEEDBACK_QUEUE**.
- **Estadísticas por proceso:** inicio y fin en formato HH:mm, tiempo simulado de
  CPU en segundos, duración real, contexto y resultado de la ejecución.
- **Protección y errores de ejecución:** los saltos fuera de rango y los errores
  de pila o servicios de archivos finalizan el proceso afectado, registran el motivo,
  liberan sus recursos y permiten continuar con el siguiente proceso mediante FCFS.
  `PARAM` comprueba el espacio antes de insertar valores para evitar cambios parciales.
- **Reinicio coordinado:** limpieza de CPU, RAM, procesos, colas, memoria virtual,
  entradas pendientes y errores.

## Objetivos no alcanzados o pendientes de verificación

- **Paginación con memoria virtual:** queda pendiente la residencia parcial de
  procesos y la carga individual de páginas ausentes. La paginación actual carga
  todas las páginas y conserva la suspensión/reactivación de programas completos.
- **Verificación integral completa:** falta cerrar la revisión de todos los
  programas de prueba y todas las instrucciones, comparando sus resultados esperados.
- **Pruebas completas de archivos y estados:** falta ampliar la comprobación de
  combinaciones de servicios de archivos, falta de espacio y procesos bloqueados
  o suspendidos.
- **Verificación final desde la interfaz:** falta comprobar todos los escenarios
  de error y la continuación automática de los demás procesos en la versión final.

## Configuración y capacidad del kernel

### Estructura de asignación de memoria

La asignación del área de usuario se organiza en:

```text
src/minipc/services/memoria/
├── EstrategiaAsignacionMemoria.java
├── AsignacionMemoria.java
├── dinamica/                 # First Fit, Next Fit y Best Fit
├── fija/                     # Particiones iguales y diferentes
└── paginacion/
    ├── GestorPaginacion.java
    ├── TablaPaginas.java
    ├── EntradaPagina.java
    └── Marco.java
```

- `EstrategiaAsignacionMemoria` define cómo reservar, liberar y consultar espacio.
- `AsignacionMemoria` conserva las posiciones físicas de las instrucciones en su
  orden lógico. La posición lógica 0 corresponde a la primera instrucción del programa.
- `FirstFit` reserva el primer bloque contiguo suficiente y permite reutilizarlo
  cuando el programa termina o se suspende.
- `Memoria` guarda las instrucciones y las reservas vigentes. Su constructor
  permite recibir otra estrategia; el constructor habitual utiliza First Fit.

El PC de la CPU y del BCP ahora es **lógico**: comienza en 0 y no cambia al mover
el programa de ubicación. `Memoria.obtenerDireccionFisica(proceso, posicionLogica)`
traduce esa posición antes de leer RAM. Los saltos siguen siendo relativos al PC.
`LOAD` y `STORE` operan con registros y acumulador; no acceden a direcciones de RAM.
La interfaz muestra el PC lógico y las direcciones físicas por separado.

La reserva del kernel y sus BCP se conserva en todas las modalidades. Las tablas
de páginas son metadatos del simulador y no aumentan las 24 posiciones reservadas
por BCP. La suspensión y reactivación siguen trasladando programas completos.

### Paginación simple

En **Configuración → Método de memoria → Paginación**, elegir el tamaño de página
en posiciones de memoria. Debe ser una potencia de 2 positiva (1, 2, 4, 8, 16,
32, etc.) y caber al menos un marco en el área de usuario. El valor inicial es 16.

- Los marcos se numeran desde 0 a partir del inicio del área de usuario, después
  del kernel. El sobrante que no complete un marco no se ofrece como espacio libre.
- Cada proceso reserva todos sus marcos antes de cargarse. Un programa de 10
  instrucciones con páginas de 4 necesita 3 marcos; desperdicia 2 posiciones en
  su última página. Esas posiciones no se pueden asignar a otro proceso.
- Se utilizan los primeros marcos libres, aunque estén separados. La dirección
  lógica se traduce con `página = PC / tamañoPagina` y
  `desplazamiento = PC % tamañoPagina`; la dirección física es el inicio del marco
  indicado en la tabla más el desplazamiento.
- Si faltan marcos, el programa completo espera en secundaria cuando existe
  espacio y un BCP disponible. Al reactivarlo se reconstruye su tabla en los marcos
  disponibles, conservando PC y registros. Al finalizar o fallar libera sus marcos.
- La pestaña **Marcos** muestra ocupación, PID, página y fragmentación interna.
  **RAM** distingue páginas, marcos libres, fragmentación y sobrante sin marco.
  **Ver BCP → Páginas** muestra las direcciones lógicas y físicas de cada página.
  Los procesos suspendidos no muestran los marcos de su asignación anterior.
- Esta etapa corresponde a la paginación simple de la sección 7.3 del material:
  no incorpora carga por demanda, fallos de página ni reemplazo FIFO.

Para `Controlador(Path)`, el archivo de configuración admite `metodoMemoria=PAGINACION`
y `tamañoPagina=16` (UTF-8). Si se omite el tamaño se utiliza 16.

Durante la implementación se ejecutaron 743 comprobaciones de fragmentación,
reservas fallidas, reutilización, traducción de posiciones separadas, suspensión
con reubicación, ejecución con los diez planificadores y cinco CPU, saltos y
liberación tras errores. También se comprobaron las tablas de Swing. Los archivos
auxiliares de esas verificaciones se retiraron en la limpieza del proyecto.

### Tamaños y reserva del kernel

La RAM y el almacenamiento se escriben en la ventana **Configuración** como
enteros con mínimos de **200** y **500**, respectivamente. Además, la RAM debe
permitir al menos un BCP por CPU y la reserva de instrucciones. El kernel se
calcula al aplicar los cambios:

```text
BCP                   = 24 posiciones
reserva instrucciones = 128 posiciones
bcpDeseados           = 5 × CPU
bcpPorRam             = (RAM − 128) / 24
bcpReales             = min(bcpDeseados, bcpPorRam)
kernel                = bcpReales × 24
```

Siempre debe caber al menos un BCP por CPU y el espacio restante se destina a las
instrucciones de los procesos. Ejemplos:

| RAM | CPU | Kernel | BCP | Instrucciones |
|---:|---:|---:|---:|---:|
| 500 | 1 | 120 | 5 | 380 |
| 500 | 5 | 360 | 15 | 140 |
| 728 | 5 | 600 | 25 | 128 |
| 1024 | 5 | 600 | 25 | 424 |

Con el mínimo de 500 y cinco CPU caben 15 BCP; para los 25 se necesitan al menos
728 posiciones de RAM. La interfaz muestra esta vista previa mientras se editan
los valores. Cambiar RAM, almacenamiento o CPU reinicia la simulación porque la
memoria y el disco tienen tamaño fijo.

El archivo [`config/minipc.txt`](config/minipc.txt) se conserva únicamente por
compatibilidad con `Controlador(Path)`; la interfaz ya no lo lee.

## Planificación por prioridad

**Selfish Round Robin (SRR)** es un algoritmo separado de PRIORIDAD: por cada
CPU los procesos admitidos comparten turnos de tamaño quantum. Los que esperan
fuera del grupo aumentan su prioridad a razón `a` por tick; los admitidos,
incluidos los que esperan su turno, aumentan a razón `b`. Cuando un proceso
alcanza la prioridad del grupo, se incorpora a sus turnos. Configure `a` y `b`
en la ventana de configuración, con `0 < b < a` (por defecto 2 y 1).
La prioridad configurada es el valor inicial; los procesos bloqueados o
suspendidos conservan su prioridad sin aumentarla. Referencia: Stallings,
9.ª edición, problema 9.12 y figura 9.19, página 458.

El algoritmo **PRIORIDAD** usa un número mayor como mayor prioridad. La prioridad
se edita por proceso en la fila `Prioridad ✎` de la tabla o en el spinner de su
tarjeta, con las mismas reglas que la llegada: entero mayor o igual a cero y solo
antes de iniciar la simulación. Si dos procesos empatan, se atiende primero el de
menor tiempo de llegada y luego el PID menor.

La prioridad es estática: no aumenta con el tiempo de espera. La variable
`expropia` de `Prioridad` vale `true` por defecto; si se cambia a `false`, el
proceso actual no se interrumpe y solo se elige otro cuando termina o se bloquea.
Con `expropia = true`, un proceso preparado con prioridad estrictamente mayor
interrumpe al que está usando la CPU.

## Cómo ejecutar

Se requiere **JDK 25**, según la configuración actual del proyecto.

1. Abrir el proyecto en NetBeans y ejecutarlo mediante `minipc.Main`.
2. Usar **Configuración** para elegir CPU, algoritmo, quantum, RAM y almacenamiento.
3. Pulsar **Cargar ASM** para importar uno o varios programas de la carpeta
   [`codigo`](codigo). **Ejecutar desde disco** permite ejecutar una copia ya guardada.
4. Pulsar **Siguiente** para avanzar manualmente o **Ejecutar** para iniciar el
   modo automático. **Pausar** detiene el avance automático.
5. Para paginación, consultar **Marcos**, **RAM** y **Ver BCP → Páginas**.
6. Consultar los procesos, BCP, registros, RAM, disco, archivos y estadísticas.



## Programas de prueba

La carpeta [`codigo`](codigo) contiene los programas ASM disponibles para probar
la aplicación..


## Video de la aplicación en ejecución

**Enlace de YouTube: https://www.youtube.com/watch?v=ljSP0X_8USQ**
