package com.example.batallanavalgame

/**
 * Clase que sirve como fachada para gestionar las operaciones de guardado y carga
 * del juego. Simplifica el acceso a los diferentes formatos de guardado.
 */
class BatallaNavalManager {

    // Esta clase es solo un adaptador que utiliza SaveGameManager para implementar
    // sus operaciones. Se mantiene por compatibilidad con el código existente.

    private val saveGameManager: SaveGameManager

    constructor(context: android.content.Context) {
        saveGameManager = SaveGameManager(context)
    }

    /**
     * Guarda una partida en el formato de guardado seleccionado por el usuario
     */
    fun guardarPartida(estadoPartida: EstadoPartida, vista: android.view.View? = null) {
        saveGameManager.guardarPartida(estadoPartida, vista = vista)
    }

    /**
     * Carga una partida desde cualquier formato disponible
     */
    fun cargarPartida(): EstadoPartida? {
        return saveGameManager.cargarPartida()
    }

    /**
     * Obtiene el formato de guardado seleccionado por el usuario
     */
    fun getSavedFormat(): SaveGameManager.SaveFormat {
        return saveGameManager.getSavedFormat()
    }

    /**
     * Guarda el formato de guardado seleccionado por el usuario
     */
    fun guardarFormatoSeleccionado(formato: SaveGameManager.SaveFormat) {
        saveGameManager.guardarFormatoSeleccionado(formato)
    }

    /**
     * Registra una victoria para el jugador indicado
     */
    fun registrarVictoria(jugador: Int) {
        saveGameManager.registrarVictoria(jugador)
    }

    /**
     * Obtiene el número de victorias del jugador indicado
     */
    fun getVictoriasJugador(jugador: Int): Int {
        return saveGameManager.getVictoriasJugador(jugador)
    }

    /**
     * Obtiene el número total de partidas jugadas
     */
    fun getPartidasJugadas(): Int {
        return saveGameManager.getPartidasJugadas()
    }

    /**
     * Resetea todas las estadísticas de juego
     */
    fun resetearEstadisticas() {
        saveGameManager.resetearEstadisticas()
    }

    /**
     * Borra los archivos de partidas guardadas
     */
    fun borrarPartidaGuardada() {
        saveGameManager.borrarPartidaGuardada()
    }
}