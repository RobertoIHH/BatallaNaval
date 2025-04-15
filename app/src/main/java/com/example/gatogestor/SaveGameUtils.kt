package com.example.batallanavalgame

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import java.lang.reflect.Type
import android.util.Log
import java.lang.reflect.ParameterizedType

/**
 * Utilidades comunes para todas las clases de guardado
 */
object SaveGameUtils {
    // Nombres de archivos para diferentes formatos
    const val SAVE_FILE_JSON = "batalla_naval_save.json"
    const val SAVE_FILE_XML = "batalla_naval_save.xml"
    const val SAVE_FILE_TEXT = "batalla_naval_save.txt"

    /**
     * Crea una instancia de Gson configurada para manejar arrays bidimensionales
     */
    fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(
                Array<Array<EstadoCelda>>::class.java,
                ArraysTypeAdapter<EstadoCelda>()
            )
            .registerTypeAdapter(
                Array<Array<Boolean>>::class.java,
                ArraysTypeAdapter<Boolean>()
            )
            .registerTypeAdapter(
                java.lang.reflect.Array.newInstance(java.lang.Boolean::class.java, 0, 0).javaClass,
                ArraysTypeAdapter<Boolean>()
            )
            .create()
    }

    /**
     * TypeAdapter para manejar Arrays bidimensionales en GSON
     */
    class ArraysTypeAdapter<T> : JsonSerializer<Array<Array<T>>>,
        JsonDeserializer<Array<Array<T>>> {

        override fun serialize(
            src: Array<Array<T>>,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonArray = JsonArray()

            for (array in src) {
                val innerArray = JsonArray()
                for (element in array) {
                    innerArray.add(context.serialize(element))
                }
                jsonArray.add(innerArray)
            }

            return jsonArray
        }

        @Suppress("UNCHECKED_CAST")
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Array<Array<T>> {
            try {
                val jsonArray = json.asJsonArray
                val rows = jsonArray.size()
                val cols = if (rows > 0) jsonArray.get(0).asJsonArray.size() else 0

                // Determinar el tipo del elemento
                val elementClass = getElementClass(typeOfT)

                // Crear un array "raw" que luego convertiremos
                val result = java.lang.reflect.Array.newInstance(
                    elementClass, rows, cols
                ) as Array<*>

                // Llenar el array con los valores
                for (i in 0 until rows) {
                    val innerArray = jsonArray.get(i).asJsonArray
                    for (j in 0 until innerArray.size()) {
                        try {
                            val element = context.deserialize<Any>(innerArray.get(j), elementClass)
                            java.lang.reflect.Array.set(
                                java.lang.reflect.Array.get(result, i),
                                j,
                                element
                            )
                        } catch (e: Exception) {
                            Log.e("SaveGameUtils", "Error deserializando elemento: ${e.message}")
                            // Usar valor por defecto
                            java.lang.reflect.Array.set(
                                java.lang.reflect.Array.get(result, i),
                                j,
                                getDefaultValue(elementClass)
                            )
                        }
                    }
                }

                return result as Array<Array<T>>
            } catch (e: Exception) {
                Log.e("SaveGameUtils", "Error completo deserializando: ${e.message}", e)
                return createEmptyArray(typeOfT) as Array<Array<T>>
            }
        }

        private fun getDefaultValue(clazz: Class<*>): Any? {
            return when (clazz) {
                EstadoCelda::class.java -> EstadoCelda.VACIA
                Boolean::class.java, java.lang.Boolean::class.java -> false
                String::class.java -> ""
                Int::class.java, java.lang.Integer::class.java -> 0
                Long::class.java, java.lang.Long::class.java -> 0L
                else -> null
            }
        }

        private fun getElementClass(type: Type): Class<*> {
            try {
                // Intentar identificar el tipo de elemento basado en el nombre del tipo
                val typeName = type.toString()

                if (typeName.contains("EstadoCelda")) {
                    return EstadoCelda::class.java
                } else if (typeName.contains("Boolean")) {
                    return java.lang.Boolean::class.java
                }

                // Si el tipo es parametrizado, intentar extraer información
                if (type is ParameterizedType) {
                    // Intentar extraer el tipo de elemento
                    val argType = type.actualTypeArguments[0]
                    val argTypeName = argType.toString()

                    if (argTypeName.contains("EstadoCelda")) {
                        return EstadoCelda::class.java
                    } else if (argTypeName.contains("Boolean")) {
                        return java.lang.Boolean::class.java
                    }
                }

                // Por defecto, usar Object como tipo seguro
                return Any::class.java
            } catch (e: Exception) {
                Log.e("SaveGameUtils", "Error determinando tipo de elemento: ${e.message}")
                return Any::class.java
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun createEmptyArray(type: Type): Array<Array<Any?>> {
            val elementClass = getElementClass(type)
            val emptyArray = java.lang.reflect.Array.newInstance(elementClass, 2, 2)
            return emptyArray as Array<Array<Any?>>
        }
    }
}