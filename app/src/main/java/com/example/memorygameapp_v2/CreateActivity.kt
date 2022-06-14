package com.example.memorygameapp_v2

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygameapp_v2.icons_and_utils.*
import com.example.memorygameapp_v2.modelli.BoardSize
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 180
        private const val READ_EXTERNAL_PHOTOS_CODE = 360
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 12
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var modificaNomeGioco: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar


    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private val chosenImage = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        modificaNomeGioco = findViewById(R.id.modificaNomeGioco)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)                                         //Imposto un tasto per tornare alla schermata precedente
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getPairs()
        supportActionBar?.title = "Scegli immagini (0 / $numImagesRequired)"                      //Tramite il ? title viene chiamato solo se la supportActionBar non è nulla (di default è nulla)

        //codice che viene eseguito quando viene cliccato il tasto 'salva'
        btnSave.setOnClickListener{
            saveDataFirebase()                                                                     //metodo che si occupa di salvare tutte le immagini e il nome gioco ad esse associato
        }

        //permetto al nome del gioco di essere al max 12 caratteri
        modificaNomeGioco.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        modificaNomeGioco.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            //metodo che permette ogni di salvare ogni qual volta l utente modifica il nome del gioco
            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = enableSaveButton()
            }
        })

        adapter = ImagePickerAdapter(this, chosenImage, boardSize, object: ImagePickerAdapter.ImageClickListener {
            override fun onPlaceHolderClicked() {

                //Permesso concesso
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    //Qui l utente ha cliccato su uno dei quadrati grigi per selezionare l immagine
                    launchIntentForPhotos()
                } else {
                    //apre una finestra di dialogo dove si chiede se si vuole autorizzare l archiviazione esterna di lettura
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)                                                       //In questo modo garantisco che le dimensioni del recyclerView non cambieranno
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }

    //Indipendentemente dalla risposta al 'requestPermission' otterremo un callback riguardante il risultato delle autorizzazioni della richiesta
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //controllo se il codice di richiesta è uguale al codice di lettura delle foto esterne
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            //controllo i risultati della concessione. Se sono == allora l utente ha concesso l autorizzazione
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this,"Per creare il tuo gioco personalizzato devi fornire l'accesso alle foto",Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //Funzione che gestisce il risultato dell attività del launch activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //controllo se i dati sono validi o meno
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG,"I dati non sono stati recuperati correttamente dall'attività di avvio")
            return
        }
        val selectedUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(TAG,"clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImage.size < numImagesRequired) {
                    chosenImage.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG,"data: $selectedUri")
            chosenImage.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Scegli immagini (${chosenImage.size} / $numImagesRequired)"
        btnSave.isEnabled = enableSaveButton()
    }

    //metodo che si occupa di andare a salvare le immagini e il nome del gioco
    private fun saveDataFirebase() {
        Log.i(TAG,"Salvataggio dati Firebase")
        btnSave.isEnabled = false
        val gameName = modificaNomeGioco.text.toString()
        //Controllo che non venga sovrascritto il gameName creato da qualcun altro
        db.collection("giochi").document(gameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Nome Preso")
                    .setMessage("Esiste già un gioco con il nome '$gameName'.Scegline un altro!")
                    .setPositiveButton("OK",null)
                    .show()
                btnSave.isEnabled = true
            }   else {
                handleImageUploading(gameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG,"Errore riscontrato nel salvataggio di '$gameName'",exception)
            Toast.makeText(this,"Errore riscontrato nel salvataggio di '$gameName'",Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE                                                                               //rendo la progress bar visibile appena inizio a caricare immagini
        var erroreRiscontrato = false
        val uploadedImageUrls = mutableListOf<String>()
        //Itero i vari uri delle foto
        for ((index,photoUri) in chosenImage.withIndex()) {
            val imageByte = getImageByte(photoUri)                                                                           //'imageByte è quello che vado a salvare su firebase'
            val filePath = "immagini/$gameName/${System.currentTimeMillis()} - ${index}.jpg"                                   //creo un path dove salvo le immagini suddividendole in base al nome gioco
            val photoReference = storage.reference.child(filePath)                                                           //riferimento alla posizione dove vado a salvare l immagine
            photoReference.putBytes(imageByte)                                                                               //inserisco i dati dell immagine nella posizione sopra descritta
                //codice eseguito una volta completata l operazione precedente
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG,"Byte caricati: ${photoUploadTask.result?.bytesTransferred}")
                    //una volta completato l upload dell immagine voglio ottenere il corrispondente url del download
                    photoReference.downloadUrl
                    //task per la generazione di una notifica dopo l operazione precendente
                    //e viene invocata ogni qual volta un immagine viene uplodata
                }.addOnCompleteListener{ downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG,"Errore col Firebase Storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Caricamento immagine fallito",Toast.LENGTH_SHORT).show()
                        erroreRiscontrato = true
                        return@addOnCompleteListener
                    }
                    if (erroreRiscontrato) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    //Aggiungo l url dell immagine alla lista contenente tutti gli url delle immagini
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImage.size
                    Log.i(TAG,"Upload $photoUri terminato, num uploaded ${uploadedImageUrls.size}")
                    //quando tutte le immagini che l utente ha scelto sono state caricate
                    //invoco il metodo per aggiungere nome e foto a Firebase
                    if (uploadedImageUrls.size == chosenImage.size) {
                        allImageUploaded(gameName,uploadedImageUrls)
                    }
                }
        }
    }

    //ogni memory game corrisponderà ad 1 documento e quest ultimo sarà contenuto in 1 collezione chiamata 'giochi'
    private fun allImageUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("giochi").document(gameName)
            .set(mapOf("images" to imageUrls))                                      //associo imageUrls ad una chiave chiamata 'images' e questi saranno i dati che verranno inseriti nel documento
            .addOnCompleteListener { gameCreation ->
                pbUploading.visibility = View.GONE
                //errore nella creazione del gioco
                if (!gameCreation.isSuccessful) {
                    Log.e(TAG,"Errore con la creazione del gioco", gameCreation.exception)
                    Toast.makeText(this,"Errore creazione gioco",Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG,"Il gioco $gameName è stato creato con successo")
                AlertDialog.Builder(this)
                    .setTitle("Upload del gioco completato.Gioca a '$gameName'")
                    .setPositiveButton("OK")
                    //dopo che l utente ha cliccato OK, il nome del gioco verrà inviato al main activity
                    {_,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    //metodo che si occupa di effettuare il downgrade della qualità delle immagini
    private fun getImageByte(photoUri: Uri): ByteArray {

        //Se il SO del telefono sta eseguendo Android P o
        // una versione superiore allora il bitmap della foto proverrà dalle righe sottostanti
        val photoBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            //caso di versioni di SO più obsolete
            MediaStore.Images.Media.getBitmap(contentResolver,photoUri)
        }
        val scaledBitmap = BitmapScaler.scaleToFitHeight(photoBitmap, 250)
        val byteOutput = ByteArrayOutputStream()
        //qualità 100 significa che non si ha una riduzione qualitativa mentre 0 indica una forte riduzione qualitativa
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteOutput)
        return byteOutput.toByteArray()
    }


    //Funzione che si occupa di abilitare o disabilitare il tasto Salva
    private fun enableSaveButton(): Boolean {
        //caso in cui l utente non ha selezionato abbastanza immagini
        if (chosenImage.size != numImagesRequired) {
            return false
        }
        //caso in cui il nome del gioco sia vuoto oppure composto da meno di 3 caratteri
        if (modificaNomeGioco.text.isBlank() || modificaNomeGioco.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        //Intent implicito
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"                                              //tramite * comunichiamo che ci interessano solo le foto
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)              //permette la scelta multipla di foto.
                                                                            // N.B: di default Android non concede ad app esterne l accesso ai file sul telefono
        startActivityForResult(Intent.createChooser(intent,"Scegli foto"), PICK_PHOTO_CODE)
    }
}