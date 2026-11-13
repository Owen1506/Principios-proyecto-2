# Mini PC — Proyecto 1

Simulador de una computadora y de la administración de procesos, desarrollado en
Java con una interfaz gráfica Swing para el curso **IC-6600 Principios de Sistemas
Operativos**.

## Integrantes

- **Owen Caleb Smith Cerdas** — Carnet **2024083328**.

## Objetivo del proyecto

Simular la ejecución de programas en ensamblador mediante una CPU, planificación
FCFS, memoria principal y virtual, almacenamiento y servicios de entrada/salida.
La interfaz permite observar los procesos, sus estados, registros, memoria y
estadísticas de ejecución.

## Objetivos alcanzados

- **Ejecución de programas ASM:** lectura, validación sintáctica, interpretación y
  ejecución de las instrucciones implementadas.
- **Ejecución manual y automática:** el botón **Siguiente** avanza un tick de
  ejecución; cada tick consumido equivale a un segundo simulado de CPU. El botón
  **Ejecutar** avanza automáticamente cada segundo real, con posibilidad de pausar.
  Las instrucciones consumen el tiempo correspondiente a su peso.
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
- **Entrada y salida:** teclado mediante `INT 09H`, pantalla mediante `INT 10H`
  y finalización mediante `INT 20H`.
- **Configuración en la interfaz:** CPU, algoritmo, quantum, RAM y almacenamiento
  se editan en la ventana **Configuración**. RAM y almacenamiento son enteros con
  mínimo 500 y el kernel se calcula automáticamente. Los algoritmos disponibles
  son **FCFS**, **SPN**, **SRT**, **HRRN**, **RR** y **PRIORIDAD**.
- **Estadísticas por proceso:** inicio y fin en formato HH:mm, tiempo simulado de
  CPU en segundos, duración real, contexto y resultado de la ejecución.
- **Protección y errores de ejecución:** los saltos fuera de rango y los errores
  de pila o servicios de archivos finalizan el proceso afectado, registran el motivo,
  liberan sus recursos y permiten continuar con el siguiente proceso mediante FCFS.
  `PARAM` comprueba el espacio antes de insertar valores para evitar cambios parciales.
- **Reinicio coordinado:** limpieza de CPU, RAM, procesos, colas, memoria virtual,
  entradas pendientes y errores.

## Objetivos no alcanzados o pendientes de verificación

- **Verificación integral completa:** falta cerrar la revisión de todos los
  programas de prueba y todas las instrucciones, comparando sus resultados esperados.
- **Pruebas completas de archivos y estados:** falta ampliar la comprobación de
  combinaciones de servicios de archivos, falta de espacio y procesos bloqueados
  o suspendidos.
- **Verificación final desde la interfaz:** falta comprobar todos los escenarios
  de error y la continuación automática de los demás procesos en la versión final.

## Configuración y capacidad del kernel

La RAM y el almacenamiento se escriben en la ventana **Configuración** como
enteros mayores o iguales a **500**. El kernel se calcula al aplicar los cambios:

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
5. Introducir un entero de 0 a 255 cuando un proceso solicite entrada de teclado.
6. Consultar los procesos, BCP, registros, RAM, disco, archivos y estadísticas.



## Programas de prueba

La carpeta [`codigo`](codigo) contiene los programas ASM disponibles para probar
la aplicación..


## Video de la aplicación en ejecución

**Enlace de YouTube: https://www.youtube.com/watch?v=ljSP0X_8USQ**
