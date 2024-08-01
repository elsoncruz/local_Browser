package com.example.local_search.fragment

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.local_search.R
import com.example.local_search.activity.BookmarkActivity
import com.example.local_search.activity.MainActivity
import com.example.local_search.activity.changeTab
import com.example.local_search.activity.check
import com.example.local_search.adapter.BookmarkAdapter
import com.example.local_search.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view=inflater.inflate(R.layout.fragment_home, container, false)
        binding= FragmentHomeBinding.bind(view)
        return view
    }

    override fun onResume() {
        super.onResume()

        val mainActivityRef=requireActivity() as MainActivity
        MainActivity.tabsBtn.text=MainActivity.tabsList.size.toString()
        MainActivity.tabsList[MainActivity.myPager.currentItem].name="Home"
        mainActivityRef.binding.topSearch.text=SpannableStringBuilder("")
        binding.searchView.setQuery("",false)
        mainActivityRef.binding.webIcon.setImageResource(R.drawable.search_24)

        mainActivityRef.binding.ref.visibility=View.GONE

        binding.searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(result: String?): Boolean {
                if(check(requireContext()))
                    changeTab(result!!, BsFragment(result))
                else
                    Snackbar.make(binding.root,"First Connect Your Internet! \uD83D\uDE0E",3000).show()
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean = false
        })

        // replace with your URL
        mainActivityRef.binding.bot.setOnClickListener {
            val bot = "https://urkwub4cfgjdggvrchrbse.streamlit.app/"
            if(check(requireContext()))
                changeTab(bot,
                    BsFragment(bot)
                )
            else
                Snackbar.make(binding.root,"First Connect Your Internet! \uD83D\uDE0E",3000).show()
        }

        mainActivityRef.binding.goBtn.setOnClickListener {
            if(check(requireContext()))
                changeTab(mainActivityRef.binding.topSearch.text.toString(),
                    BsFragment(mainActivityRef.binding.topSearch.text.toString())
                )
            else
                Snackbar.make(binding.root,"First Connect Your Internet! \uD83D\uDE0E",3000).show()
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(5)
        binding.recyclerView.layoutManager= GridLayoutManager(requireContext(),5)
        binding.recyclerView.adapter= BookmarkAdapter(requireContext())

        if (MainActivity.bookmarkList.size<1)
            binding.viewAllBtn.visibility=View.GONE
        binding.viewAllBtn.setOnClickListener {
            startActivity(Intent(requireContext(),BookmarkActivity::class.java))
        }

    }

}