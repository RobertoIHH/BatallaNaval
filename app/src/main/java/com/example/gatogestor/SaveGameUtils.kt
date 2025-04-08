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
                val outerArray = ArrayList<Array<Any?>>()

                // Determinar el tipo de elemento
                val elementClass = getElementClass(typeOfT)

                for (innerJson in jsonArray) {
                    val innerJsonArray = innerJson.asJsonArray
                    val innerArray = java.lang.reflect.Array.newInstance(
                        elementClass,
                        innerJsonArray.size()
                    ) as Array<Any?>

                    for (i in 0 until innerJsonArray.size()) {
                        try {
                            innerArray[i] = context.deserialize<Any>(
                                innerJsonArray.get(i),
                                elementClass
                            )
                        } catch (e: Exception) {
                            Log.e("SaveGameUtils", "Error deserializando elemento: ${e.message}")
                            // Valor por defecto según el tipo
                            innerArray[i] = getDefaultValue(elementClass)
                        }
                    }

                    outerArray.add(innerArray)
                }

                // Crear array final
                val resultArray = java.lang.reflect.Array.newInstance(
                    getArrayClass(elementClass),
                    outerArray.size
                )

                for (i in outerArray.indices) {
                    java.lang.reflect.Array.set(resultArray, i, outerArray[i])
                }

                return resultArray as Array<Array<T>>
            } catch (e: Exception) {
                Log.e("SaveGameUtils", "Error completo deserializando: ${e.message}")
                return createEmptyArray(typeOfT) as Array<Array<T>>
            }
        }

        private fun getDefaultValue(clazz: Class<*>): Any? {
            return when (clazz) {
                EstadoCelda::class.java -> EstadoCelda.VACIA
                Boolean::class.java -> false
                String::class.java -> ""
                Int::class.java -> 0
                Long::class.java -> 0L
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
                    return Boolean::class.java
                }

                // Si el tipo es parametrizado, intentar extraer información
                if (type is ParameterizedType) {
                    // Intentar extraer el tipo de elemento
                    val argType = type.actualTypeArguments[0]
                    val argTypeName = argType.toString()

                    if (argTypeName.contains("EstadoCelda")) {
                        return EstadoCelda::class.java
                    } else if (argTypeName.contains("Boolean")) {
                        return Boolean::class.java
                    }
                }

                // Por defecto, usar Object como tipo seguro
                return Any::class.java
            } catch (e: Exception) {
                Log.e("SaveGameUtils", "Error determinando tipo de elemento: ${e.message}")
                return Any::class.java
            }
        }

        private fun getArrayClass(elementClass: Class<*>): Class<*> {
            return java.lang.reflect.Array.newInstance(elementClass, 0).javaClass
        }

        private fun createEmptyArray(type: Type): Array<Array<Any?>> {
            val elementClass = getElementClass(type)

            return Array(2) {
                Array<Any?>(2) { getDefaultValue(elementClass) }
            }
        }
    }
}