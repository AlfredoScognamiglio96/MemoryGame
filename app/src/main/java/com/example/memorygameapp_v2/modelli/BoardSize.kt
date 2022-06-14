package com.example.memorygameapp_v2.modelli

//Definizione livelli del gioco e definizione dell altezza e larghezza in base al num di carte
enum class BoardSize(val numCards: Int) {
    FACILE(8),
    MEDIO(18),
    DIFFICILE(24);

    //utilizzato per accedere al valore del BoardSize in base al livello scelto(FACILE-MEDIO-DIFFICILE)
    companion object{
        fun getByValue(value: Int) = values().first{ it.numCards == value }
    }

    fun getWidth(): Int {
        return when (this) {
            FACILE -> 2
            MEDIO -> 3
            DIFFICILE -> 4
        }
    }

    fun getHeight(): Int {
        return numCards / getWidth()
    }

    fun getPairs(): Int {
        return numCards / 2
    }
}