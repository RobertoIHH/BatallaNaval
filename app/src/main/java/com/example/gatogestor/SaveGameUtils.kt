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