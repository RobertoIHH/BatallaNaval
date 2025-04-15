package com.example.batallanavalgame

import java.io.Serializable

/**
 * Representa una partida guardada de Batalla Naval.
 * Contiene toda la información necesaria para restaurar el estado del juego.
 */
data class PartidaGuardada(
    var fecha: Long = System.currentTimeMillis(),
    var dificultad: String = "NORMAL",
    var barcos: MutableMap<String, DatosBarco> = mutableMapOf(),
    var tableroJugador: Array<Array<Int>> = Array(10) { Array(10) { 0 } },
    var tableroOponente: Array<Array<Int>> = Array(10) { Array(10) { 0 } }
) : Serializable {
    // Método para garantizar la igualdad correcta en arrays
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PartidaGuardada

        if (fecha != other.fecha) return false
        if (dificultad != other.dificultad) return false
        if (barcos != other.barcos) return false
        if (!tableroJugador.contentDeepEquals(other.tableroJugador)) return false
        if (!tableroOponente.contentDeepEquals(other.tableroOponente)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fecha.hashCode()
        result = 31 * result + dificultad.hashCode()
        result = 31 * result + barcos.hashCode()
        result = 31 * result + tableroJugador.contentDeepHashCode()
        result = 31 * result + tableroOponente.contentDeepHashCode()
        return result
    }
}

/**
 * Datos de un barco colocado en el tablero
 */
data class DatosBarco(
    var fila: Int = 0,
    var columna: Int = 0,
    var esHorizontal: Boolean = true,
    var tamaño: Int = 0
) : Serializable