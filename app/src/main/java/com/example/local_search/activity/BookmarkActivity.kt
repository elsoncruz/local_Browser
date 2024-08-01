package com.example.local_search.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.local_search.adapter.BookmarkAdapter
import com.example.local_search.databinding.ActivityBookmarkBinding

class BookmarkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding=ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rbBookmark.setItemViewCacheSize(5)
        binding.rbBookmark.hasFixedSize()
        binding.rbBookmark.layoutManager=LinearLayoutManager(this)
        binding.rbBookmark.adapter=BookmarkAdapter(this, isActivity = true)
    }
}