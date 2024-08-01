package com.example.local_search.activity


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.local_search.R
import com.example.local_search.activity.MainActivity.Companion.myPager
import com.example.local_search.activity.MainActivity.Companion.tabsBtn
import com.example.local_search.adapter.TabAdapter
import com.example.local_search.databinding.ActivityMainBinding
import com.example.local_search.databinding.BookmarkDialogBinding
import com.example.local_search.databinding.MoreFsBinding
import com.example.local_search.databinding.TabsViewBinding
import com.example.local_search.fragment.BsFragment
import com.example.local_search.fragment.HomeFragment
import com.example.local_search.model.Bookmark
import com.example.local_search.model.Tab
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("DEPRECATION")
class  MainActivity : AppCompatActivity() {
    private var printJob:PrintJob?=null
    lateinit var binding:ActivityMainBinding
    companion object{
        var tabsList:ArrayList<Tab> = ArrayList()
        private var isFullscreen:Boolean=true
        var isDesktopSite:Boolean=false
        var bookmarkList:ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex:Int=-1
        lateinit var myPager:ViewPager2
        lateinit var tabsBtn:MaterialTextView
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        getAllBookmarks()


        tabsList.add(Tab("Home",HomeFragment()))

        binding.mypage.adapter=TabsAdapter(supportFragmentManager,lifecycle)
        binding.mypage.isUserInputEnabled=false
        myPager=binding.mypage
        tabsBtn=binding.tabsBtn
        initializeView()
        changeFullscreen(enable = true)

        binding.textBtn.setOnClickListener {
            val intent = Intent(this, TextActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var frag: BsFragment?=null
        try {
            frag= tabsList[binding.mypage.currentItem].fragment as BsFragment
        } catch (e:Exception){}
        when{
            frag?.binding?.webView?.canGoBack()==true->frag.binding.webView.goBack()
            binding.mypage.currentItem !=0->{
                tabsList.removeAt(binding.mypage.currentItem)
                binding.mypage.adapter?.notifyDataSetChanged()
                binding.mypage.currentItem= tabsList.size-1
            }
            else->super.onBackPressed()
        }
    }

    private inner class TabsAdapter(fa:FragmentManager,lc:Lifecycle):FragmentStateAdapter(fa,lc){
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }

    private fun initializeView(){
        binding.tabsBtn.setOnClickListener {
            val viewTabs=layoutInflater.inflate(R.layout.tabs_view,binding.root,false)
            val bindingTabs= TabsViewBinding.bind(viewTabs)

            val dialogTabs=MaterialAlertDialogBuilder(this,R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self,_->
                    changeTab("Home",HomeFragment())
                    self.dismiss()
                }
                .setNeutralButton("Google"){self,_->
                    changeTab("Google",BsFragment(urlNew = "www.google.com"))
                    self.dismiss()
                }
                .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager=LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter=TabAdapter(this,dialogTabs)

            dialogTabs.show()

            val pBtn=dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn=dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps=false
            nBtn.isAllCaps=false

            pBtn.setTextColor(Color.BLACK)
            nBtn.setTextColor(Color.BLACK)

            pBtn.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources,R.drawable.ic_home1, theme)
                ,null,null,null)

            nBtn.setCompoundDrawablesWithIntrinsicBounds(
                ResourcesCompat.getDrawable(resources,R.drawable.ic_add, theme)
                ,null,null,null)
        }
        binding.settingBtn.setOnClickListener {

            var frag: BsFragment?=null
            try {
                frag= tabsList[binding.mypage.currentItem].fragment as BsFragment
            }catch (e:Exception){}
            val view=layoutInflater.inflate(R.layout.more_fs,binding.root,false)
            val dialogBinding=MoreFsBinding.bind(view)

            val dialog=MaterialAlertDialogBuilder(this).setView(view).create()

            dialog.window?.apply {
                attributes.gravity=Gravity.BOTTOM
                attributes.y=50
                setBackgroundDrawable(ColorDrawable(0xFFFFFF))
            }

            dialog.show()

            if(isFullscreen){
                dialogBinding.fullscreenBtn.apply {
                    setIconResource(R.color.white)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
            }

            frag?.let {
                bookmarkIndex=isBookmarked(it.binding.webView.url!!)
                if(bookmarkIndex!=-1){
                    dialogBinding.bookmarkBtn.apply {
                        setIconResource(R.color.white)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    }
                }
            }

            if(isDesktopSite){
                dialogBinding.desktopBtn.apply {
                    setIconResource(R.color.white)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
            }


            dialogBinding.backBtn.setOnClickListener{
                onBackPressed()
            }
            dialogBinding.forwardBtn.setOnClickListener {
                frag?.apply {
                    if(binding.webView.canGoForward())
                        binding.webView.goForward()
                }
            }

            dialogBinding.saveBtn.setOnClickListener {
                dialog.dismiss()
                if(frag!=null)
                    saveAsPdf(web=frag.binding.webView)
                else Snackbar.make(binding.root,"First Open A WebPage",3000).show()

            }

            dialogBinding.fullscreenBtn.setOnClickListener {
                it as MaterialButton

                isFullscreen =if (isFullscreen) {
                    changeFullscreen(enable = false)
                    it.setIconResource(R.color.white)
                    it.setTextColor(ContextCompat.getColor(this, R.color.white))
                    false
                }
                else {
                    changeFullscreen(enable = true)
                    it.setIconResource(R.color.black)
                    it.setTextColor(ContextCompat.getColor(this, R.color.black))
                    true
                }
            }

            dialogBinding.desktopBtn.setOnClickListener {
                it as MaterialButton
                frag?.binding?.webView?.apply {
                    isDesktopSite =if (isDesktopSite) {
                        settings.userAgentString=null
                        it.setIconResource(R.color.white)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                        false
                    }
                    else {
                        settings.userAgentString="Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
                        settings.useWideViewPort=true
                        evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content',"+"'width=1024px, initial-scale='+(document.documentElement.clientWidth/1024));",null)

                        it.setIconResource(R.color.black)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                        true
                    }
                    reload()
                    dialog.dismiss()
                }

            }

            dialogBinding.bookmarkBtn.setOnClickListener {
                frag?.let {

                    if (bookmarkIndex==-1){
                        val viewB=layoutInflater.inflate(R.layout.bookmark_dialog,binding.root,false)
                        val bBinding= BookmarkDialogBinding.bind(viewB)
                        val dialogB=MaterialAlertDialogBuilder(this)
                            .setTitle("Add Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Add"){self,_->
                                try {
                                    val array= ByteArrayOutputStream()
                                    it.webicon?.compress(Bitmap.CompressFormat.PNG,100,array)
                                    bookmarkList.add(
                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(),url=it.binding.webView.url!!,array.toByteArray() ))

                                }catch (e:Exception){
                                    bookmarkList.add(
                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(),url=it.binding.webView.url!!))
                                }
                                self.dismiss()}
                            .setNegativeButton("Cancel"){self,_->self.dismiss()}
                            .setView(viewB).create()
                        dialogB.show()
                        bBinding.bookmarkTitle.setText(it.binding.webView.title)
                    }else{
                        val dialogB=MaterialAlertDialogBuilder(this)
                            .setTitle("Remove Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Remove"){self,_->
                                bookmarkList.removeAt(bookmarkIndex)
                                self.dismiss()}
                            .setNegativeButton("Cancel"){self,_->self.dismiss()}
                            .create()
                        dialogB.show()
                    }
                }

                dialog.dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(binding.root,"Successful ->${it.info.label}",4000).show()
                it.isFailed -> Snackbar.make(binding.root,"Failed ->${it.info.label}",4000).show()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveAsPdf(web:WebView){
        val pm=getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName="${URL(web.url).host}_${SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH).format(Calendar.getInstance().time)}"
        val printAdapter=web.createPrintDocumentAdapter(jobName)
        val printAttributes=PrintAttributes.Builder()
        printJob=pm.print(jobName, printAdapter, printAttributes.build())
    }

    private fun changeFullscreen(enable:Boolean){
        if (enable){
            WindowCompat.setDecorFitsSystemWindows(window,false)
            WindowInsetsControllerCompat(window,binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window,true)
            WindowInsetsControllerCompat(window,binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url:String):Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if (bookmark.url==url) return index
        }
        return -1
    }
    @SuppressLint("CommitPrefEdits")
    fun saveBookmarks(){
        //for storing bookmark data using share preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data=GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply ()
    }
    fun  getAllBookmarks() {
        //for getting bookmark data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)
        if (data != null) {
            val list: ArrayList<Bookmark> = GsonBuilder().create()
                .fromJson(data, object : TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }
    }
}
@SuppressLint("NotifyDataSetChanged")
fun changeTab(url:String, fragment: Fragment,isBackground: Boolean = false){
    MainActivity.tabsList.add(Tab(name = url,fragment=fragment))
    myPager.adapter?.notifyDataSetChanged()
    tabsBtn.text=MainActivity.tabsList.size.toString()

    if(!isBackground)myPager.currentItem= MainActivity.tabsList.size-1
}

@SuppressLint("ObsoleteSdkInt")
fun check(context: Context):Boolean{
    val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
        val network=connectivityManager.activeNetwork ?:return false
        val activityNetwork=connectivityManager.getNetworkCapabilities(network)?:return false

        return when{
            activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true
            activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->true
            else->false
        }
    }else{
        @Suppress("DEPRECATION")val networkInfo=
            connectivityManager.activeNetworkInfo?:return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected



    }
}