package com.example.memorygameapp_v2

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygameapp_v2.modelli.AttributeMemoryCard
import com.example.memorygameapp_v2.modelli.BoardSize
import com.squareup.picasso.Picasso
import kotlin.math.min

//MemoryBoardAdapter sottoclasse del recyclerView
//ViewHolder è un oggetto che fornisce accesso a tutte le viste di un elemento recyclerView
class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<AttributeMemoryCard>,
    private val cardClickListener: CardClickListener
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    //Ci permette di definire costanti e accedere ai suoi membri mediante la classe contenitore
    companion object {
        private const val margin_size = 10                  //Margine tra ogni carta all interno dello spazio allocato
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    //onCreateViewHolder è responsabile della creazione di una view del recyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val larghezza_carta = parent.width / boardSize.getWidth() - (2 * margin_size)
        val altezza_carta = parent.height / boardSize.getHeight() - (2 * margin_size)
        val lunghezza_lato_carta = min(larghezza_carta,altezza_carta)

        //Ottengo la view della carta
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card,parent,false)

        //Setto la lunghezza del lato della carta in base all altezza e lunghezza della carta ottenute
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.width = lunghezza_lato_carta
        layoutParams.height = lunghezza_lato_carta
        //Setto il margine sui 4 lati del layout della carta
        layoutParams.setMargins(margin_size, margin_size, margin_size, margin_size)
        return ViewHolder(view)
    }

    //Quanti elementi ci sono nella nostra recyclerView
    override fun getItemCount() = boardSize.numCards

    //onBindViewHolder è responsabile di prendere gli elementi che si trovano in "position" e legarli al viewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        //Riferimento al pulsante dell immagine
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val attributeMemoryCard = cards[position]

            if (attributeMemoryCard.isFaceUp) {
                //caso in cui abbiamo un immagine da renderizzare al posto di una resource id
                if (attributeMemoryCard.imageUrl != null) {
                    Picasso.get().load(attributeMemoryCard.imageUrl).into(imageButton)                  //tramite Picasso carico all interno dell image button l url relativo della custom image
                } else {
                    imageButton.setImageResource(attributeMemoryCard.identifier)
                }
            } else {
                //caso della carta faceDown in cui mostro il launcher background
                imageButton.setImageResource(R.drawable.ic_question_mark)
            }

            imageButton.setOnClickListener{
                Log.i(TAG,"Cliccato il tasto in posizione $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
