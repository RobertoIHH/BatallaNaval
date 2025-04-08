package com.example.batallanavalgame

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Gestor para guardar partidas en almacenamiento externo donde el usuario puede acceder
 */
class ExternalStorageSaveManager(private val context: Context) {
    private val gson = SaveGameUtils.createGson()

    companion object {
        private const val TAG = "ExternalStorageSaveManager"
        private const val SAVE_DIRECTORY = "BatallaNaval"
        private const val SAVE_FILE_JSON = "batalla_naval_save.json"
    }

    /**
     * Guarda una partida en el almacenamiento externo en un archivo JSON
     */
    fun guardarPartida(estadoJuego: EstadoPartida): Boolean {
        try {
            // Verificar que el almacenamiento externo está disponible
            if (!isExternalStorageWritable()) {
                Log.e(TAG, "Almacenamiento externo no disponible para escritura")
                return false
            }

            // Crear el directorio si no existe
            val directory = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), SAVE_DIRECTORY)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "No se pudo crear el directorio para guardar")
                    return false
                }
            }

            // Crear el archivo y escribir el JSON
            val file = File(directory, SAVE_FILE_JSON)
            val jsonEstado = gson.toJson(estadoJuego)

            FileWriter(file).use { writer ->
                writer.write(jsonEstado)
            }

            Log.d(TAG, "Partida guardada en: ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando partida en almacenamiento externo: ${e.message}")
            return false
        }
    }

    /**
     * Carga una partida desde el almacenamiento externo
     */
    fun cargarPartida(): EstadoPartida? {
        try {
            // Verificar que el almacenamiento externo está disponible
            if (!isExternalStorageReadable()) {
                Log.e(TAG, "Almacenamiento externo no disponible para lectura")
                return null
            }

            // Encontrar el archivo
            val directory = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), SAVE_DIRECTORY)
            val file = File(directory, SAVE_FILE_JSON)

            if (!file.exists()) {
                Log.d(TAG, "No existe archivo de guardado en: ${file.absolutePath}")
                return null
            }

            // Leer el archivo JSON
            val jsonEstado = FileReader(file).use { reader ->
                reader.readText()
            }

            return gson.fromJson(jsonEstado, EstadoPartida::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando partida desde almacenamiento externo: ${e.message}")
            return null
        }
    }

    /**
     * Verifica si el almacenamiento externo está disponible para escritura
     */
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Verifica si el almacenamiento externo está disponible al menos para lectura
     */
    private fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }
}