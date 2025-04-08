package com.example.batallanavalgame

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.xmlpull.v1.XmlPullParser
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
                SaveFormat.XML -> alternativeSaveFormats.guardarPartidaXML(estadoJuego)
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
                SaveFormat.XML -> alternativeSaveFormats.cargarPartidaXML()
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
                alternativeSaveFormats.cargarPartidaXML()
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

    // Métodos para extraer tableros de los mapas
    private fun extraerTablero(data: Any?): Array<Array<EstadoCelda>>? {
        if (data == null) return null

        // Convertimos de vuelta a JSON y utilizamos Gson para la deserialización
        val json = gson.toJson(data)
        val type = object : TypeToken<Array<Array<EstadoCelda>>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun extraerTableroAtaques(data: Any?): Array<Array<Boolean>>? {
        if (data == null) return null

        val json = gson.toJson(data)
        val type = object : TypeToken<Array<Array<Boolean>>>() {}.type
        return gson.fromJson(json, type)
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