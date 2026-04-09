package com.example.cs388_mobile_application_part_2;

import android.app.Application;

class PetApplication: Application() {
    val db by lazy{AppDatabase.getInstance(this)}
}
