package com.example.batallanavalgame

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class BatallaNavalActivity : AppCompatActivity() {

    // Constantes
    private val TABLERO_SIZE = 10
    private val BARCOS = listOf(
        Barco(5, "Portaaviones"),  // Tamaño 5
        Barco(4, "Acorazado"),     // Tamaño 4
        Barco(3, "Crucero"),       // Tamaño 3
        Barco(3, "Submarino"),     // Tamaño 3
        Barco(2, "Destructor")     // Tamaño 2
    )

    // UI Components
    private lateinit var tvEstadoJuego: TextView
    private lateinit var tvJugadorActual: TextView
    private lateinit var tvInstrucciones: TextView
    private lateinit var btnRotarBarco: Button
    private lateinit var btnSiguienteBarco: Button
    private lateinit var btnTerminarConfiguracion: Button
    private lateinit var btnCambiarJugador: Button
    private lateinit var btnGuardarPartida: Button
    private lateinit var btnNuevaPartida: Button
    private lateinit var glTablero: GridLayout
    private lateinit var tvTiempoJuego: TextView
    private lateinit var tvPuntajeJugador1: TextView
    private lateinit var tvPuntajeJugador2: TextView
    private lateinit var tvVictoriasJugador1: TextView
    private lateinit var tvVictoriasJugador2: TextView
    private lateinit var spinnerFormatoGuardado: Spinner
    private lateinit var btnVerHistorial: Button
    private lateinit var btnEstadisticas: Button

    // Game State
    private var faseActual = FaseJuego.CONFIGURACION
    private var jugadorActual = 1
    private var barcoActualIndex = 0
    private var orientacionHorizontal = true
    private var nombreJugador1 = "Jugador 1"
    private var nombreJugador2 = "Jugador 2"
    private var puntajeJugador1 = 0
    private var puntajeJugador2 = 0

    // Control de tiempo
    private var tiempoInicioJuego = 0L
    private var tiempoTranscurridoMs = 0L
    private var cronometroActivo = false
    private val handlerCronometro = Handler(Looper.getMainLooper())
    private val actualizarCronometroRunnable = object : Runnable {
        override fun run() {
            actualizarCronometro()
            handlerCronometro.postDelayed(this, 1000) // Actualizar cada segundo
        }
    }

    // Historial de movimientos
    private val historialMovimientos = mutableListOf<Movimiento>()

    // Propiedades para vista previa
    private var previewFila: Int = -1
    private var previewColumna: Int = -1

    // Tableros de juego (uno para cada jugador)
    private val tableroJugador1 = Array(TABLERO_SIZE) { Array(TABLERO_SIZE) { EstadoCelda.VACIA } }
    private val tableroJugador2 = Array(TABLERO_SIZE) { Array(TABLERO_SIZE) { EstadoCelda.VACIA } }
    private val tableroAtaquesJugador1 = Array(TABLERO_SIZE) { Array(TABLERO_SIZE) { false } }
    private val tableroAtaquesJugador2 = Array(TABLERO_SIZE) { Array(TABLERO_SIZE) { false } }

    // Barcos colocados
    private val barcosJugador1 = mutableListOf<BarcoColocado>()
    private val barcosJugador2 = mutableListOf<BarcoColocado>()
    private var interaccionBloqueada = false

    // Manager para guardar/cargar partidas
    private lateinit var batallaNavalManager: SaveGameManager
    // Lógica del juego
    private lateinit var gameLogic: BatallaNavalGameLogic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batalla_naval)

        // Inicializar lógica del juego
        gameLogic = BatallaNavalGameLogic(TABLERO_SIZE, BARCOS)

        // Inicializar UI components
        inicializarViews()

        // Obtener nombres de los jugadores si se proporcionaron
        nombreJugador1 = intent.getStringExtra("JUGADOR1") ?: "Jugador 1"
        nombreJugador2 = intent.getStringExtra("JUGADOR2") ?: "Jugador 2"

        // Inicializar manager
        batallaNavalManager = SaveGameManager(this)

        // Configurar UI avanzada
        configurarSpinnerFormatoGuardado()
        actualizarEstadisticasUI()

        // Configurar listeners
        configurarBotones()

        // Inicializar tablero
        inicializarTablero()

        // Comprobar si debemos cargar partida
        if (intent.getBooleanExtra("CARGAR_PARTIDA", false)) {
            cargarPartida()
        } else {
            // Iniciar juego nuevo y cronómetro
            iniciarCronometro()
            actualizarUI()
        }
    }

    override fun onPause() {
        super.onPause()
        detenerCronometro()
    }

    override fun onResume() {
        super.onResume()
        if (faseActual == FaseJuego.ATAQUE) {
            reanudarCronometro()
        }
    }

    private fun inicializarViews() {
        tvEstadoJuego = findViewById(R.id.tvEstadoJuego)
        tvJugadorActual = findViewById(R.id.tvJugadorActual)
        tvInstrucciones = findViewById(R.id.tvInstrucciones)
        btnRotarBarco = findViewById(R.id.btnRotarBarco)
        btnSiguienteBarco = findViewById(R.id.btnSiguienteBarco)
        btnTerminarConfiguracion = findViewById(R.id.btnTerminarConfiguracion)
        btnCambiarJugador = findViewById(R.id.btnCambiarJugador)
        btnGuardarPartida = findViewById(R.id.btnGuardarPartida)
        btnNuevaPartida = findViewById(R.id.btnNuevaPartida)
        glTablero = findViewById(R.id.glTablero)
        tvTiempoJuego = findViewById(R.id.tvTiempoJuego)
        tvPuntajeJugador1 = findViewById(R.id.tvPuntajeJugador1)
        tvPuntajeJugador2 = findViewById(R.id.tvPuntajeJugador2)
        tvVictoriasJugador1 = findViewById(R.id.tvVictoriasJugador1)
        tvVictoriasJugador2 = findViewById(R.id.tvVictoriasJugador2)
        spinnerFormatoGuardado = findViewById(R.id.spinnerFormatoGuardado)
        btnVerHistorial = findViewById(R.id.btnVerHistorial)
        btnEstadisticas = findViewById(R.id.btnEstadisticas)
    }

    // Resto de los métodos de la Activity...
    // (Los métodos específicos serían demasiado largos para incluirlos todos aquí)

    // Métodos para manejar clicks
    private fun manejarClick(fila: Int, columna: Int) {
        // Verificar si la interacción está bloqueada
        if (interaccionBloqueada) {
            return
        }

        when (faseActual) {
            FaseJuego.CONFIGURACION -> manejarClickConfiguracion(fila, columna)
            FaseJuego.ATAQUE -> manejarClickAtaque(fila, columna)
        }
    }

    private fun manejarClickConfiguracion(fila: Int, columna: Int) {
        // Fix: Verificar si ya se colocaron todos los barcos
        if (barcoActualIndex >= BARCOS.size) {
            Toast.makeText(this, R.string.todos_barcos_colocados, Toast.LENGTH_SHORT).show()
            return
        }

        val barcoActual = BARCOS[barcoActualIndex]
        val tableroActual = if (jugadorActual == 1) tableroJugador1 else tableroJugador2
        val barcosJugador = if (jugadorActual == 1) barcosJugador1 else barcosJugador2

        // Verificar que no se coloquen más barcos de los permitidos
        if (barcosJugador.size >= BARCOS.size) {
            Toast.makeText(this, R.string.maximo_barcos_alcanzado, Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el barco cabe en esa posición
        if (gameLogic.comprobarPosicionValida(
                fila,
                columna,
                barcoActual.longitud,
                orientacionHorizontal,
                tableroActual
            )
        ) {
            // Colocar barco
            val posiciones = mutableListOf<Pair<Int, Int>>()

            for (i in 0 until barcoActual.longitud) {
                val nuevaFila = if (orientacionHorizontal) fila else fila + i
                val nuevaColumna = if (orientacionHorizontal) columna + i else columna

                tableroActual[nuevaFila][nuevaColumna] = EstadoCelda.BARCO
                posiciones.add(Pair(nuevaFila, nuevaColumna))
            }

            // Registrar barco colocado
            barcosJugador.add(BarcoColocado(barcoActual.longitud, posiciones))

            // Pasar al siguiente barco si hay disponible
            if (barcoActualIndex < BARCOS.size - 1) {
                barcoActualIndex++
                actualizarInstrucciones()
            } else {
                Toast.makeText(this, R.string.todos_barcos_colocados, Toast.LENGTH_SHORT).show()
                btnTerminarConfiguracion.visibility = View.VISIBLE
            }

            // Actualizar vista
            actualizarVistaTableroConPreview()

            // Mostrar botón de terminar configuración si todos los barcos están colocados
            if (barcosJugador.size >= BARCOS.size) {
                btnTerminarConfiguracion.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(this, R.string.posicion_invalida, Toast.LENGTH_SHORT).show()
        }
    }

    private fun manejarClickAtaque(fila: Int, columna: Int) {
        if (interaccionBloqueada) {
            return
        }

        val tableroOponente = if (jugadorActual == 1) tableroJugador2 else tableroJugador1
        val tableroAtaques = if (jugadorActual == 1) tableroAtaquesJugador1 else tableroAtaquesJugador2
        val barcosOponente = if (jugadorActual == 1) barcosJugador2 else barcosJugador1

        // Procesar ataque con la lógica de juego
        val resultado = gameLogic.procesarAtaque(fila, columna, tableroOponente, tableroAtaques, barcosOponente)

        interaccionBloqueada = true

        // Registrar movimiento en historial
        val resultadoString = when (resultado) {
            BatallaNavalGameLogic.AtaqueResultado.IMPACTO,
            BatallaNavalGameLogic.AtaqueResultado.HUNDIDO,
            BatallaNavalGameLogic.AtaqueResultado.HUNDIDO_VICTORIA -> "Impacto"
            BatallaNavalGameLogic.AtaqueResultado.AGUA -> "Agua"
            BatallaNavalGameLogic.AtaqueResultado.YA_ATACADO -> {
                interaccionBloqueada = false
                Toast.makeText(this, R.string.ya_atacada, Toast.LENGTH_SHORT).show()
                return
            }
        }

        historialMovimientos.add(Movimiento(jugadorActual, fila, columna, resultadoString))

        when (resultado) {
            BatallaNavalGameLogic.AtaqueResultado.IMPACTO -> {
                Toast.makeText(this, R.string.impacto, Toast.LENGTH_SHORT).show()
                // Incrementar puntuación
                if (jugadorActual == 1) {
                    puntajeJugador1 += 10
                } else {
                    puntajeJugador2 += 10
                }
            }
            BatallaNavalGameLogic.AtaqueResultado.HUNDIDO -> {
                Toast.makeText(this, R.string.barco_hundido, Toast.LENGTH_SHORT).show()
                // Encontrar el barco hundido
                val barcoHundido = barcosOponente.find { barco ->
                    barco.posiciones.any { it.first == fila && it.second == columna }
                }
                // Puntos extra por hundir barco
                if (jugadorActual == 1) {
                    puntajeJugador1 += 20 + (barcoHundido?.longitud ?: 0) * 5
                } else {
                    puntajeJugador2 += 20 + (barcoHundido?.longitud ?: 0) * 5
                }
            }
            BatallaNavalGameLogic.AtaqueResultado.HUNDIDO_VICTORIA -> {
                // Registrar victoria y bonus
                if (jugadorActual == 1) {
                    puntajeJugador1 += 50
                } else {
                    puntajeJugador2 += 50
                }
                mostrarDialogoVictoria()
                return
            }
            BatallaNavalGameLogic.AtaqueResultado.AGUA -> {
                Toast.makeText(this, R.string.agua, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }

        // Actualizar vista y cambiar turno automáticamente después de cada ataque
        actualizarVistaTablero()
        actualizarEstadisticasUI()

        // Pequeña pausa para que el jugador vea el resultado antes de cambiar
        Handler(Looper.getMainLooper()).postDelayed({
            cambiarJugador()
        }, 1500) // 1.5 segundos de pausa
    }

    // Métodos para interfaz de usuario, tablero y cronómetro
    // [Incluye aquí los métodos restantes]
}