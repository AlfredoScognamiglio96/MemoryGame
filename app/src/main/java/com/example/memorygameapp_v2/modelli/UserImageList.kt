package com.example.memorygameapp_v2.modelli

import com.google.firebase.firestore.PropertyName

data class UserImageList(
    @PropertyName("images") val images: List<String>? = null                //questo sarà l attributo
)

