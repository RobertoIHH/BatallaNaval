package com.example.tictactoeapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Referencias a la vista
    private lateinit var tvTurno: TextView
    private lateinit var btnReiniciar: Button
    private lateinit var botones: Array<Array<Button>>

    // Variables de juego
    private var tablero = Array(3) { Array(3) { "" } }
    private var jugadorActual = "X"
    private var rondas = 0

    // Nombres de los jugadores
    private lateinit var nombreJugador1: String
    private lateinit var nombreJugador2: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener nombres de los jugadores
        nombreJugador1 = intent.getStringExtra("JUGADOR1") ?: getString(R.string.player1_hint)
        nombreJugador2 = intent.getStringExtra("JUGADOR2") ?: getString(R.string.player2_hint)

        // Inicializar vistas
        tvTurno = findViewById(R.id.tvTurno)
        btnReiniciar = findViewById(R.id.btnReiniciar)

        // Inicializar matriz de botones
        botones = Array(3) { Array(3) { Button(this) } }

        // Asignar los botones de la interfaz a la matriz
        botones[0][0] = findViewById(R.id.btn00)
        botones[0][1] = findViewById(R.id.btn01)
        botones[0][2] = findViewById(R.id.btn02)
        botones[1][0] = findViewById(R.id.btn10)
        botones[1][1] = findViewById(R.id.btn11)
        botones[1][2] = findViewById(R.id.btn12)
        botones[2][0] = findViewById(R.id.btn20)
        botones[2][1] = findViewById(R.id.btn21)
        botones[2][2] = findViewById(R.id.btn22)

        // Configurar los clicks de los botones
        for (i in 0..2) {
            for (j in 0..2) {
                botones[i][j].setOnClickListener {
                    hacerJugada(i, j)
                }
            }
        }

        // Configurar botón de reinicio
        btnReiniciar.setOnClickListener {
            reiniciarJuego()
        }

        // Establecer el texto inicial del turno
        actualizarTurno()
    }

    private fun hacerJugada(fila: Int, columna: Int) {
        // Si la celda ya está ocupada, no hacer nada
        if (tablero[fila][columna] != "") {
            return
        }

        // Actualizar el tablero y el botón
        tablero[fila][columna] = jugadorActual
        botones[fila][columna].text = jugadorActual

        // Cambiar el color del texto según el jugador
        if (jugadorActual == "X") {
            botones[fila][columna].setTextColor(ContextCompat.getColor(this, R.color.player_x))
        } else {
            botones[fila][columna].setTextColor(ContextCompat.getColor(this, R.color.player_o))
        }

        // Incrementar conteo de rondas
        rondas++

        // Verificar si hay un ganador
        if (verificarGanador()) {
            val ganador = if (jugadorActual == "X") nombreJugador1 else nombreJugador2
            mostrarMensajeGanador(getString(R.string.winner_format, ganador))
            return
        }

        // Verificar si hay empate
        if (rondas == 9) {
            mostrarMensajeGanador(getString(R.string.tie_game))
            return
        }

        // Cambiar al siguiente jugador
        jugadorActual = if (jugadorActual == "X") "O" else "X"

        // Actualizar texto del turno
        actualizarTurno()
    }

    private fun verificarGanador(): Boolean {
        // Verificar filas
        for (i in 0..2) {
            if (tablero[i][0] != "" && tablero[i][0] == tablero[i][1] && tablero[i][1] == tablero[i][2]) {
                // Resaltar la fila ganadora
                resaltarCeldasGanadoras(i, 0, i, 1, i, 2)
                return true
            }
        }

        // Verificar columnas
        for (i in 0..2) {
            if (tablero[0][i] != "" && tablero[0][i] == tablero[1][i] && tablero[1][i] == tablero[2][i]) {
                // Resaltar la columna ganadora
                resaltarCeldasGanadoras(0, i, 1, i, 2, i)
                return true
            }
        }

        // Verificar diagonales
        if (tablero[0][0] != "" && tablero[0][0] == tablero[1][1] && tablero[1][1] == tablero[2][2]) {
            // Resaltar la diagonal principal
            resaltarCeldasGanadoras(0, 0, 1, 1, 2, 2)
            return true
        }

        if (tablero[0][2] != "" && tablero[0][2] == tablero[1][1] && tablero[1][1] == tablero[2][0]) {
            // Resaltar la diagonal secundaria
            resaltarCeldasGanadoras(0, 2, 1, 1, 2, 0)
            return true
        }

        return false
    }

    private fun resaltarCeldasGanadoras(fila1: Int, col1: Int, fila2: Int, col2: Int, fila3: Int, col3: Int) {
        val colorGanador = ContextCompat.getColor(this, R.color.winner_background)
        botones[fila1][col1].setBackgroundColor(colorGanador)
        botones[fila2][col2].setBackgroundColor(colorGanador)
        botones[fila3][col3].setBackgroundColor(colorGanador)
    }

    private fun actualizarTurno() {
        val jugadorActualNombre = if (jugadorActual == "X") nombreJugador1 else nombreJugador2
        tvTurno.text = getString(R.string.turn_format, jugadorActualNombre, jugadorActual)
    }

    private fun mostrarMensajeGanador(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.game_over)
            .setMessage(mensaje)
            .setPositiveButton(R.string.new_game) { _, _ ->
                reiniciarJuego()
            }
            .setCancelable(false)
            .show()
    }

    private fun reiniciarJuego() {
        // Limpiar el tablero
        tablero = Array(3) { Array(3) { "" } }

        // Limpiar los botones
        for (i in 0..2) {
            for (j in 0..2) {
                botones[i][j].text = ""
                botones[i][j].setBackgroundResource(R.drawable.board_cell_background)
            }
        }

        // Reiniciar variables
        jugadorActual = "X"
        rondas = 0

        // Actualizar texto del turno
        actualizarTurno()
    }
}