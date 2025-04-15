package com.example.batallanavalgame

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*

/**
 * Gestiona todas las operaciones de guardado/carga usando las implementaciones específicas
 */
class SaveGameManager(private val context: Context) {

    // Formatos de guardado soportados
    enum class SaveFormat {
        JSON, XML, TEXT
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    // Dependencias para los distintos formatos
    private val jsonSaveFormat = JsonSaveFormat(context)
    private val alternativeSaveFormats = AlternativeSaveFormats(context)
    private val gson: Gson = SaveGameUtils.createGson()

    companion object {
        private const val PREFS_NAME = "batalla_naval_prefs"
        private const val KEY_SAVE_FORMAT = "formato_guardado"
        private const val KEY_PLAYER1_WINS = "victorias_jugador1"
        private const val KEY_PLAYER2_WINS = "victorias_jugador2"
        private const val KEY_GAMES_PLAYED = "partidas_jugadas"
        private const val TAG = "SaveGameManager"

        // Constantes para nombres de archivos
        const val SAVE_FILE_JSON = "batalla_naval_save.json"
        const val SAVE_FILE_XML = "batalla_naval_save.xml"
        const val SAVE_FILE_TEXT = "batalla_naval_save.txt"
    }

    /**
     * Guarda el estado del juego en el formato seleccionado
     */
    fun guardarPartida(
        estadoJuego: EstadoPartida,
        formato: SaveFormat = getSavedFormat(),
        vista: View? = null
    ) {
        // Guardar formato seleccionado
        guardarFormatoSeleccionado(formato)

        try {
            when (formato) {
                SaveFormat.JSON -> jsonSaveFormat.guardarPartidaJSON(estadoJuego)
                SaveFormat.XML -> {
                    // Convertir EstadoPartida a PartidaGuardada para XML
                    val partidaGuardada = convertirAPartidaGuardada(estadoJuego)
                    alternativeSaveFormats.guardarPartidaXML(partidaGuardada, SAVE_FILE_XML)
                }
                SaveFormat.TEXT -> alternativeSaveFormats.guardarPartidaTXT(estadoJuego)
            }

            if (vista != null) {
                Snackbar.make(
                    vista,
                    context.getString(R.string.partida_guardada_formato, formato.name),
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.partida_guardada),
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar partida en formato $formato", e)

            // Si falla el formato seleccionado, intentar con JSON como respaldo
            if (formato != SaveFormat.JSON) {
                try {
                    jsonSaveFormat.guardarPartidaJSON(estadoJuego)
                    if (vista != null) {
                        Snackbar.make(
                            vista,
                            context.getString(R.string.backup_guardado_json),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Error al intentar guardar en formato de respaldo JSON", e2)
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_guardar_partida, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_guardar_partida, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Carga el estado del juego desde cualquier formato guardado disponible
     */
    fun cargarPartida(): EstadoPartida? {
        // Intentar cargar en el formato preferido del usuario
        val formatoPreferido = getSavedFormat()
        var estado = try {
            when (formatoPreferido) {
                SaveFormat.JSON -> jsonSaveFormat.cargarPartidaJSON()
                SaveFormat.XML -> {
                    val partidaGuardada = alternativeSaveFormats.cargarPartidaXML(SAVE_FILE_XML)
                    if (partidaGuardada != null) {
                        convertirDesdePartidaGuardada(partidaGuardada)
                    } else null
                }
                SaveFormat.TEXT -> alternativeSaveFormats.cargarPartidaTXT()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar partida en formato $formatoPreferido", e)
            null
        }

        // Si falla, intentar los otros formatos
        if (estado == null && formatoPreferido != SaveFormat.JSON) {
            estado = try {
                jsonSaveFormat.cargarPartidaJSON()
            } catch (e: Exception) {
                null
            }
        }

        if (estado == null && formatoPreferido != SaveFormat.XML) {
            estado = try {
                val partidaGuardada = alternativeSaveFormats.cargarPartidaXML(SAVE_FILE_XML)
                if (partidaGuardada != null) {
                    convertirDesdePartidaGuardada(partidaGuardada)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        if (estado == null && formatoPreferido != SaveFormat.TEXT) {
            estado = try {
                alternativeSaveFormats.cargarPartidaTXT()
            } catch (e: Exception) {
                null
            }
        }

        return estado
    }

    /**
     * Convierte un EstadoPartida a PartidaGuardada (para formato XML)
     */
    private fun convertirAPartidaGuardada(estadoJuego: EstadoPartida): PartidaGuardada {
        val partidaGuardada = PartidaGuardada()

        // Configurar fecha y dificultad
        partidaGuardada.fecha = System.currentTimeMillis()
        partidaGuardada.dificultad = "NORMAL" // O usar un valor del estado si existe

        // Convertir tableros (simplificado)
        // Aquí se asume que el enum EstadoCelda tiene un ordinal que puede servir como valor numérico
        val tableroJugador = Array(10) { i ->
            Array(10) { j ->
                estadoJuego.tableroJugador1[i][j].ordinal
            }
        }
        val tableroOponente = Array(10) { i ->
            Array(10) { j ->
                estadoJuego.tableroJugador2[i][j].ordinal
            }
        }

        partidaGuardada.tableroJugador = tableroJugador
        partidaGuardada.tableroOponente = tableroOponente

        // Convertir barcos (simplificado)
        val barcos = mutableMapOf<String, DatosBarco>()

        // Agregar barcos del jugador 1
        estadoJuego.barcosJugador1.forEachIndexed { index, barcoColocado ->
            if (barcoColocado.posiciones.isNotEmpty()) {
                val primeraPosicion = barcoColocado.posiciones.first()
                val esHorizontal = barcoColocado.posiciones.size > 1 &&
                        barcoColocado.posiciones[0].first == barcoColocado.posiciones[1].first

                val datosBarco = DatosBarco(
                    primeraPosicion.first,
                    primeraPosicion.second,
                    esHorizontal,
                    barcoColocado.longitud
                )
                barcos["barco_j1_$index"] = datosBarco
            }
        }

        // Agregar barcos del jugador 2
        estadoJuego.barcosJugador2.forEachIndexed { index, barcoColocado ->
            if (barcoColocado.posiciones.isNotEmpty()) {
                val primeraPosicion = barcoColocado.posiciones.first()
                val esHorizontal = barcoColocado.posiciones.size > 1 &&
                        barcoColocado.posiciones[0].first == barcoColocado.posiciones[1].first

                val datosBarco = DatosBarco(
                    primeraPosicion.first,
                    primeraPosicion.second,
                    esHorizontal,
                    barcoColocado.longitud
                )
                barcos["barco_j2_$index"] = datosBarco
            }
        }

        partidaGuardada.barcos = barcos

        return partidaGuardada
    }

    /**
     * Convierte una PartidaGuardada a EstadoPartida (desde formato XML)
     */
    private fun convertirDesdePartidaGuardada(partidaGuardada: PartidaGuardada): EstadoPartida {
        // Convertir los tableros
        val tableroJugador1 = Array(10) { i ->
            Array(10) { j ->
                val valor = partidaGuardada.tableroJugador[i][j]
                when(valor) {
                    0 -> EstadoCelda.VACIA
                    1 -> EstadoCelda.BARCO
                    2 -> EstadoCelda.AGUA
                    3 -> EstadoCelda.IMPACTO
                    else -> EstadoCelda.VACIA
                }
            }
        }

        val tableroJugador2 = Array(10) { i ->
            Array(10) { j ->
                val valor = partidaGuardada.tableroOponente[i][j]
                when(valor) {
                    0 -> EstadoCelda.VACIA
                    1 -> EstadoCelda.BARCO
                    2 -> EstadoCelda.AGUA
                    3 -> EstadoCelda.IMPACTO
                    else -> EstadoCelda.VACIA
                }
            }
        }

        // Tableros de ataques (simplificados)
        val tableroAtaquesJugador1 = Array(10) { Array(10) { false } }
        val tableroAtaquesJugador2 = Array(10) { Array(10) { false } }

        // Convertir barcos
        val barcosJugador1 = mutableListOf<BarcoColocado>()
        val barcosJugador2 = mutableListOf<BarcoColocado>()

        // Procesar barcos desde XML
        for ((id, barco) in partidaGuardada.barcos) {
            val posiciones = mutableListOf<Pair<Int, Int>>()

            // Generar las posiciones basadas en la orientación y tamaño
            for (i in 0 until barco.tamaño) {
                val fila = if (barco.esHorizontal) barco.fila else barco.fila + i
                val columna = if (barco.esHorizontal) barco.columna + i else barco.columna
                posiciones.add(Pair(fila, columna))
            }

            val barcoColocado = BarcoColocado(barco.tamaño, posiciones)

            if (id.startsWith("barco_j1_")) {
                barcosJugador1.add(barcoColocado)
            } else {
                barcosJugador2.add(barcoColocado)
            }
        }

        // Crear un estado de partida con los datos disponibles
        return EstadoPartida(
            faseActual = FaseJuego.CONFIGURACION, // Fase por defecto
            jugadorActual = 1, // Jugador actual por defecto
            barcoActualIndex = 0, // Índice de barco actual por defecto
            orientacionHorizontal = true, // Orientación horizontal por defecto
            tableroJugador1 = tableroJugador1,
            tableroJugador2 = tableroJugador2,
            tableroAtaquesJugador1 = tableroAtaquesJugador1,
            tableroAtaquesJugador2 = tableroAtaquesJugador2,
            barcosJugador1 = barcosJugador1,
            barcosJugador2 = barcosJugador2,
            nombreJugador1 = "Jugador 1", // Nombre predeterminado
            nombreJugador2 = "Jugador 2", // Nombre predeterminado
            puntajeJugador1 = 0, // Puntuación por defecto
            puntajeJugador2 = 0, // Puntuación por defecto
            tiempoJuegoSegundos = 0L, // Tiempo de juego por defecto
            historialMovimientos = emptyList() // Historial de movimientos vacío
        )
    }

    // Guarda el formato preferido del usuario
    fun guardarFormatoSeleccionado(formato: SaveFormat) {
        sharedPreferences.edit().putString(KEY_SAVE_FORMAT, formato.name).apply()
    }

    // Obtiene el formato de guardado preferido del usuario
    fun getSavedFormat(): SaveFormat {
        val formatName = sharedPreferences.getString(KEY_SAVE_FORMAT, SaveFormat.JSON.name)
        return try {
            SaveFormat.valueOf(formatName ?: SaveFormat.JSON.name)
        } catch (e: Exception) {
            SaveFormat.JSON
        }
    }

    // Métodos para registro de estadísticas de juego
    fun registrarVictoria(jugador: Int) {
        val key = if (jugador == 1) KEY_PLAYER1_WINS else KEY_PLAYER2_WINS
        val victorias = sharedPreferences.getInt(key, 0) + 1
        sharedPreferences.edit().putInt(key, victorias).apply()

        // Incrementar partidas jugadas
        val partidasJugadas = sharedPreferences.getInt(KEY_GAMES_PLAYED, 0) + 1
        sharedPreferences.edit().putInt(KEY_GAMES_PLAYED, partidasJugadas).apply()
    }

    fun getVictoriasJugador(jugador: Int): Int {
        val key = if (jugador == 1) KEY_PLAYER1_WINS else KEY_PLAYER2_WINS
        return sharedPreferences.getInt(key, 0)
    }

    fun getPartidasJugadas(): Int {
        return sharedPreferences.getInt(KEY_GAMES_PLAYED, 0)
    }

    fun resetearEstadisticas() {
        sharedPreferences.edit()
            .putInt(KEY_PLAYER1_WINS, 0)
            .putInt(KEY_PLAYER2_WINS, 0)
            .putInt(KEY_GAMES_PLAYED, 0)
            .apply()
    }

    fun borrarPartidaGuardada() {
        // Borrar todos los formatos
        jsonSaveFormat.borrarArchivoGuardado()
        alternativeSaveFormats.borrarArchivosGuardados()
    }
}