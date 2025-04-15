package com.example.batallanavalgame

import android.content.Context
import android.util.Log
import android.util.Xml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementa formatos alternativos de guardado: XML y TXT
 */
class AlternativeSaveFormats(private val context: Context) {

    // Usar la instancia de Gson de SaveGameUtils
    private val gson: Gson = SaveGameUtils.createGson()

    companion object {
        private const val TAG = "AlternativeSaveFormats"
    }

    /**
     * Guarda una partida en formato XML
     */
    fun guardarPartidaXML(estadoJuego: EstadoPartida) {
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_XML)
        val fileOutputStream = FileOutputStream(file)

        val serializer = Xml.newSerializer()
        serializer.setOutput(fileOutputStream, "UTF-8")
        serializer.startDocument("UTF-8", true)
        serializer.startTag("", "BatallaNavalSave")

        // Escribir metadatos
        serializer.startTag("", "Metadata")
        serializer.startTag("", "SaveDate")
        serializer.text(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        serializer.endTag("", "SaveDate")
        serializer.startTag("", "Version")
        serializer.text("1.0")
        serializer.endTag("", "Version")
        serializer.endTag("", "Metadata")

        // Escribir información básica del estado
        serializer.startTag("", "GameState")
        serializer.startTag("", "Phase")
        serializer.text(estadoJuego.faseActual.name)
        serializer.endTag("", "Phase")
        serializer.startTag("", "CurrentPlayer")
        serializer.text(estadoJuego.jugadorActual.toString())
        serializer.endTag("", "CurrentPlayer")
        serializer.startTag("", "ShipIndex")
        serializer.text(estadoJuego.barcoActualIndex.toString())
        serializer.endTag("", "ShipIndex")
        serializer.startTag("", "Orientation")
        serializer.text(estadoJuego.orientacionHorizontal.toString())
        serializer.endTag("", "Orientation")
        serializer.startTag("", "Player1Name")
        serializer.text(estadoJuego.nombreJugador1)
        serializer.endTag("", "Player1Name")
        serializer.startTag("", "Player2Name")
        serializer.text(estadoJuego.nombreJugador2)
        serializer.endTag("", "Player2Name")
        serializer.startTag("", "Player1Score")
        serializer.text(estadoJuego.puntajeJugador1.toString())
        serializer.endTag("", "Player1Score")
        serializer.startTag("", "Player2Score")
        serializer.text(estadoJuego.puntajeJugador2.toString())
        serializer.endTag("", "Player2Score")
        serializer.startTag("", "GameTimeSeconds")
        serializer.text(estadoJuego.tiempoJuegoSegundos.toString())
        serializer.endTag("", "GameTimeSeconds")
        serializer.endTag("", "GameState")

        // Convertir datos complejos a JSON para simplificar XML
        val boardsJson = gson.toJson(mapOf(
            "tableroJugador1" to estadoJuego.tableroJugador1,
            "tableroJugador2" to estadoJuego.tableroJugador2,
            "tableroAtaquesJugador1" to estadoJuego.tableroAtaquesJugador1,
            "tableroAtaquesJugador2" to estadoJuego.tableroAtaquesJugador2
        ))

        val barcosJson = gson.toJson(mapOf(
            "barcosJugador1" to estadoJuego.barcosJugador1,
            "barcosJugador2" to estadoJuego.barcosJugador2
        ))

        val movimientosJson = gson.toJson(estadoJuego.historialMovimientos)

        // Escribir datos complejos como CDATA
        serializer.startTag("", "ComplexData")
        serializer.startTag("", "Boards")
        serializer.cdsect(boardsJson)
        serializer.endTag("", "Boards")
        serializer.startTag("", "Ships")
        serializer.cdsect(barcosJson)
        serializer.endTag("", "Ships")
        serializer.startTag("", "MoveHistory")
        serializer.cdsect(movimientosJson)
        serializer.endTag("", "MoveHistory")
        serializer.endTag("", "ComplexData")

        serializer.endTag("", "BatallaNavalSave")
        serializer.endDocument()

        fileOutputStream.close()
    }

    /**
     * Carga una partida desde formato XML
     */
    fun cargarPartidaXML(): EstadoPartida? {
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_XML)
        if (!file.exists()) return null

        val fileInputStream = FileInputStream(file)
        val parser = Xml.newPullParser()
        parser.setInput(fileInputStream, null)

        var eventType = parser.eventType

        // Valores base
        var faseActual = FaseJuego.CONFIGURACION
        var jugadorActual = 1
        var barcoActualIndex = 0
        var orientacionHorizontal = true
        var nombreJugador1 = "Jugador 1"
        var nombreJugador2 = "Jugador 2"
        var puntajeJugador1 = 0
        var puntajeJugador2 = 0
        var tiempoJuegoSegundos = 0L

        // Para datos complejos
        var boardsJson = ""
        var shipsJson = ""
        var movesJson = ""

        // Para seguimiento de dónde estamos en el XML
        var insideGameState = false
        var insideComplexData = false
        var currentTag = ""
        var currentComplexTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "GameState" -> insideGameState = true
                        "ComplexData" -> insideComplexData = true
                        else -> {
                            if (insideGameState) currentTag = parser.name
                            if (insideComplexData) currentComplexTag = parser.name
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideGameState && !parser.isWhitespace) {
                        when (currentTag) {
                            "Phase" -> faseActual = FaseJuego.valueOf(parser.text)
                            "CurrentPlayer" -> jugadorActual = parser.text.toInt()
                            "ShipIndex" -> barcoActualIndex = parser.text.toInt()
                            "Orientation" -> orientacionHorizontal = parser.text.toBoolean()
                            "Player1Name" -> nombreJugador1 = parser.text
                            "Player2Name" -> nombreJugador2 = parser.text
                            "Player1Score" -> puntajeJugador1 = parser.text.toInt()
                            "Player2Score" -> puntajeJugador2 = parser.text.toInt()
                            "GameTimeSeconds" -> tiempoJuegoSegundos = parser.text.toLong()
                        }
                    }
                }
                XmlPullParser.CDSECT -> {
                    if (insideComplexData) {
                        when (currentComplexTag) {
                            "Boards" -> boardsJson = parser.text
                            "Ships" -> shipsJson = parser.text
                            "MoveHistory" -> movesJson = parser.text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "GameState" -> insideGameState = false
                        "ComplexData" -> insideComplexData = false
                    }
                }
            }
            eventType = parser.next()
        }

        fileInputStream.close()

        // Procesar datos complejos desde JSON
        try {
            // Tableros
            val boardsMapType = object : TypeToken<Map<String, Any>>() {}.type
            val boardsMap = gson.fromJson<Map<String, Any>>(boardsJson, boardsMapType)

            // Extraer tableros
            val tableroJugador1 = extraerTablero(boardsMap["tableroJugador1"])
            val tableroJugador2 = extraerTablero(boardsMap["tableroJugador2"])
            val tableroAtaquesJugador1 = extraerTableroAtaques(boardsMap["tableroAtaquesJugador1"])
            val tableroAtaquesJugador2 = extraerTableroAtaques(boardsMap["tableroAtaquesJugador2"])

            // Barcos
            val shipsMapType = object : TypeToken<Map<String, List<BarcoColocado>>>() {}.type
            val shipsMap = gson.fromJson<Map<String, List<BarcoColocado>>>(shipsJson, shipsMapType)

            val barcosJugador1 = shipsMap["barcosJugador1"] ?: listOf()
            val barcosJugador2 = shipsMap["barcosJugador2"] ?: listOf()

            // Movimientos
            val movesType = object : TypeToken<List<Movimiento>>() {}.type
            val historialMovimientos = gson.fromJson<List<Movimiento>>(movesJson, movesType) ?: listOf()

            // Crear objeto de estado
            return EstadoPartida(
                faseActual,
                jugadorActual,
                barcoActualIndex,
                orientacionHorizontal,
                tableroJugador1 ?: Array(10) { Array(10) { EstadoCelda.VACIA } },
                tableroJugador2 ?: Array(10) { Array(10) { EstadoCelda.VACIA } },
                tableroAtaquesJugador1 ?: Array(10) { Array(10) { false } },
                tableroAtaquesJugador2 ?: Array(10) { Array(10) { false } },
                barcosJugador1,
                barcosJugador2,
                nombreJugador1,
                nombreJugador2,
                puntajeJugador1,
                puntajeJugador2,
                tiempoJuegoSegundos,
                historialMovimientos
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos complejos de XML", e)
            return null
        }
    }

    // Métodos auxiliares para extraer tableros de los mapas
    private fun extraerTablero(data: Any?): Array<Array<EstadoCelda>>? {
        if (data == null) return null

        try {
            // Si ya es del tipo correcto, devolverlo directamente
            if (data is Array<*> && data.isArrayOf<Array<EstadoCelda>>()) {
                return data as Array<Array<EstadoCelda>>
            }

            // Convertir de vuelta a JSON y utilizar Gson para la deserialización
            val json = gson.toJson(data)
            val type = object : TypeToken<Array<Array<EstadoCelda>>>() {}.type
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error extraendo tablero: ${e.message}", e)
            return null
        }
    }

    private fun extraerTableroAtaques(data: Any?): Array<Array<Boolean>>? {
        if (data == null) return null

        try {
            // Si ya es del tipo correcto, devolverlo directamente
            if (data is Array<*> && data.isArrayOf<Array<Boolean>>()) {
                return data as Array<Array<Boolean>>
            }

            // Procesar manualmente si es una lista o un array
            if (data is List<*>) {
                val rows = data.size
                val cols = if (rows > 0 && data[0] is List<*>) (data[0] as List<*>).size else 0

                return Array(rows) { i ->
                    Array(cols) { j ->
                        if (data[i] is List<*>) {
                            (data[i] as List<*>).getOrNull(j)?.toString()?.toBoolean() ?: false
                        } else {
                            false
                        }
                    }
                }
            }

            // Convertir de vuelta a JSON y utilizar Gson
            val json = gson.toJson(data)
            val type = object : TypeToken<Array<Array<Boolean>>>() {}.type
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error extraendo tablero de ataques: ${e.message}", e)
            // Retornar un array vacío en caso de error
            return Array(10) { Array(10) { false } }
        }
    }

    /**
     * Guarda una partida en formato TXT
     */
    fun guardarPartidaTXT(estadoJuego: EstadoPartida) {
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_TEXT)
        val writer = FileWriter(file)
        val buffer = BufferedWriter(writer)

        try {
            // Metadatos
            buffer.write("# BATALLA NAVAL - PARTIDA GUARDADA")
            buffer.newLine()
            buffer.write("# Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            buffer.newLine()
            buffer.write("# Versión: 1.0")
            buffer.newLine()
            buffer.newLine()

            // Estado del juego
            buffer.write("[ESTADO_JUEGO]")
            buffer.newLine()
            buffer.write("Fase=${estadoJuego.faseActual.name}")
            buffer.newLine()
            buffer.write("JugadorActual=${estadoJuego.jugadorActual}")
            buffer.newLine()
            buffer.write("BarcoActualIndex=${estadoJuego.barcoActualIndex}")
            buffer.newLine()
            buffer.write("OrientacionHorizontal=${estadoJuego.orientacionHorizontal}")
            buffer.newLine()
            buffer.newLine()

            // Información de jugadores
            buffer.write("[JUGADORES]")
            buffer.newLine()
            buffer.write("NombreJugador1=${estadoJuego.nombreJugador1}")
            buffer.newLine()
            buffer.write("NombreJugador2=${estadoJuego.nombreJugador2}")
            buffer.newLine()
            buffer.write("PuntajeJugador1=${estadoJuego.puntajeJugador1}")
            buffer.newLine()
            buffer.write("PuntajeJugador2=${estadoJuego.puntajeJugador2}")
            buffer.newLine()
            buffer.write("TiempoJuego=${estadoJuego.tiempoJuegoSegundos}")
            buffer.newLine()
            buffer.newLine()

            // Para tableros y datos complejos, usar una representación simplificada
            buffer.write("[TABLEROS]")
            buffer.newLine()
            // Los datos complejos se almacenan como JSON ya que es difícil serializarlos manualmente
            buffer.write("TableroDatos=${gson.toJson(mapOf(
                "tableroJugador1" to estadoJuego.tableroJugador1,
                "tableroJugador2" to estadoJuego.tableroJugador2,
                "tableroAtaquesJugador1" to estadoJuego.tableroAtaquesJugador1,
                "tableroAtaquesJugador2" to estadoJuego.tableroAtaquesJugador2
            ))}")
            buffer.newLine()
            buffer.newLine()

            // Barcos
            buffer.write("[BARCOS]")
            buffer.newLine()
            buffer.write("BarcosDatos=${gson.toJson(mapOf(
                "barcosJugador1" to estadoJuego.barcosJugador1,
                "barcosJugador2" to estadoJuego.barcosJugador2
            ))}")
            buffer.newLine()
            buffer.newLine()

            // Historial de movimientos
            buffer.write("[MOVIMIENTOS]")
            buffer.newLine()
            buffer.write("HistorialMovimientos=${gson.toJson(estadoJuego.historialMovimientos)}")
            buffer.newLine()

            buffer.flush()
        } finally {
            buffer.close()
        }
    }

    /**
     * Carga una partida desde formato TXT
     */
    fun cargarPartidaTXT(): EstadoPartida? {
        val file = File(context.filesDir, SaveGameUtils.SAVE_FILE_TEXT)
        if (!file.exists()) return null

        val reader = BufferedReader(FileReader(file))

        // Variables para almacenar los datos
        var fase = FaseJuego.CONFIGURACION
        var jugadorActual = 1
        var barcoActualIndex = 0
        var orientacionHorizontal = true
        var nombreJugador1 = "Jugador 1"
        var nombreJugador2 = "Jugador 2"
        var puntajeJugador1 = 0
        var puntajeJugador2 = 0
        var tiempoJuegoSegundos = 0L

        // Datos complejos
        var tablerosDatos = ""
        var barcosDatos = ""
        var movimientosDatos = ""

        var seccionActual = ""

        try {
            reader.forEachLine { linea ->
                // Ignorar comentarios y líneas vacías
                if (linea.startsWith("#") || linea.isEmpty()) {
                    return@forEachLine
                }

                // Detectar cabeceras de sección
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    seccionActual = linea
                    return@forEachLine
                }

                // Procesar según la sección
                when (seccionActual) {
                    "[ESTADO_JUEGO]" -> {
                        val partes = linea.split("=", limit = 2)
                        if (partes.size == 2) {
                            when (partes[0]) {
                                "Fase" -> fase = FaseJuego.valueOf(partes[1])
                                "JugadorActual" -> jugadorActual = partes[1].toInt()
                                "BarcoActualIndex" -> barcoActualIndex = partes[1].toInt()
                                "OrientacionHorizontal" -> orientacionHorizontal = partes[1].toBoolean()
                            }
                        }
                    }

                    "[JUGADORES]" -> {
                        val partes = linea.split("=", limit = 2)
                        if (partes.size == 2) {
                            when (partes[0]) {
                                "NombreJugador1" -> nombreJugador1 = partes[1]
                                "NombreJugador2" -> nombreJugador2 = partes[1]
                                "PuntajeJugador1" -> puntajeJugador1 = partes[1].toInt()
                                "PuntajeJugador2" -> puntajeJugador2 = partes[1].toInt()
                                "TiempoJuego" -> tiempoJuegoSegundos = partes[1].toLong()
                            }
                        }
                    }

                    "[TABLEROS]" -> {
                        val partes = linea.split("=", limit = 2)
                        if (partes.size == 2 && partes[0] == "TableroDatos") {
                            tablerosDatos = partes[1]
                        }
                    }

                    "[BARCOS]" -> {
                        val partes = linea.split("=", limit = 2)
                        if (partes.size == 2 && partes[0] == "BarcosDatos") {
                            barcosDatos = partes[1]
                        }
                    }

                    "[MOVIMIENTOS]" -> {
                        val partes = linea.split("=", limit = 2)
                        if (partes.size == 2 && partes[0] == "HistorialMovimientos") {
                            movimientosDatos = partes[1]
                        }
                    }
                }
            }

            // Procesar los datos complejos
            try {
                // Tableros
                val boardsMapType = object : TypeToken<Map<String, Any>>() {}.type
                val boardsMap = gson.fromJson<Map<String, Any>>(tablerosDatos, boardsMapType)

                // Extraer tableros
                val tableroJugador1 = extraerTablero(boardsMap["tableroJugador1"])
                val tableroJugador2 = extraerTablero(boardsMap["tableroJugador2"])
                val tableroAtaquesJugador1 = extraerTableroAtaques(boardsMap["tableroAtaquesJugador1"])
                val tableroAtaquesJugador2 = extraerTableroAtaques(boardsMap["tableroAtaquesJugador2"])

                // Barcos
                val shipsMapType = object : TypeToken<Map<String, List<BarcoColocado>>>() {}.type
                val shipsMap = gson.fromJson<Map<String, List<BarcoColocado>>>(barcosDatos, shipsMapType)

                val barcosJugador1 = shipsMap["barcosJugador1"] ?: listOf()
                val barcosJugador2 = shipsMap["barcosJugador2"] ?: listOf()

                // Movimientos
                val movesType = object : TypeToken<List<Movimiento>>() {}.type
                val historialMovimientos = gson.fromJson<List<Movimiento>>(movimientosDatos, movesType) ?: listOf()

                return EstadoPartida(
                    fase,
                    jugadorActual,
                    barcoActualIndex,
                    orientacionHorizontal,
                    tableroJugador1 ?: Array(10) { Array(10) { EstadoCelda.VACIA } },
                    tableroJugador2 ?: Array(10) { Array(10) { EstadoCelda.VACIA } },
                    tableroAtaquesJugador1 ?: Array(10) { Array(10) { false } },
                    tableroAtaquesJugador2 ?: Array(10) { Array(10) { false } },
                    barcosJugador1,
                    barcosJugador2,
                    nombreJugador1,
                    nombreJugador2,
                    puntajeJugador1,
                    puntajeJugador2,
                    tiempoJuegoSegundos,
                    historialMovimientos
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando datos complejos desde TXT", e)
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al leer archivo TXT", e)
            return null
        } finally {
            reader.close()
        }
    }

    /**
     * Borra los archivos de guardado de formatos alternativos
     */
    fun borrarArchivosGuardados() {
        try {
            File(context.filesDir, SaveGameUtils.SAVE_FILE_XML).delete()
            File(context.filesDir, SaveGameUtils.SAVE_FILE_TEXT).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar archivos de guardado", e)
        }
    }
}