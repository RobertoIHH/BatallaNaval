package com.example.batallanavalgame

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.app.AlertDialog
import android.content.Intent
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
import android.util.Log

class BatallaNavalActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 101
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

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
    private val THEME_PREF_NAME = "tema_preferido"
    private val KEY_THEME = "tema_actual"

    private fun cambiarTema(usarTemaGuinda: Boolean) {
        // Guardar la preferencia de tema
        val sharedPreferences = getSharedPreferences(THEME_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_THEME, usarTemaGuinda).apply()

        // Aplicar el tema inmediatamente
        if (usarTemaGuinda) {
            setTheme(R.style.Theme_BatallaNaval) // Tema guinda
        } else {
            setTheme(R.style.Theme_BatallaNavalGame) // Tema azul (por defecto)
        }

        // Recrear la actividad para que el cambio de tema surta efecto
        recreate()
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
        val sharedPreferences = getSharedPreferences(THEME_PREF_NAME, Context.MODE_PRIVATE)
        val usarTemaGuinda = sharedPreferences.getBoolean(KEY_THEME, false)
        if (usarTemaGuinda) {
            setTheme(R.style.Theme_BatallaNaval) // Tema guinda
        } else {
            setTheme(R.style.Theme_BatallaNavalGame) // Tema azul (por defecto)
        }

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
        ajustarTableros()
        aplicarEstiloGuinda()


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
        // Verificar y solicitar permisos
        if (!hasPermissions()) {
            requestPermissions()
        }
    }
    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permisos concedidos
                Toast.makeText(this, "Permisos concedidos para acceder a archivos externos", Toast.LENGTH_SHORT).show()
            } else {
                // Permisos denegados
                Toast.makeText(this, "Se requieren permisos para guardar/cargar partidas en almacenamiento externo", Toast.LENGTH_LONG).show()
            }
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
    private fun configurarSelectorTema() {
        // Crear un Switch para seleccionar tema
        val switchTema = Switch(this)
        switchTema.text = getString(R.string.tema_guinda)

        // Configurar el estado inicial basado en la preferencia guardada
        val sharedPreferences = getSharedPreferences(THEME_PREF_NAME, Context.MODE_PRIVATE)
        val usarTemaGuinda = sharedPreferences.getBoolean(KEY_THEME, false)
        switchTema.isChecked = usarTemaGuinda

        // Añadir el Switch a la parte superior de la vista
        val container = findViewById<LinearLayout>(R.id.container_principal)
        container.addView(switchTema, 0) // Añadir al principio

        // Configurar el listener
        switchTema.setOnCheckedChangeListener { _, isChecked ->
            cambiarTema(isChecked)
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

    private fun aplicarEstiloGuinda() {
        // Aplicar a botones
        val botones = listOf(
            findViewById<Button>(R.id.btnNuevaPartida),
            // Eliminar referencias a botones que no existen
            // findViewById<Button>(R.id.btnPosicionarBarcos),
            // findViewById<Button>(R.id.btnIniciarJuego)
        )

        botones.forEach { boton ->
            boton.setBackgroundColor(ContextCompat.getColor(this, R.color.guinda_ipn_primary))
            boton.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        // Comentar la línea que hace referencia a tvTitulo que no existe
        // findViewById<TextView>(R.id.tvTitulo)?.setTextColor(
        //    ContextCompat.getColor(this, R.color.guinda_ipn_primary)
        // )

        // Aplicar a celdas de tableros - revisar si estos tableros existen
        aplicarEstiloTableros()
    }

    private fun aplicarEstiloTableros() {
        // Solo mantener la lógica para el tablero que sabemos que existe - glTablero
        // En lugar de buscar tableroJugador y tableroOponente que no existen en el layout

        // Aplicar estilo directamente a glTablero
        for (i in 0 until glTablero.childCount) {
            val celda = glTablero.getChildAt(i)
            // Usar un estilo genérico en lugar de uno específico que podría no existir
            celda.background = ContextCompat.getDrawable(this, R.drawable.cell_empty)
        }

        // El código original que referencia tableroJugador y tableroOponente se comenta:
        /*
        // Tablero jugador
        val tableroJugador = findViewById<GridLayout>(R.id.tableroJugador)
        for (i in 0 until tableroJugador.childCount) {
            val celda = tableroJugador.getChildAt(i)
            celda.background = ContextCompat.getDrawable(this, R.drawable.celda_jugador_background)
        }

        // Tablero oponente
        val tableroOponente = findViewById<GridLayout>(R.id.tableroOponente)
        for (i in 0 until tableroOponente.childCount) {
            val celda = tableroOponente.getChildAt(i)
            celda.background = ContextCompat.getDrawable(this, R.drawable.celda_oponente_background)
        }
        */
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

    private fun ajustarTableros() {
        // Obtener dimensiones de la pantalla
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Calcular el tamaño máximo del tablero (80% del ancho de pantalla)
        // Reducido de 90% a 80% para dejar más espacio
        val tableroSize = (screenWidth * 0.8).toInt()
        // Asegurar que el tablero no sea más grande que el 40% de la altura de la pantalla
        val maxHeight = (screenHeight * 0.4).toInt()
        val finalSize = minOf(tableroSize, maxHeight)

        val cellSize = finalSize / 10 // 10 celdas por lado

        // Aplicar al tablero principal
        val paramsTablero = glTablero.layoutParams
        paramsTablero.width = finalSize
        paramsTablero.height = finalSize
        glTablero.layoutParams = paramsTablero

        // Ajustar cada celda del tablero
        for (i in 0 until glTablero.childCount) {
            val celda = glTablero.getChildAt(i)
            if (celda.layoutParams is GridLayout.LayoutParams) {
                val paramsCelda = celda.layoutParams as GridLayout.LayoutParams
                paramsCelda.width = cellSize
                paramsCelda.height = cellSize
                // Reducir los márgenes para celdas más compactas
                paramsCelda.setMargins(1, 1, 1, 1)
                celda.layoutParams = paramsCelda
            }
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
// Continúa desde línea 432 de BatallaNavalActivity.kt

        // Actualizar vista y cambiar turno automáticamente después de un tiempo breve
        actualizarVistaTablero()
        actualizarEstadisticasUI()

        // Programar cambio de turno automático después de un breve retraso (para que el jugador vea el resultado)
        Handler(Looper.getMainLooper()).postDelayed({
            interaccionBloqueada = false
            if (resultado != BatallaNavalGameLogic.AtaqueResultado.IMPACTO &&
                resultado != BatallaNavalGameLogic.AtaqueResultado.HUNDIDO) {
                cambiarJugador()
            }
        }, 1500)
    }

    private fun cambiarJugador() {
        // Cambiar al otro jugador
        jugadorActual = if (jugadorActual == 1) 2 else 1

        // Diálogo informativo
        AlertDialog.Builder(this)
            .setTitle(R.string.cambio_jugador)
            .setMessage(getString(R.string.turno_jugador, jugadorActual))
            .setPositiveButton(R.string.ok) { _, _ ->
                // Actualizar UI
                actualizarUI()
            }
            .setCancelable(false)
            .show()
    }

    private fun iniciarFaseAtaque() {
        faseActual = FaseJuego.ATAQUE
        jugadorActual = 1 // El primer jugador comienza el ataque

        // Reiniciar cronómetro para fase de ataque
        detenerCronometro()
        iniciarCronometro()

        actualizarUI()

        // Mostrar mensaje de inicio de fase de ataque
        Snackbar.make(glTablero, R.string.fase_ataque, Snackbar.LENGTH_LONG).show()
    }

    private fun mostrarDialogoVictoria() {
        // Detener cronómetro
        detenerCronometro()

        // Formato para el tiempo total
        val formatoTiempo = SimpleDateFormat("mm:ss", Locale.getDefault())
        formatoTiempo.timeZone = TimeZone.getTimeZone("UTC")
        val tiempoFormateado = formatoTiempo.format(Date(tiempoTranscurridoMs))

        // Nombre del jugador ganador
        val nombreGanador = if (jugadorActual == 1) nombreJugador1 else nombreJugador2

        // Registrar victoria en las estadísticas
        batallaNavalManager.registrarVictoria(jugadorActual)

        // Mostrar diálogo de victoria con estadísticas
        AlertDialog.Builder(this)
            .setTitle(R.string.victoria)
            .setMessage(
                getString(R.string.felicidades_victoria, nombreGanador) + "\n\n" +
                        getString(R.string.puntuacion_final, if (jugadorActual == 1) puntajeJugador1 else puntajeJugador2) + "\n" +
                        getString(R.string.tiempo_total, tiempoFormateado) + "\n" +
                        getString(R.string.movimientos_totales, historialMovimientos.size)
            )
            .setPositiveButton(R.string.nueva_partida) { _, _ ->
                // Iniciar nueva partida
                mostrarDialogoNuevaPartida()
            }
            .setNegativeButton(R.string.salir) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun guardarPartida() {
        // Crear objeto de estado para guardar
        val estadoPartida = EstadoPartida(
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
            nombreJugador2,
            puntajeJugador1,
            puntajeJugador2,
            tiempoTranscurridoMs / 1000, // Guardar en segundos
            historialMovimientos
        )

        // Utilizar el SaveGameManager para guardar
        batallaNavalManager.guardarPartida(estadoPartida, vista = findViewById(android.R.id.content))
    }

    private fun cargarPartida() {
        try {
            val estadoPartida = batallaNavalManager.cargarPartida()

            if (estadoPartida == null) {
                Toast.makeText(this, R.string.no_partida_guardada, Toast.LENGTH_SHORT).show()
                // Si no hay partida guardada, iniciar juego nuevo
                iniciarCronometro()
                actualizarUI()
                return
            }

            // Cargar todos los datos del estado
            faseActual = estadoPartida.faseActual
            jugadorActual = estadoPartida.jugadorActual
            barcoActualIndex = estadoPartida.barcoActualIndex
            orientacionHorizontal = estadoPartida.orientacionHorizontal

            // Copiar tableros
            for (i in 0 until TABLERO_SIZE) {
                for (j in 0 until TABLERO_SIZE) {
                    tableroJugador1[i][j] = estadoPartida.tableroJugador1[i][j]
                    tableroJugador2[i][j] = estadoPartida.tableroJugador2[i][j]
                    tableroAtaquesJugador1[i][j] = estadoPartida.tableroAtaquesJugador1[i][j]
                    tableroAtaquesJugador2[i][j] = estadoPartida.tableroAtaquesJugador2[i][j]
                }
            }

            // Cargar barcos
            barcosJugador1.clear()
            barcosJugador1.addAll(estadoPartida.barcosJugador1)

            barcosJugador2.clear()
            barcosJugador2.addAll(estadoPartida.barcosJugador2)

            // Cargar nombres y puntuaciones
            nombreJugador1 = estadoPartida.nombreJugador1
            nombreJugador2 = estadoPartida.nombreJugador2
            puntajeJugador1 = estadoPartida.puntajeJugador1
            puntajeJugador2 = estadoPartida.puntajeJugador2

            // Cargar tiempo transcurrido (convertir de segundos a ms)
            tiempoTranscurridoMs = estadoPartida.tiempoJuegoSegundos * 1000

            // Cargar historial de movimientos
            historialMovimientos.clear()
            historialMovimientos.addAll(estadoPartida.historialMovimientos)

            // Iniciar cronómetro con el tiempo cargado
            if (faseActual == FaseJuego.ATAQUE) {
                iniciarCronometro()
            }

            // Actualizar la UI
            actualizarUI()

            // Mostrar resumen de la partida cargada
            mostrarResumenPartidaCargada(estadoPartida)
        } catch (e: Exception) {
            Log.e("BatallaNaval", "Error cargando partida: ${e.message}")
            Toast.makeText(this, "Error al cargar partida: ${e.message}", Toast.LENGTH_LONG).show()
            // Si hay un error, iniciar juego nuevo
            iniciarCronometro()
            actualizarUI()
        }
    }

    private fun mostrarResumenPartidaCargada(estadoPartida: EstadoPartida) {
        // Formatear el tiempo de juego
        val formatoTiempo = SimpleDateFormat("mm:ss", Locale.getDefault())
        formatoTiempo.timeZone = TimeZone.getTimeZone("UTC")
        val tiempoFormateado = formatoTiempo.format(Date(estadoPartida.tiempoJuegoSegundos * 1000))

        // Nombre del jugador actual
        val nombreJugadorActual = if (estadoPartida.jugadorActual == 1)
            estadoPartida.nombreJugador1 else estadoPartida.nombreJugador2

        // Crear mensaje de resumen
        val mensaje = StringBuilder()
            .append(getString(R.string.fase_actual, estadoPartida.faseActual.name))
            .append("\n")
            .append(getString(R.string.turno_actual, nombreJugadorActual))
            .append("\n")
            .append(getString(R.string.tiempo_jugado, tiempoFormateado))
            .append("\n\n")
            .append(getString(R.string.puntuacion_jugador1, estadoPartida.nombreJugador1, estadoPartida.puntajeJugador1))
            .append("\n")
            .append(getString(R.string.puntuacion_jugador2, estadoPartida.nombreJugador2, estadoPartida.puntajeJugador2))
            .append("\n\n")
            .append(getString(R.string.movimientos_realizados, estadoPartida.historialMovimientos.size))

        // Mostrar diálogo con resumen
        AlertDialog.Builder(this)
            .setTitle(R.string.resumen_partida)
            .setMessage(mensaje.toString())
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun mostrarDialogoNuevaPartida() {
        AlertDialog.Builder(this)
            .setTitle(R.string.nueva_partida)
            .setMessage(R.string.confirmar_nueva_partida)
            .setPositiveButton(R.string.si) { _, _ ->
                // Iniciar nueva partida (volver a la actividad de input de jugadores)
                val intent = Intent(this, PlayerInputActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun mostrarHistorialMovimientos() {
        if (historialMovimientos.isEmpty()) {
            Toast.makeText(this, R.string.no_movimientos, Toast.LENGTH_SHORT).show()
            return
        }

        // Crear lista formateada de movimientos
        val formatoFecha = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val stringBuilder = StringBuilder()

        historialMovimientos.forEachIndexed { index, movimiento ->
            val nombreJugador = if (movimiento.jugador == 1) nombreJugador1 else nombreJugador2
            val horaFormateada = formatoFecha.format(Date(movimiento.timestamp))

            stringBuilder.append("${index + 1}. ")
            stringBuilder.append(
                getString(
                    R.string.movimiento_detalle,
                    nombreJugador,
                    movimiento.fila,
                    movimiento.columna,
                    movimiento.resultado,
                    horaFormateada
                )
            )
            stringBuilder.append("\n")
        }

        // Mostrar diálogo con historial
        AlertDialog.Builder(this)
            .setTitle(R.string.historial_movimientos)
            .setMessage(stringBuilder.toString())
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    // Funciones para el cronómetro
    private fun iniciarCronometro() {
        if (!cronometroActivo) {
            tiempoInicioJuego = SystemClock.elapsedRealtime() - tiempoTranscurridoMs
            handlerCronometro.postDelayed(actualizarCronometroRunnable, 1000)
            cronometroActivo = true
        }
    }

    private fun detenerCronometro() {
        if (cronometroActivo) {
            tiempoTranscurridoMs = SystemClock.elapsedRealtime() - tiempoInicioJuego
            handlerCronometro.removeCallbacks(actualizarCronometroRunnable)
            cronometroActivo = false
        }
    }

    private fun reanudarCronometro() {
        if (!cronometroActivo) {
            tiempoInicioJuego = SystemClock.elapsedRealtime() - tiempoTranscurridoMs
            handlerCronometro.postDelayed(actualizarCronometroRunnable, 1000)
            cronometroActivo = true
        }
    }

    private fun actualizarCronometro() {
        tiempoTranscurridoMs = SystemClock.elapsedRealtime() - tiempoInicioJuego

        // Formatear tiempo
        val segundos = (tiempoTranscurridoMs / 1000).toInt() % 60
        val minutos = (tiempoTranscurridoMs / (1000 * 60)).toInt() % 60

        // Actualizar TextView
        tvTiempoJuego.text = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Asegurar que se detiene el cronómetro al destruir la actividad
        detenerCronometro()
    }
}