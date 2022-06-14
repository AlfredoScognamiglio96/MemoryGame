package com.example.memorygameapp_v2.modelli

//Considero tutte le possibili info di una carta
//DIFFERENZA TRA VAL E VAR
// VAL è qualcosa che una volta settato non può essere modificato
// Viceversa VAR

data class AttributeMemoryCard(
    val identifier: Int,                                    //non sarà presente nel caso del customGame
    val imageUrl: String? = null,                           //parametro opzionale
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false

)
