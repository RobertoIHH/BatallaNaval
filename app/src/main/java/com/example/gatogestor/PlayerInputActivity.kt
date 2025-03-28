package com.example.tictactoeapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class PlayerInputActivity : AppCompatActivity() {

    private lateinit var etJugador1: TextInputEditText
    private lateinit var etJugador2: TextInputEditText
    private lateinit var btnEmpezarJuego: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_input)

        // Establecer el título de la actividad
        title = getString(R.string.app_name)

        // Inicializar vistas
        etJugador1 = findViewById(R.id.etJugador1)
        etJugador2 = findViewById(R.id.etJugador2)
        btnEmpezarJuego = findViewById(R.id.btnEmpezarJuego)

        // Configurar botón para iniciar juego
        btnEmpezarJuego.setOnClickListener {
            val nombreJugador1 = etJugador1.text.toString().trim()
            val nombreJugador2 = etJugador2.text.toString().trim()

            // Validar que se ingresaron los nombres
            if (nombreJugador1.isEmpty() || nombreJugador2.isEmpty()) {
                Toast.makeText(this, R.string.enter_names_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Crear intent para iniciar la actividad del juego
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("JUGADOR1", nombreJugador1)
                putExtra("JUGADOR2", nombreJugador2)
            }
            startActivity(intent)
        }
    }
}