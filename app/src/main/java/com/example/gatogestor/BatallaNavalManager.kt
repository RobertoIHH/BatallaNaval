private fun cargarPartidaXML(): EstadoPartida? {
    val file = File(context.filesDir, SAVE_FILE_XML)
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

        // Crear y devolver objeto de estado
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

private fun cargarPartidaTXT(): EstadoPartida? {
    val file = File(context.filesDir, SAVE_FILE_TEXT)
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