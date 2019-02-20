package com.anwesh.uiprojects.linkedbitrifillstepview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.bitrifillstepview.BiTriFillStepView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiTriFillStepView.create(this)
    }
}
