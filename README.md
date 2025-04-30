# Batalla Naval Game
## Integrantes
- Gonzalez Llamosas Noe
- Hernandez Hernandez Roberto Isaac

## Introducción

Batalla Naval es una implementación para Android del clásico juego de estrategia naval donde dos jugadores compiten para hundir los barcos de su oponente. Este juego permite a dos jugadores colocar sus barcos en un tablero de 10x10 y luego turnarse para atacar al oponente hasta que uno de ellos hunda todos los barcos del adversario.

La aplicación ofrece una experiencia completa que incluye:
- Configuración personalizada con nombres de jugadores
- Posicionamiento estratégico de barcos (horizontal o vertical)
- Sistema de turnos para ataques
- Registro de puntuaciones y estadísticas
- Guardado/carga de partidas en múltiples formatos (JSON, XML, TXT)
- Historial detallado de movimientos
- Temas visuales personalizables (tema normal y tema guinda IPN)
- Cronómetro para registrar el tiempo de juego

## Explicación General

El juego consta de dos fases principales:

1. **Fase de Configuración**: Cada jugador coloca sus barcos en el tablero. Los barcos incluyen:
   - Portaaviones (5 casillas)
   - Acorazado (4 casillas)
   - Crucero (3 casillas)
   - Submarino (3 casillas)
   - Destructor (2 casillas)

2. **Fase de Ataque**: Los jugadores se turnan para disparar a coordenadas del tablero enemigo. El juego informa si el ataque resultó en agua, impacto o hundimiento de un barco.

El juego termina cuando un jugador consigue hundir todos los barcos de su oponente. Se registra la victoria, se actualiza la puntuación y se muestra un resumen de la partida.

## Desarrollo: Descripción Técnica

### Arquitectura

La aplicación está desarrollada en Kotlin para Android y sigue un diseño basado en:

- **Modelo-Vista-Controlador (MVC)**: Separación clara entre la lógica de juego, la interfaz de usuario y los controladores.
- **Manejo de estados**: Uso de estructuras de datos para representar el estado del juego completo.
- **Sistema de persistencia multiformato**: Diferentes estrategias para guardar y cargar partidas.

### Componentes Principales

1. **Actividades**:
   - `MainActivity`: Pantalla principal con opciones para nueva partida o cargar existente.
   - `PlayerInputActivity`: Entrada de nombres de jugadores.
   - `BatallaNavalActivity`: Actividad principal donde se desarrolla el juego.

2. **Gestión de Datos**:
   - `SaveGameManager`: Administrador central de guardado/carga de partidas.
   - `JsonSaveFormat`: Implementación de guardado en formato JSON.
   - `AlternativeSaveFormats`: Implementaciones para XML y TXT.
   - `ExternalStorageSaveManager`: Manejo de almacenamiento externo.
   - `DataStoreSaveManager`: Guardado usando DataStore de Android.

3. **Lógica de Juego**:
   - `BatallaNavalGameLogic`: Encapsula las reglas del juego.
   - `BatallaNavalManager`: Coordina la lógica y los sistemas de guardado.

4. **Modelos de Datos**:
   - `EstadoPartida`: Representa el estado completo del juego.
   - `Barco`: Representa los tipos de barcos y sus características.
   - `BarcoColocado`: Representa un barco ya posicionado en el tablero.
   - `Movimiento`: Registra los movimientos realizados durante el juego.

### Diagramas UML

#### Diagrama de Clases

```
+----------------+         +------------------+         +-------------------+
| MainActivity   |-------->| PlayerInputActivity|------->| BatallaNavalActivity|
+----------------+         +------------------+         +-------------------+
                                                               |
                                                               |
                                                               v
+------------------+      +----------------+      +----------------------+
| BatallaNavalManager|<----| SaveGameManager |<-----| BatallaNavalGameLogic |
+------------------+      +----------------+      +----------------------+
        |                      |
        |                      |
        v                      v
+------------------+    +------------------+    +------------------+    +------------------+
| JsonSaveFormat   |    | AlternativeSaveFormats|    | ExternalStorageSaveManager|    | DataStoreSaveManager|
+------------------+    +------------------+    +------------------+    +------------------+
```

#### Diagrama de Flujo de Juego

```
+------------+     +-----------------+     +--------------------+
| Inicio     |---->| Ingresar Nombres|---->| Configuración Barcos|
+------------+     +-----------------+     +--------------------+
                                                   |
                                                   v
+------------+     +-----------------+     +--------------------+
| Victoria   |<----| Fase de Ataque  |<----| Cambio de Jugador  |
+------------+     +-----------------+     +--------------------+
      |                    ^                        |
      |                    |                        |
      v                    +------------------------+
+------------+
| Fin Juego  |
+------------+
```

## Implementación de Operaciones de Archivos

Una característica destacada de la aplicación es su sistema avanzado de gestión de archivos para guardar y cargar partidas en múltiples formatos.

### Características del Sistema de Archivos

- **Múltiples formatos de guardado**: JSON, XML y TXT
- **Múltiples ubicaciones de almacenamiento**: Almacenamiento interno, DataStore y almacenamiento externo
- **Recuperación resiliente**: Intento secuencial en diferentes formatos si el preferido falla
- **Sistema de respaldo**: Guardado automático en JSON si falla el formato preferido por el usuario

### Implementación Técnica

1. **Abstracción**: La clase `SaveGameManager` proporciona una interfaz unificada para las operaciones de guardado/carga.

2. **Estrategia**: Cada formato de guardado es implementado como una estrategia diferente:
   - `JsonSaveFormat`: Serialización y deserialización usando la biblioteca Gson.
   - `AlternativeSaveFormats`: Manejo de XML usando XMLPullParser y texto plano.

3. **Serialización personalizada**: Implementación de TypeAdapters personalizados para manejar correctamente estructuras de datos complejas como arrays bidimensionales.

4. **Manejo de errores**: Sistema completo de gestión de errores con intentos fallback a otros formatos para garantizar la recuperación de datos.

5. **Adaptadores de tipos**: Para resolver problemas de serialización/deserialización, se implementaron adaptadores personalizados para arrays:
   ```kotlin
   class ArraysTypeAdapter<T> : JsonSerializer<Array<Array<T>>>, JsonDeserializer<Array<Array<T>>>
   ```

6. **Guardado temporal para cambios de tema**: Sistema especializado para preservar el estado durante cambios de configuración como rotación de pantalla o cambio de tema:
   ```kotlin
   fun guardarPartidaTemporalCambioTema(estadoJuego: EstadoPartida)
   fun cargarPartidaTemporalCambioTema(): EstadoPartida?
   ```

7. **Conversión entre formatos**: Implementación de convertidores para transformar entre diferentes representaciones de datos:
   ```kotlin
   private fun convertirAPartidaGuardada(estadoJuego: EstadoPartida): PartidaGuardada
   private fun convertirDesdePartidaGuardada(partidaGuardada: PartidaGuardada): EstadoPartida
   ```

## Conclusiones

Batalla Naval es una aplicación completa que demuestra la implementación de conceptos avanzados de programación Android:

- Manejo de estados complejos de aplicación
- Persistencia de datos en múltiples formatos
- Diseño responsive para adaptarse a diferentes dispositivos
- Modularización de la lógica de negocio
- Uso apropiado de patrones de diseño
- Manejo eficiente de recursos Android

Este proyecto sirve como un excelente ejemplo de aplicación de juego con una arquitectura bien estructurada y funcionalidad completa de persistencia de datos.
