package com.example.batallanavalgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnNuevaPartida: Button
    private lateinit var btnCargarPartida: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnNuevaPartida = findViewById(R.id.btnNuevaPartida)
        btnCargarPartida = findViewById(R.id.btnCargarPartida)

        btnNuevaPartida.setOnClickListener {
            // Iniciar nueva partida
            val intent = Intent(this, BatallaNavalActivity::class.java)
            startActivity(intent)
        }

        btnCargarPartida.setOnClickListener {
            // Cargar partida guardada
            val intent = Intent(this, BatallaNavalActivity::class.java)
            intent.putExtra("CARGAR_PARTIDA", true)
            startActivity(intent)
        }
    }
}