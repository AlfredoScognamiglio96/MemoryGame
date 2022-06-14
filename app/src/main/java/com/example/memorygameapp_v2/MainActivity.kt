package com.example.memorygameapp_v2

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygameapp_v2.icons_and_utils.EXTRA_BOARD_SIZE
import com.example.memorygameapp_v2.icons_and_utils.EXTRA_GAME_NAME
import com.example.memorygameapp_v2.modelli.AttributeMemoryGame
import com.example.memorygameapp_v2.modelli.BoardSize
import com.example.memorygameapp_v2.modelli.UserImageList
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 100
    }

    //Lateinit poichè non verranno create dal costruttore del MainActivity ma dal onCreate Method
    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var mosse_textView: TextView

    private val db = Firebase.firestore
    private var gameName: String? = null                              //null poichè le icone del gioco inizialmente sono di default
    private var customGameImages: List<String>? = null                      //null poichè nel caso base questa lista sarà sempre vuota
    private lateinit var memoryGame: AttributeMemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private var boardSize: BoardSize = BoardSize.FACILE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        mosse_textView = findViewById(R.id.mosse_textView)

        //Funzione che contiene le info relative al gioco e al suo livello
        setupBoard()
    }

    private fun setupBoard() {

        supportActionBar?.title = gameName ?: getString(R.string.app_name)                      //cambio titolo activity se sto giocando ad un customGame
        when (boardSize) {
            BoardSize.FACILE -> {
                mosse_textView.text = "Facile 4 x 2"
            }
            BoardSize.MEDIO -> {
                mosse_textView.text = "Medio 6 x 3"
            }
            BoardSize.DIFFICILE -> {
                mosse_textView.text = "Difficile 6 x 4"
            }
        }

        memoryGame = AttributeMemoryGame(boardSize, customGameImages)
        adapter = MemoryBoardAdapter(this,boardSize,memoryGame.cards,object: MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })

        //MemoryBoardAdapter classe che implementa la logica dell adattatore del recycler
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())                                 //2 = colonne presenti
    }

    //Vengo notificato quando l utente clicca sul tasto del menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.gameWon()) {
                    showAlertDialog("Esci dalla partita corrente?", null, View.OnClickListener {
                        setupBoard()
                    })
                } else {
                    //vado ad impostare il gioco di nuovo
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_gioco_personalizzato -> {
                showCreationDialog()
                return true
            }
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Crea il tuo Memory Game", boardSizeView, View.OnClickListener {
            //Scelta del nuovo valore per la grandezza
            val sceltaboardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbFacile -> BoardSize.FACILE
                R.id.rbMedio -> BoardSize.MEDIO
                else -> BoardSize.DIFFICILE
            }
            //Nuova activity del gioco personalizzato
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, sceltaboardSize)
            //Tramite StartActivityForResult recupero dei dati dall activity che ho lanciato
            startActivityForResult(intent, REQUEST_CODE)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null){
                Log.e(TAG,"Nome gioco null!")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    //funzione creata per permettere all utente di inserire il nome del customGame da scaricare
    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Ottieni memory game",boardDownloadView, View.OnClickListener {
            //qui ottengo il text del custom game name
            val scaricaGioco = boardDownloadView.findViewById<EditText>(R.id.scaricaGioco)
            val gameToDownload = scaricaGioco.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }


    //funzione per effettuare query a Firestore ed ottenere il set di immagini corrispondente
    private fun downloadGame(customGameName: String) {
            db.collection("giochi").document(customGameName).get().addOnSuccessListener { document ->
                val userImageList = document.toObject(UserImageList::class.java)
                //caso in cui userImageList è null oppure l attributo di userImageList è null
                if (userImageList?.images == null) {
                    Log.e(TAG,"I dati di gioco ottenuti da Firestore sono invalidi")
                    Snackbar.make(clRoot,"Nessun gioco trovato con nome '$customGameName'",Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                //a questo punto siamo riusciti a trovare il gioco ricercato
                val numCards = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                customGameImages = userImageList.images                                 //query a Firebase per ottenere le immagini
                //Precaricamento con Picasso dell url delle immagini
                for (imageUrl in userImageList.images){
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(clRoot,"Stai giocando a '$customGameName'!",Snackbar.LENGTH_LONG).show()
                gameName = customGameName
                setupBoard()
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Errore nel recupero gioco",exception)
            }
    }

    //Finestra opzione scelta nuovo size
    private fun showNewSizeDialog() {
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Scegli la nuova grandezza", boardSizeView, View.OnClickListener {
            //Scelta del nuovo valore per la grandezza
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbFacile -> BoardSize.FACILE
                R.id.rbMedio -> BoardSize.MEDIO
                else -> BoardSize.DIFFICILE
            }
            //resetto i valori nel caso in cui l utente torni a giocare il defaultGame
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    //Finestra che appare quando clicco sul tasto per refreshare il gioco
    private fun showAlertDialog(title: String, view: View?,positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    //Metodo che gestisce il flip della carta
    private fun updateGameWithFlip(position: Int) {

        if (memoryGame.gameWon()) {
            Snackbar.make(clRoot,"Hai già vinto!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            //Notifica l user della mossa invalida
            Snackbar.make(clRoot,"Mossa non valida!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.flipCard(position)) {

            if (memoryGame.gameWon()) {
                Snackbar.make(clRoot,"Congratulazione,hai vinto!",Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.RED, Color.WHITE)).oneShot()
            }
        }

        mosse_textView.text = "Mosse: ${memoryGame.getNumMoves()}"  //Aggiorno il text del contatore delle mosse
        adapter.notifyDataSetChanged()                              //Notifico l adapter che il contenuto di quello che viene mostrato è cambiato
    }
}