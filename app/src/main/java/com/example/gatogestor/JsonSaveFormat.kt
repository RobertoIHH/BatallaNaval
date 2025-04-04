package com.example.batallanavalgame

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Implementa el guardado y carga de partidas en formato JSON
 */
class JsonSaveFormat(private val context: Context) {

    private val gson: Gson = SaveGameUtils.createGson()

    companion object {
        private const val TAG = "JsonSaveFormat"
    }

    /**
     * Guarda una partida en formato JSON
     */
    fun guardarPartidaJSON(estadoJuego: EstadoPartida) {
        val jsonEstado = gson.toJson(estadoJuego)

        // Guardar en archivo
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_JSON)
        FileWriter(file).use { writer ->
            writer.write(jsonEstado)
        }
    }

    /**
     * Carga una partida desde formato JSON
     */
    fun cargarPartidaJSON(): EstadoPartida? {
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_JSON)
        if (!file.exists()) return null

        val jsonEstado = FileReader(file).use { reader ->
            reader.readText()
        }

        return try {
            val type = object : TypeToken<EstadoPartida>() {}.type
            gson.fromJson(jsonEstado, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error al deserializar JSON", e)
            null
        }
    }

    /**
     * Borra el archivo de guardado JSON
     */
    fun borrarArchivoGuardado() {
        try {
            File(context.filesDir, SaveGameUtils.SAVE_FILE_JSON).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar archivo de guardado JSON", e)
        }
    }
}