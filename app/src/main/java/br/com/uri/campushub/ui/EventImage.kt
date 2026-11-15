package br.com.uri.campushub.ui

import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView.loadEventImage(url: String, fallback: Int) {
    Glide.with(this).load(url.trim().takeIf(String::isNotBlank))
        .placeholder(fallback).error(fallback).centerCrop().into(this)
}
