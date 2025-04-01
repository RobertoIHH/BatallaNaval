// Esta es una actualización parcial para agregar soporte para nombres de jugadores

// En la clase BatallaNavalActivity, agrega estas propiedades:
private var nombreJugador1 = "Jugador 1"
private var nombreJugador2 = "Jugador 2"

// Modifica el método onCreate después de inicializar vistas:
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_batalla_naval)

    // Inicializar UI components
    inicializarViews()

    // Obtener nombres de los jugadores si se proporcionaron
    nombreJugador1 = intent.getStringExtra("JUGADOR1") ?: "Jugador 1"
    nombreJugador2 = intent.getStringExtra("JUGADOR2") ?: "Jugador 2"

    // Inicializar manager
    batallaNavalManager = BatallaNavalManager(this)

    // Configurar listeners
    configurarBotones()

    // Inicializar tablero
    inicializarTablero()

    // Comprobar si debemos cargar partida
    if (intent.getBooleanExtra("CARGAR_PARTIDA", false)) {
        cargarPartida()
    } else {
        // Iniciar juego nuevo
        actualizarUI()
    }
}

// Modifica el método actualizarUI en la sección donde establece el texto del jugador actual:
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

            // Mostrar tablero vacío para el jugador actual
            actualizarVistaTablero()
        }
        FaseJuego.ATAQUE -> {
            tvEstadoJuego.text = getString(R.string.fase_ataque)

            // Usar los nombres personalizados en lugar de los textos genéricos
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
}

// Modifica el método mostrarDialogoVictoria:
private fun mostrarDialogoVictoria() {
    val nombreGanador = if (jugadorActual == 1) nombreJugador1 else nombreJugador2

    AlertDialog.Builder(this)
        .setTitle(R.string.victoria)
        .setMessage("¡Felicidades $nombreGanador! Has ganado la partida.")
        .setPositiveButton(R.string.nueva_partida) { _, _ ->
            reiniciarJuego()
        }
        .setNegativeButton(R.string.salir) { _, _ ->
            finish()
        }
        .setCancelable(false)
        .show()
}

// Modifica el método cambiarJugador:
private fun cambiarJugador() {
    jugadorActual = if (jugadorActual == 1) 2 else 1
    actualizarUI()

    // Mostrar diálogo de cambio de jugador con nombre personalizado
    val nombreJugadorActual = if (jugadorActual == 1) nombreJugador1 else nombreJugador2

    AlertDialog.Builder(this)
        .setTitle(R.string.cambio_jugador)
        .setMessage("Turno de $nombreJugadorActual")
        .setPositiveButton(R.string.ok, null)
        .show()
}

// Modifica la clase EstadoJuego para incluir los nombres de los jugadores:
data class EstadoJuego(
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
    val nombreJugador2: String
) {
    // El resto del código de la clase EstadoJuego...
}

// Modifica el método guardarPartida para incluir los nombres:
private fun guardarPartida() {
    val estadoJuego = EstadoJuego(
        faseActual,
        jugadorActual,
        barcoActualIndex,
        orientacionHorizontal,
        tableroJugador1,
        tableroJugador2,
        tableroAtaquesJugador1,
        tableroAtaquesJugador2,
        barcosJugador1,
        barcosJugador2,
        nombreJugador1,
        nombreJugador2
    )

    batallaNavalManager.guardarPartida(estadoJuego)
    Toast.makeText(this, R.string.partida_guardada, Toast.LENGTH_SHORT).show()
}

// Modifica el método cargarPartida para recuperar los nombres:
private fun cargarPartida() {
    val estadoJuego = batallaNavalManager.cargarPartida()
    if (estadoJuego != null) {
        // Restaurar estado
        faseActual = estadoJuego.faseActual
        jugadorActual = estadoJuego.jugadorActual
        barcoActualIndex = estadoJuego.barcoActualIndex
        orientacionHorizontal = estadoJuego.orientacionHorizontal

        // Restaurar nombres de jugadores
        nombreJugador1 = estadoJuego.nombreJugador1
        nombreJugador2 = estadoJuego.nombreJugador2

        // Copiar tableros
        for (i in 0 until TABLERO_SIZE) {
            for (j in 0 until TABLERO_SIZE) {
                tableroJugador1[i][j] = estadoJuego.tableroJugador1[i][j]
                tableroJugador2[i][j] = estadoJuego.tableroJugador2[i][j]
                tableroAtaquesJugador1[i][j] = estadoJuego.tableroAtaquesJugador1[i][j]
                tableroAtaquesJugador2[i][j] = estadoJuego.tableroAtaquesJugador2[i][j]
            }
        }

        // Restaurar barcos
        barcosJugador1.clear()
        barcosJugador1.addAll(estadoJuego.barcosJugador1)
        barcosJugador2.clear()
        barcosJugador2.addAll(estadoJuego.barcosJugador2)

        // Actualizar UI
        actualizarUI()
        Toast.makeText(this, R.string.partida_cargada, Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(this, R.string.no_partida_guardada, Toast.LENGTH_SHORT).show()
    }
}