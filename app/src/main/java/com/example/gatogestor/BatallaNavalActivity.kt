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

    private fun configurarSpinnerFormatoGuardado() {
        // Crear adapter con las opciones de formatos
        val formatosGuardado = listOf("JSON", "XML", "TXT")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formatosGuardado)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFormatoGuardado.adapter = adapter

        // Establecer formato guardado anteriormente
        val formatoActual = batallaNavalManager.getSavedFormat()
        spinnerFormatoGuardado.setSelection(formatoActual.ordinal)

        // Listener para cambios
        spinnerFormatoGuardado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val formato = when(position) {
                    0 -> SaveGameManager.SaveFormat.JSON
                    1 -> SaveGameManager.SaveFormat.XML
                    2 -> SaveGameManager.SaveFormat.TEXT
                    else -> SaveGameManager.SaveFormat.JSON
                }
                batallaNavalManager.guardarFormatoSeleccionado(formato)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    private fun actualizarEstadisticasUI() {
        tvVictoriasJugador1.text = getString(R.string.victorias_jugador, batallaNavalManager.getVictoriasJugador(1))
        tvVictoriasJugador2.text = getString(R.string.victorias_jugador, batallaNavalManager.getVictoriasJugador(2))
        tvPuntajeJugador1.text = getString(R.string.puntuacion_jugador, puntajeJugador1)
        tvPuntajeJugador2.text = getString(R.string.puntuacion_jugador, puntajeJugador2)
    }

    private fun configurarBotones() {
        btnRotarBarco.setOnClickListener {
            orientacionHorizontal = !orientacionHorizontal
            Toast.makeText(
                this,
                if (orientacionHorizontal) R.string.orientacion_horizontal else R.string.orientacion_vertical,
                Toast.LENGTH_SHORT
            ).show()
            actualizarVistaTableroConPreview()
        }

        btnSiguienteBarco.setOnClickListener {
            // Fix: Comprobar que no se pase del límite de barcos disponibles
            if (barcoActualIndex < BARCOS.size - 1) {
                barcoActualIndex++
                actualizarInstrucciones()
                actualizarVistaTableroConPreview()
            } else {
                Toast.makeText(this, R.string.no_mas_barcos, Toast.LENGTH_SHORT).show()
                btnTerminarConfiguracion.visibility = View.VISIBLE
            }
        }

        btnTerminarConfiguracion.setOnClickListener {
            // Fix: Verificar que todos los barcos han sido colocados
            if (jugadorActual == 1) {
                if (barcosJugador1.size < BARCOS.size) {
                    Toast.makeText(this, R.string.faltan_barcos, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Cambiar al jugador 2 para configuración
                jugadorActual = 2
                barcoActualIndex = 0
                btnTerminarConfiguracion.visibility = View.GONE
                actualizarUI()
            } else {
                if (barcosJugador2.size < BARCOS.size) {
                    Toast.makeText(this, R.string.faltan_barcos, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Ambos jugadores han terminado la configuración
                iniciarFaseAtaque()
            }
        }

        btnCambiarJugador.setOnClickListener {
            cambiarJugador()
        }

        btnGuardarPartida.setOnClickListener {
            guardarPartida()
        }

        btnNuevaPartida.setOnClickListener {
            mostrarDialogoNuevaPartida()
        }

        btnVerHistorial.setOnClickListener {
            mostrarHistorialMovimientos()
        }

        btnEstadisticas.setOnClickListener {
            mostrarEstadisticas()
        }
    }

    private fun mostrarEstadisticas() {
        val mensaje = StringBuilder()
        mensaje.append(getString(R.string.estadisticas))
        mensaje.append("\n\n")
        mensaje.append(getString(R.string.puntuacion_jugador1, nombreJugador1, puntajeJugador1))
        mensaje.append("\n")
        mensaje.append(getString(R.string.puntuacion_jugador2, nombreJugador2, puntajeJugador2))
        mensaje.append("\n\n")
        mensaje.append(getString(R.string.victorias_jugador, nombreJugador1, batallaNavalManager.getVictoriasJugador(1)))
        mensaje.append("\n")
        mensaje.append(getString(R.string.victorias_jugador, nombreJugador2, batallaNavalManager.getVictoriasJugador(2)))
        mensaje.append("\n\n")
        mensaje.append(getString(R.string.partidas_jugadas, batallaNavalManager.getPartidasJugadas()))

        AlertDialog.Builder(this)
            .setTitle(R.string.estadisticas)
            .setMessage(mensaje.toString())
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun inicializarTablero() {
        glTablero.removeAllViews()

        for (fila in 0 until TABLERO_SIZE) {
            for (columna in 0 until TABLERO_SIZE) {
                val celda = View(this)
                val params = GridLayout.LayoutParams()

                // Calcular tamaño de celda basado en el tamaño de pantalla
                val size = (resources.displayMetrics.widthPixels - glTablero.paddingLeft -
                        glTablero.paddingRight - (TABLERO_SIZE * 2)) / TABLERO_SIZE

                params.width = size
                params.height = size
                params.rowSpec = GridLayout.spec(fila)
                params.columnSpec = GridLayout.spec(columna)

                celda.layoutParams = params
                celda.background = ContextCompat.getDrawable(this, R.drawable.cell_empty)
                celda.tag = "$fila,$columna"

                // Agregar eventos para detectar hover/movimiento
                celda.setOnHoverListener { view, event ->
                    if (faseActual == FaseJuego.CONFIGURACION) {
                        when (event.action) {
                            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                                val position = (view.tag as String).split(",")
                                previewFila = position[0].toInt()
                                previewColumna = position[1].toInt()
                                actualizarVistaTableroConPreview()
                                return@setOnHoverListener true
                            }

                            MotionEvent.ACTION_HOVER_EXIT -> {
                                previewFila = -1
                                previewColumna = -1
                                actualizarVistaTablero()
                                return@setOnHoverListener true
                            }
                        }
                    }
                    false
                }

                // También detectar movimiento del dedo
                celda.setOnTouchListener { view, event ->
                    if (faseActual == FaseJuego.CONFIGURACION) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                val position = (view.tag as String).split(",")
                                previewFila = position[0].toInt()
                                previewColumna = position[1].toInt()
                                actualizarVistaTableroConPreview()
                                return@setOnTouchListener false // para que el clic siga funcionando
                            }

                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                previewFila = -1
                                previewColumna = -1
                                actualizarVistaTablero()
                                return@setOnTouchListener false // para que el clic siga funcionando
                            }
                        }
                    }
                    false // para que el clic siga funcionando
                }

                celda.setOnClickListener {
                    val position = (it.tag as String).split(",")
                    val row = position[0].toInt()
                    val col = position[1].toInt()
                    manejarClick(row, col)
                }

                glTablero.addView(celda)
            }
        }
    }

    private fun actualizarUI() {
        // Actualizar textos según la fase actual
        when (faseActual) {
            FaseJuego.CONFIGURACION -> {
                tvEstadoJuego.text = getString(R.string.fase_configuracion)

                // Usar los nombres personalizados en lugar de los textos genéricos
                tvJugadorActual.text = if (jugadorActual == 1) nombreJugador1 else nombreJugador2

                actualizarInstrucciones()

                btnRotarBarco.visibility = View.VISIBLE
                btnSiguienteBarco.visibility = View.VISIBLE
                btnCambiarJugador.visibility = View.GONE

                // Fix: Mostrar botón de terminar configuración solo cuando todos los barcos estén colocados
                val barcosColocados = if (jugadorActual == 1) barcosJugador1.size else barcosJugador2.size
                btnTerminarConfiguracion.visibility = if (barcosColocados >= BARCOS.size) View.VISIBLE else View.GONE

                // Mostrar tablero vacío para el jugador actual
                actualizarVistaTablero()
            }

            FaseJuego.ATAQUE -> {
                tvEstadoJuego.text = getString(R.string.fase_ataque)

                // Usar los nombres personalizados
                tvJugadorActual.text = if (jugadorActual == 1) nombreJugador1 else nombreJugador2

                tvInstrucciones.text = getString(R.string.instrucciones_ataque)

                btnRotarBarco.visibility = View.GONE
                btnSiguienteBarco.visibility = View.GONE
                btnTerminarConfiguracion.visibility = View.GONE
                btnCambiarJugador.visibility = View.VISIBLE

                // Mostrar tablero del oponente para ataques
                actualizarVistaTablero()
            }
        }

        // Actualizar puntuación y estadísticas
        actualizarEstadisticasUI()
    }

    private fun actualizarInstrucciones() {
        if (barcoActualIndex < BARCOS.size) {
            val barcoActual = BARCOS[barcoActualIndex]
            tvInstrucciones.text = getString(
                R.string.instrucciones_colocar_barco,
                barcoActual.nombre,
                barcoActual.longitud
            )
        } else {
            tvInstrucciones.text = getString(R.string.todos_barcos_colocados)
        }
    }

    private fun actualizarVistaTablero() {
        val tableroActual: Array<Array<EstadoCelda>>
        val tableroAtaques: Array<Array<Boolean>>

        if (faseActual == FaseJuego.CONFIGURACION) {
            // En fase de configuración, mostrar el tablero propio
            tableroActual = if (jugadorActual == 1) tableroJugador1 else tableroJugador2
            tableroAtaques =
                if (jugadorActual == 1) tableroAtaquesJugador1 else tableroAtaquesJugador2

            // Actualizar cada celda
            var index = 0
            for (fila in 0 until TABLERO_SIZE) {
                for (columna in 0 until TABLERO_SIZE) {
                    val celda = glTablero.getChildAt(index++)
                    when (tableroActual[fila][columna]) {
                        EstadoCelda.VACIA -> celda.background =
                            ContextCompat.getDrawable(this, R.drawable.cell_empty)

                        EstadoCelda.BARCO -> celda.background =
                            ContextCompat.getDrawable(this, R.drawable.cell_ship)

                        EstadoCelda.AGUA -> celda.background =
                            ContextCompat.getDrawable(this, R.drawable.cell_water)

                        EstadoCelda.IMPACTO -> celda.background =
                            ContextCompat.getDrawable(this, R.drawable.cell_hit)
                    }
                }
            }
        } else {
            // En fase de ataque, mostrar el tablero del oponente
            tableroActual = if (jugadorActual == 1) tableroJugador2 else tableroJugador1
            tableroAtaques =
                if (jugadorActual == 1) tableroAtaquesJugador1 else tableroAtaquesJugador2

            // Actualizar cada celda, ocultando barcos no atacados
            var index = 0
            for (fila in 0 until TABLERO_SIZE) {
                for (columna in 0 until TABLERO_SIZE) {
                    val celda = glTablero.getChildAt(index++)
                    if (tableroAtaques[fila][columna]) {
                        // Celda ya atacada
                        when (tableroActual[fila][columna]) {
                            EstadoCelda.BARCO -> celda.background =
                                ContextCompat.getDrawable(this, R.drawable.cell_hit)

                            else -> celda.background =
                                ContextCompat.getDrawable(this, R.drawable.cell_water)
                        }
                    } else {
                        // Celda no atacada
                        celda.background = ContextCompat.getDrawable(this, R.drawable.cell_empty)
                    }
                }
            }
        }
    }

    private fun actualizarVistaTableroConPreview() {
        if (previewFila == -1 || previewColumna == -1 || barcoActualIndex >= BARCOS.size) {
            actualizarVistaTablero()
            return
        }

        val tableroActual = if (jugadorActual == 1) tableroJugador1 else tableroJugador2
        val barcoActual = BARCOS[barcoActualIndex]

        // Actualizar cada celda
        var index = 0
        for (fila in 0 until TABLERO_SIZE) {
            for (columna in 0 until TABLERO_SIZE) {
                val celda = glTablero.getChildAt(index++)

                // Verificar si la celda es parte de la vista previa
                val esVistaPrevia = gameLogic.esParteDeLaVistaPrevia(
                    fila, columna, previewFila, previewColumna,
                    barcoActual.longitud, orientacionHorizontal
                )

                // Determinar qué color mostrar
                val color = when {
                    esVistaPrevia -> {
                        // Verificar si la posición es válida
                        if (gameLogic.comprobarPosicionValida(
                                previewFila, previewColumna,
                                barcoActual.longitud, orientacionHorizontal, tableroActual
                            )
                        ) {
                            R.drawable.cell_ship_preview_valid
                        } else {
                            R.drawable.cell_ship_preview_invalid
                        }
                    }

                    tableroActual[fila][columna] == EstadoCelda.VACIA -> R.drawable.cell_empty
                    tableroActual[fila][columna] == EstadoCelda.BARCO -> R.drawable.cell_ship
                    tableroActual[fila][columna] == EstadoCelda.AGUA -> R.drawable.cell_water
                    else -> R.drawable.cell_hit
                }

                celda.background = ContextCompat.getDrawable(this, color)
            }
        }
    }

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

// Actualizar vista y cambiar turno autom