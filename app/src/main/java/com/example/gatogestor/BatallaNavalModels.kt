package com.example.batallanavalgame

// Enums y clases de datos
enum class FaseJuego {
    CONFIGURACION, ATAQUE
}

enum class EstadoCelda {
    VACIA, BARCO, AGUA, IMPACTO
}

data class Barco(val longitud: Int, val nombre: String)

data class BarcoColocado(val longitud: Int, val posiciones: List<Pair<Int, Int>>)

data class Movimiento(
    val jugador: Int,
    val fila: Int,
    val columna: Int,
    val resultado: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Clase para representar el estado completo de la partida
data class EstadoPartida(
    val faseActual: FaseJuego,
    val jugadorActual: Int,
    val barcoActualIndex: Int,
    val orientacionHorizontal: Boolean,
    val tableroJugador1: Array<Array<EstadoCelda>>,
    val tableroJugador2: Array<Array<EstadoCelda>>,
    val tableroAtaquesJugador1: Array<Array<Boolean>>,
    val tableroAtaquesJugador2: Array<Array<Boolean>>,
    val barcosJugador1: List<BarcoColocado>,
    val barcosJugador2: List<BarcoColocado>,
    val nombreJugador1: String,
    val nombreJugador2: String,
    val puntajeJugador1: Int,
    val puntajeJugador2: Int,
    val tiempoJuegoSegundos: Long,
    val historialMovimientos: List<Movimiento>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EstadoPartida

        if (faseActual != other.faseActual) return false
        if (jugadorActual != other.jugadorActual) return false
        if (barcoActualIndex != other.barcoActualIndex) return false
        if (orientacionHorizontal != other.orientacionHorizontal) return false
        if (!tableroJugador1.contentDeepEquals(other.tableroJugador1)) return false
        if (!tableroJugador2.contentDeepEquals(other.tableroJugador2)) return false
        if (!tableroAtaquesJugador1.contentDeepEquals(other.tableroAtaquesJugador1)) return false
        if (!tableroAtaquesJugador2.contentDeepEquals(other.tableroAtaquesJugador2)) return false
        if (barcosJugador1 != other.barcosJugador1) return false
        if (barcosJugador2 != other.barcosJugador2) return false
        if (nombreJugador1 != other.nombreJugador1) return false
        if (nombreJugador2 != other.nombreJugador2) return false
        if (puntajeJugador1 != other.puntajeJugador1) return false
        if (puntajeJugador2 != other.puntajeJugador2) return false
        if (tiempoJuegoSegundos != other.tiempoJuegoSegundos) return false
        if (historialMovimientos != other.historialMovimientos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = faseActual.hashCode()
        result = 31 * result + jugadorActual
        result = 31 * result + barcoActualIndex
        result = 31 * result + orientacionHorizontal.hashCode()
        result = 31 * result + tableroJugador1.contentDeepHashCode()
        result = 31 * result + tableroJugador2.contentDeepHashCode()
        result = 31 * result + tableroAtaquesJugador1.contentDeepHashCode()
        result = 31 * result + tableroAtaquesJugador2.contentDeepHashCode()
        result = 31 * result + barcosJugador1.hashCode()
        result = 31 * result + barcosJugador2.hashCode()
        result = 31 * result + nombreJugador1.hashCode()
        result = 31 * result + nombreJugador2.hashCode()
        result = 31 * result + puntajeJugador1
        result = 31 * result + puntajeJugador2
        result = 31 * result + tiempoJuegoSegundos.hashCode()
        result = 31 * result + historialMovimientos.hashCode()
        return result
    }
}