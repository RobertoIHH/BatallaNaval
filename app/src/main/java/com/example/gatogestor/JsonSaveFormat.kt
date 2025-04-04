package com.example.batallanavalgame

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type
import com.example.batallanavalgame.BatallaNavalActivity.*

/**
 * Implementa el guardado y carga de partidas en formato JSON
 */
class JsonSaveFormat(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            Array<Array<EstadoCelda>>::class.java,
            ArraysTypeAdapter<EstadoCelda>()
        )
        .registerTypeAdapter(
            Array<Array<Boolean>>::class.java,
            ArraysTypeAdapter<Boolean>()
        )
        .create()

    companion object {
        private const val TAG = "JsonSaveFormat"
    }

    /**
     * Guarda una partida en formato JSON
     */
    fun guardarPartidaJSON(estadoJuego: EstadoPartida) {
        val jsonEstado = gson.toJson(estadoJuego)

        // Guardar en archivo
        val file = File(context.filesDir, SaveGameManager.SAVE_FILE_JSON)
        FileWriter(file).use { writer ->
            writer.write(jsonEstado)
        }
    }

    /**
     * Carga una partida desde formato JSON
     */
    fun cargarPartidaJSON(): EstadoPartida? {
        val file = File(context.filesDir, SaveGameManager.SAVE_FILE_JSON)
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
            File(context.filesDir, SaveGameManager.SAVE_FILE_JSON).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar archivo de guardado JSON", e)
        }
    }

    // TypeAdapter personalizado para manejar Arrays bidimensionales en GSON
    class ArraysTypeAdapter<T> : com.google.gson.JsonSerializer<Array<Array<T>>>,
        com.google.gson.JsonDeserializer<Array<Array<T>>> {

        override fun serialize(
            src: Array<Array<T>>,
            typeOfSrc: Type,
            context: com.google.gson.JsonSerializationContext
        ): com.google.gson.JsonElement {
            val jsonArray = com.google.gson.JsonArray()

            for (array in src) {
                val innerArray = com.google.gson.JsonArray()
                for (element in array) {
                    innerArray.add(context.serialize(element))
                }
                jsonArray.add(innerArray)
            }

            return jsonArray
        }

        @Suppress("UNCHECKED_CAST")
        override fun deserialize(
            json: com.google.gson.JsonElement,
            typeOfT: Type,
            context: com.google.gson.JsonDeserializationContext
        ): Array<Array<T>> {
            val jsonArray = json.asJsonArray
            val outerArray = ArrayList<Array<T>>()

            for (innerJson in jsonArray) {
                val innerJsonArray = innerJson.asJsonArray
                val innerArray = java.lang.reflect.Array.newInstance(
                    getElementType(typeOfT),
                    innerJsonArray.size()
                ) as Array<T>

                for (i in 0 until innerJsonArray.size()) {
                    val element = context.deserialize<T>(
                        innerJsonArray.get(i),
                        getElementType(typeOfT)
                    )
                    innerArray[i] = element
                }

                outerArray.add(innerArray)
            }

            return java.lang.reflect.Array.newInstance(
                getArrayType(typeOfT),
                outerArray.size
            ).also { array ->
                for (i in outerArray.indices) {
                    java.lang.reflect.Array.set(array, i, outerArray[i])
                }
            } as Array<Array<T>>
        }

        private fun getElementType(type: Type): Class<*> {
            // Extraer el tipo de elemento del array bidimensional
            val elementType = (type as java.lang.reflect.ParameterizedType)
                .actualTypeArguments[0] as java.lang.reflect.ParameterizedType

            val className = elementType.actualTypeArguments[0].toString()

            return when {
                className.contains("EstadoCelda") -> EstadoCelda::class.java
                className.contains("Boolean") -> Boolean::class.java
                else -> Class.forName(className.replace("class ", ""))
            }
        }

        private fun getArrayType(type: Type): Class<*> {
            // Obtener el tipo del array
            val elementType = getElementType(type)
            return java.lang.reflect.Array.newInstance(elementType, 0).javaClass
        }
    }
}