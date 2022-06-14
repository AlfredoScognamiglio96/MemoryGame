package com.example.memorygameapp_v2.icons_and_utils

import android.graphics.Bitmap

object BitmapScaler {

    //metodo che ridimensiona e mantiene le proporzioni data la larghezza desiderata
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b,width, (b.height * factor).toInt(), true)
    }

    //metodo che ridimensiona e mantiene le proporzioni data l altezza desiderata
    fun scaleToFitHeight(b: Bitmap, heigth: Int): Bitmap {
        val factor = heigth / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), heigth, true)
    }

}
