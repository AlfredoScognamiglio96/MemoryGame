package com.example.memorygameapp_v2

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygameapp_v2.modelli.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImage: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
)
    : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    //Interfaccia contenente metodo che indica il click da parte dell utente sull elemento dell immagine scelta
    interface ImageClickListener {
        fun onPlaceHolderClicked()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)              //Null poichè una volta scelta l immagine, non potrà più essere modificata
        }

        fun bind() {
            ivCustomImage.setOnClickListener {

                imageClickListener.onPlaceHolderClicked()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLenght = min(cardWidth,cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParams.width = cardSideLenght
        layoutParams.height = cardSideLenght
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < chosenImage.size) {
            holder.bind(chosenImage[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount() = boardSize.getPairs()
}
