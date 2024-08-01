package com.example.local_search.fragment

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Base64
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.example.local_search.R
import com.example.local_search.activity.MainActivity
import com.example.local_search.activity.changeTab
import com.example.local_search.databinding.FragmentBsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.net.URL

@Suppress("DEPRECATION")
class BsFragment(private val urlNew:String) : Fragment() {

    lateinit var binding: FragmentBsBinding
    var webicon:Bitmap?=null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view=inflater.inflate(R.layout.fragment_bs, container, false)
        binding= FragmentBsBinding.bind(view)
        registerForContextMenu(binding.webView)

        binding.webView.apply {
            when{
                URLUtil.isValidUrl(urlNew)->loadUrl(urlNew)
                urlNew.contains(".com" , ignoreCase = true)->loadUrl(urlNew)
                else->loadUrl("https://www.google.co.in/search?q=$urlNew")
            }
        }

        return view
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        val mainRef=requireActivity() as MainActivity
        MainActivity.tabsList[MainActivity.myPager.currentItem].name=binding.webView.url.toString()
        MainActivity.tabsBtn.text=MainActivity.tabsList.size.toString()

        binding.webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            context?.let { context ->
                Thread {
                    try {
                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, mimeType)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        val fileUri = context.contentResolver.insert(collection, values)

                        fileUri?.let {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                val input = URL(url).openStream()

                                // Start the download notification with progress
                                showDownloadNotification(context, fileName)

                                // Download the file
                                input.copyTo(outputStream)

                                // Download complete, update pending status
                                values.clear()
                                values.put(MediaStore.Downloads.IS_PENDING, 0)
                                context.contentResolver.update(it, values, null, null)

                                // Show a notification for download completion
                                showDownloadCompleteNotification(context, fileName)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()

                Toast.makeText(context, "Downloading File..", Toast.LENGTH_LONG).show()
            }
        }



        mainRef.binding.ref.visibility=View.VISIBLE
        mainRef.binding.ref.setOnClickListener {
            binding.webView.reload()
        }

        binding.webView.apply {
            settings.javaScriptEnabled=true
            settings.setSupportZoom(true)
            settings.builtInZoomControls=true
            settings.displayZoomControls=false
            webViewClient= object: WebViewClient(){
                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (MainActivity.isDesktopSite)
                        evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content',"+"'width=1024px, initial-scale='+(document.documentElement.clientWidth/1024));",null)

                }
                //val bot = "https://urkwub4cfgjdggvrchrbse.streamlit.app/"
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainRef.binding.topSearch.text=SpannableStringBuilder(url)
                    MainActivity.tabsList[MainActivity.myPager.currentItem].name=url.toString()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    mainRef.binding.progressBar.progress=0
                    mainRef.binding.progressBar.visibility=View.VISIBLE
                    if (url!!.contains("you",ignoreCase = false)) mainRef.binding.root.transitionToEnd()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainRef.binding.progressBar.visibility=View.GONE
                    binding.webView.zoomOut()
                }

            }
            webChromeClient= object: WebChromeClient(){

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try {
                        mainRef.binding.webIcon.setImageBitmap(icon)
                        webicon = icon
                        MainActivity.bookmarkIndex=mainRef.isBookmarked(view?.url!!)
                        if(MainActivity.bookmarkIndex!=-1){
                            val array=ByteArrayOutputStream()
                            icon!!.compress(Bitmap.CompressFormat.PNG,100,array)
                            MainActivity.bookmarkList[MainActivity.bookmarkIndex].image=array.toByteArray()
                        }
                    }catch (e:Exception){
                        //
                    }
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    binding.webView.visibility=View.GONE
                    binding.customView.visibility=View.VISIBLE
                    binding.customView.addView(view)
                    mainRef.binding.root.transitionToEnd()
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    binding.webView.visibility=View.VISIBLE
                    binding.customView.visibility=View.GONE
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    mainRef.binding.progressBar.progress=newProgress
                }

            }

            binding.webView.setOnTouchListener { _, motionEvent ->
                mainRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }
            binding.webView.reload()
        }
    }

    // Function to show a notification when the download is complete
    private fun showDownloadCompleteNotification(context: Context, fileName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Download Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Download Complete")
            .setContentText("file: $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }
    // Declare notificationBuilder as a global variable
    private var notificationBuilder: NotificationCompat.Builder? = null

    private fun showDownloadNotification(context: Context, fileName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"
        val notificationId = 1

        // Create a notification channel (for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Download Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification builder with progress bar and cancellation action
        notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Downloading: $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)  // Set ongoing to true to make it not dismissible
            .addAction(
                R.drawable.ic_cancel,
                "Cancel",
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, DownloadCancelReceiver::class.java).apply {
                        action = "CANCEL_DOWNLOAD_ACTION"
                        putExtra("notificationId", notificationId)
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(false)  // AutoCancel is set to false to keep the notification after download

        // Create a progress bar
        val progress = 0
        val maxProgress = 100
        notificationBuilder?.setProgress(maxProgress, progress, false)

        // Show the notification
        notificationManager.notify(notificationId, notificationBuilder?.build())
    }




    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).saveBookmarks()
        //for clearing all web History
        binding.webView.apply {
            clearMatches()
            clearHistory()
            clearFormData()
            clearSslPreferences()
            clearCache(true)

            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View,menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val result=binding.webView.hitTestResult
        when(result.type){
            WebView.HitTestResult.IMAGE_TYPE->{
                menu.add("View Image")
                menu.add("Save Image")
                menu.add("Share")
                menu.add("Close")
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE,WebView.HitTestResult.ANCHOR_TYPE->{
                menu.add("Open in new Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }
            WebView.HitTestResult.EDIT_TEXT_TYPE,WebView.HitTestResult.UNKNOWN_TYPE->{}
            else ->{
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val message=Handler().obtainMessage()
        binding.webView.requestFocusNodeHref(message)
        val url=message.data.getString("url")
        val imgUrl= message.data.getString("src")

        when(item.title){
            "Open in New Tab" ->{
                changeTab(url.toString(),BsFragment(url.toString()))
            }
            "Open Tab in Background" ->{
                changeTab(url.toString(),BsFragment(url.toString()),isBackground = true)
            }
            "View Image"->{
                if (imgUrl!=null) {
                    if (imgUrl.contains("base64")) {

                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodeBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.size)
                        val imgView = ShapeableImageView(requireContext())
                        imgView.setImageBitmap(finalImg)

                        val imgDialog =
                            MaterialAlertDialogBuilder(requireContext()).setView(imgView).create()
                        imgDialog.show()

                        imgView.layoutParams.width =
                            Resources.getSystem().displayMetrics.widthPixels
                        imgView.layoutParams.height =
                            (Resources.getSystem().displayMetrics.heightPixels * .75).toInt()
                        imgView.requestLayout()

                        imgDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    } else changeTab(imgUrl, BsFragment(imgUrl))
                }
            }
            "Save Image"->{
                if (imgUrl!=null){
                    if (imgUrl.contains("base64")){

                        val pureBytes=imgUrl.substring(imgUrl.indexOf(",")+1)
                        val  decodeBytes= Base64.decode(pureBytes,Base64.DEFAULT)
                        val finalImg=BitmapFactory.decodeByteArray(decodeBytes,0,decodeBytes.size)

                        MediaStore.Images.Media.insertImage(requireActivity().contentResolver,
                            finalImg,"Image",null)
                        Snackbar.make(binding.root,"Image Save Successfully",3000).show()
                    }
                    else {
                        val request = DownloadManager.Request(Uri.parse(imgUrl))
                            .setTitle("Downloading")
                            .setDescription("Downloading image")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(imgUrl, null, null))
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(request)
                        Snackbar.make(binding.root,"Image Save Successfully",3000).show()
                    }
                }

            }
            "Share"->{
                val tempUrl=url?:imgUrl
                if (tempUrl!=null){
                    if (tempUrl.contains("base64")){

                        val pureBytes=tempUrl.substring(tempUrl.indexOf(",")+1)
                        val  decodeBytes= Base64.decode(pureBytes,Base64.DEFAULT)
                        val finalImg=BitmapFactory.decodeByteArray(decodeBytes,0,decodeBytes.size)

                        val path=MediaStore.Images.Media.insertImage(requireActivity().contentResolver,
                            finalImg,"Image",null)

                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle(url)
                            .setType("image/*")
                            .setStream(Uri.parse(path))
                            .startChooser()
                    }
                else{
                    ShareCompat.IntentBuilder(requireContext()).setChooserTitle(url)
                            .setType("text/plain")
                            .setText(tempUrl)
                            .startChooser()
                    }
                }
                else Snackbar.make(binding.root,"Not a Valid Link!",3000).show()
            }
            "Close" ->{}
        }

        return super.onContextItemSelected(item)
    }
}
class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationId = intent?.getIntExtra("notificationId", 0) ?: 0
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // Cancel the download task or take appropriate action
        // Add your cancellation logic here
    }
}


