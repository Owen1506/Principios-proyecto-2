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
  Todas las instrucciones tienen peso 1 y consumen un segundo simulado.
- **Administración de procesos:** identificación por PID, BCP, estados del proceso,
  planificación FCFS y almacenamiento/restauración del contexto de CPU.
- **Límite de RAM:** hasta cinco procesos simultáneos en memoria principal,
  incluidos los bloqueados.
- **Memoria principal y kernel:** división fija configurada al inicio y visualización
  de los atributos del BCP, uno por posición del kernel.
- **Memoria virtual:** admisión de programas en estado `PREPARADO_SUSPENDIDO`
  cuando no pueden ingresar a RAM y reactivación al liberarse espacio y un cupo.
- **Almacenamiento simulado:** copia de los programas en disco, control de capacidad
  e índice de nombres y direcciones en sus primeras posiciones. Los programas
  ocupan una posición por instrucción.
- **Servicios de archivos:** crear, abrir, leer, escribir y eliminar mediante
  `INT 21H`, con control de archivos abiertos por proceso.
- **Interrupciones:** pantalla mediante `INT 10H`, finalización mediante
  `INT 20H` y archivos mediante `INT 21H`, sin esperar Enter.
  La entrada por teclado `INT 09H` ya no está disponible.
- **Configuración externa:** archivo TXT para los tamaños de memoria y
  almacenamiento, partición de kernel, límites de procesos y algoritmo. El
  algoritmo disponible actualmente es **FCFS**.
- **Estadísticas por proceso:** inicio y fin en formato HH:mm, tiempo simulado de
  CPU en segundos, duración real, contexto y resultado de la ejecución.
- **Protección y errores de ejecución:** los saltos fuera de rango y los errores
  de pila o servicios de archivos finalizan el proceso afectado, registran el motivo,
  liberan sus recursos y permiten continuar con el siguiente proceso mediante FCFS.
  `PARAM` comprueba el espacio antes de insertar valores para evitar cambios parciales.
- **Reinicio coordinado:** limpieza de CPU, RAM, procesos, colas, memoria virtual,
  salidas y errores.

## Objetivos no alcanzados o pendientes de verificación

- **Verificación integral completa:** falta cerrar la revisión de todos los
  programas de prueba y todas las instrucciones, comparando sus resultados esperados.
- **Pruebas completas de archivos y estados:** falta ampliar la comprobación de
  combinaciones de servicios de archivos, falta de espacio y procesos bloqueados
  o suspendidos.
- **Verificación final desde la interfaz:** falta comprobar todos los escenarios
  de error y la continuación automática de los demás procesos en la versión final.

## Configuración y capacidad del kernel

La configuración inicial se encuentra en [`config/minipc.txt`](config/minipc.txt):

```txt
algoritmo=FCFS
ram=256
almacenamiento=512
memoriaVirtual=64
indice=10
inicioUsuario=128
maxProcesosEnRam=5
maxProcesos=20
conservarArchivosAlReiniciar=true
```

`indice=10` reserva diez entradas para nombres y direcciones de archivos. El
espacio de archivos y el de memoria virtual forman parte del almacenamiento total.

El kernel ocupa las posiciones anteriores a `inicioUsuario` y no puede superar
el 50% de RAM. Cada BCP ocupa **24 posiciones**, por lo que el kernel inicial de
128 posiciones permite alojar cinco BCP activos, incluidos los de procesos
suspendidos. `maxProcesos` es un límite adicional y también está sujeto al espacio
disponible para BCP.

Para demostrar cinco procesos en RAM y otro esperando en virtual, se puede usar
`ram=512` e `inicioUsuario=256`, manteniendo `maxProcesosEnRam=5`. La partición
permanece fija durante la simulación.

## Cómo ejecutar

Se requiere **JDK 25**, según la configuración actual del proyecto.

1. Abrir el proyecto en NetBeans y ejecutarlo mediante `minipc.Main`.
2. Usar **Configuración** si se desea seleccionar otro archivo TXT.
3. Pulsar **Cargar ASM** para importar uno o varios programas de la carpeta
   [`codigo`](codigo). **Ejecutar desde disco** permite ejecutar una copia ya guardada.
4. Pulsar **Siguiente** para avanzar manualmente o **Ejecutar** para iniciar el
   modo automático. **Pausar** detiene el avance automático.
5. Consultar los procesos, BCP, registros, RAM, disco, archivos y estadísticas.



## Programas de prueba

La carpeta [`codigo`](codigo) contiene los programas ASM disponibles para probar
la aplicación..


## Video de la aplicación en ejecución

**Enlace de YouTube: https://www.youtube.com/watch?v=ljSP0X_8USQ**
