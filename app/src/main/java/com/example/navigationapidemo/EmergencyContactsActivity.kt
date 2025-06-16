package com.example.navigationapidemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText


class EmergencyContactsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emergency_contacts_page)

        val button = findViewById<Button>(R.id.Confirn_Contacts)
        button.setOnClickListener {
            val Email1 = findViewById<TextInputEditText>(R.id.contact_1)
            val Email2 = findViewById<TextInputEditText>(R.id.contact_2)
            val Email3 = findViewById<TextInputEditText>(R.id.contact_3)

            if(Email1.text!!.isNotEmpty() || Email2.text!!.isNotEmpty() || Email3.text!!.isNotEmpty()){
                val sharedPref = this.getSharedPreferences("emergency_contacts", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("saved_contacts","true")

                if(Email1.text!!.isNotEmpty()){
                    editor.putString("email1", Email1.text.toString()).apply()
                }

                if(Email2.text!!.isNotEmpty()){
                    editor.putString("email2", Email2.text.toString()).apply()
                }

                if(Email3.text!!.isNotEmpty()){
                    editor.putString("email3", Email3.text.toString()).apply()
                }
                editor.apply()
                startActivity(Intent(this, NavViewActivity::class.java))

            }else{
                Toast.makeText(this,"Enter at least one contact",Toast.LENGTH_LONG).show();
            }
        }
    }
}