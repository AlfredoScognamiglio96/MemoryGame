package com.example.memorygameapp_v2.modelli

import com.example.memorygameapp_v2.icons_and_utils.DEFAULT_ICONS

class AttributeMemoryGame(
    private val boardSize: BoardSize,
    private val customImages: List<String>?
) {

    val cards: List<AttributeMemoryCard>
    var numPairsFound = 0

    private var numCardFlips = 0
    private var indexOfSelectionCard: Int? = null                                       //Inizializzato a null poichè quando inizia il gioco nessuna carta è selezionata

    //Considero una list di carte basate sul boardSize, prendo delle immagini random e creo il gioco basandomi su quelle
    init {
        //caso del gioco base con icone scelte randomicamente
        if (customImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getPairs())
            //Copie delle varie icone
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { AttributeMemoryCard(it) }
        }
        //caso in cui viene scelto un customGame con immagini relative sottostanti le carte
        else {
            val randomImages = (customImages + customImages).shuffled()
            //tramite hashCode prendo un imageUrl e lo converto in un intero univoco
            cards = randomImages.map { AttributeMemoryCard(it.hashCode(),it) }
        }
    }

    //Boolean per indicare se una corrispondenza è stata trovata o meno
    fun flipCard(position: Int): Boolean {

        numCardFlips++
        val card = cards[position]
        var foundMatch = false

        if (indexOfSelectionCard == null) {
            //Caso in cui 0 o 2 carte sono girate
            restoreCards()
            indexOfSelectionCard = position
        } else {
            // 1 carta già girata
            foundMatch = checkForMatch(indexOfSelectionCard!!,position)                             //Uso !! per forzare indexOfSelectionCard ad essere non nullo
            indexOfSelectionCard = null                                                                 //In questo momento 1 carta è girata e selezionata
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) {
            return false                                            //Le due carte non matchano
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if(!card.isMatched) {
                card.isFaceUp = false                               //Riporta la carta allo stato iniziale se la carta selezionata non è matchata
            }
        }
    }

    fun gameWon(): Boolean {
        return numPairsFound == boardSize.getPairs()
    }

    //Metodo che gestisce il caso in cui la carta già sia girata
    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int{
        return numCardFlips / 2
    }

}