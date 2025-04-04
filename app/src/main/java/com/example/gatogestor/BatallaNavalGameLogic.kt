package com.example.batallanavalgame

class BatallaNavalGameLogic(
    private val TABLERO_SIZE: Int,
    private val BARCOS: List<Barco>
) {

    // Comprobar si una posición es válida para colocar un barco
    fun comprobarPosicionValida(
        fila: Int,
        columna: Int,
        longitud: Int,
        horizontal: Boolean,
        tablero: Array<Array<EstadoCelda>>
    ): Boolean {
        // Comprobar límites del tablero
        if (horizontal) {
            if (columna + longitud > TABLERO_SIZE) return false
        } else {
            if (fila + longitud > TABLERO_SIZE) return false
        }

        // Comprobar que no hay otros barcos en la posición
        for (i in 0 until longitud) {
            val nuevaFila = if (horizontal) fila else fila + i
            val nuevaColumna = if (horizontal) columna + i else columna

            if (tablero[nuevaFila][nuevaColumna] != EstadoCelda.VACIA) {
                return false
            }
        }
        return true
    }

    // Comprobar si una celda es parte de la vista previa
    fun esParteDeLaVistaPrevia(
        fila: Int,
        columna: Int,
        previewFila: Int,
        previewColumna: Int,
        longitud: Int,
        horizontal: Boolean
    ): Boolean {
        if (horizontal) {
            return fila == previewFila && columna >= previewColumna && columna < previewColumna + longitud
        } else {
            return columna == previewColumna && fila >= previewFila && fila < previewFila + longitud
        }
    }

    // Obtener el barco por índice
    fun getBarco(index: Int): Barco? {
        return if (index < BARCOS.size) BARCOS[index] else null
    }

    // Procesar un ataque y devolver si hubo impacto
    fun procesarAtaque(
        fila: Int,
        columna: Int,
        tableroOponente: Array<Array<EstadoCelda>>,
        tableroAtaques: Array<Array<Boolean>>,
        barcosOponente: List<BarcoColocado>
    ): AtaqueResultado {
        // Verificar si esa celda ya fue atacada
        if (tableroAtaques[fila][columna]) {
            return AtaqueResultado.YA_ATACADO
        }

        // Registrar ataque
        tableroAtaques[fila][columna] = true

        // Comprobar resultado
        val impacto = tableroOponente[fila][columna] == EstadoCelda.BARCO

        if (!impacto) {
            return AtaqueResultado.AGUA
        }

        // Comprobar si el barco ha sido hundido
        val barcoImpactado = barcosOponente.find { barco ->
            barco.posiciones.any { it.first == fila && it.second == columna }
        }

        if (barcoImpactado != null) {
            val todasPosicionesAtacadas = barcoImpactado.posiciones.all { posicion ->
                tableroAtaques[posicion.first][posicion.second]
            }

            if (todasPosicionesAtacadas) {
                // Comprobar victoria
                val todosLosBarcosHundidos = barcosOponente.all { barco ->
                    barco.posiciones.all { posicion ->
                        tableroAtaques[posicion.first][posicion.second]
                    }
                }

                return if (todosLosBarcosHundidos) {
                    AtaqueResultado.HUNDIDO_VICTORIA
                } else {
                    AtaqueResultado.HUNDIDO
                }
            }
        }

        return AtaqueResultado.IMPACTO
    }

    enum class AtaqueResultado {
        YA_ATACADO, AGUA, IMPACTO, HUNDIDO, HUNDIDO_VICTORIA
    }
}