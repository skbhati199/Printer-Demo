package com.fluper.printerdemo

import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintJob
import android.print.PrintManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*


class HomeActivity : AppCompatActivity() {

    private val IMAGE_REQUEST_ID = 1337
    private var prose: EditText? = null
    private var wv: WebView? = null
    private var mgr: PrintManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        prose=findViewById(R.id.prose);
        mgr=getSystemService(PRINT_SERVICE) as PrintManager
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.bitmap -> {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*")
                startActivityForResult(i, IMAGE_REQUEST_ID)
                return true
            }
            R.id.web -> {
                printWebPage()
                return true
            }
            R.id.report -> {
                printReport()
                return true
            }
            R.id.pdf -> {
                print(
                    "Test PDF",
                    PdfDocumentAdapter(applicationContext),
                    PrintAttributes.Builder().build()
                )
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_ID
            && resultCode == RESULT_OK
        ) {
            try {
                val help = PrintHelper(this)
                help.scaleMode = PrintHelper.SCALE_MODE_FIT
                help.printBitmap("Photo!", data?.data!!)
            } catch (e: FileNotFoundException) {
                Log.e(
                    javaClass.simpleName, "Exception printing bitmap",
                    e
                )
            }
        }
    }




    private fun printWebPage() {
        val print = prepPrintWebView("Web Page")
        print?.loadUrl("https://commonsware.com/Android")
    }

    private fun printReport() {
        val tmpl: Template =
            Mustache.compiler().compile("Report Body")
        val print = prepPrintWebView("tps_report")
        print?.loadData(
            tmpl.execute(
                TpsReportContext(
                    prose!!.text
                        .toString()
                )
            ),
            "text/html; charset=UTF-8", null
        )
    }

    private fun prepPrintWebView(name: String): WebView? {
        val result = getWebView()
        result?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                print(
                    name, view.createPrintDocumentAdapter(),
                    PrintAttributes.Builder().build()
                )
            }
        }
        return result
    }

    private fun getWebView(): WebView? {
        if (wv == null) {
            wv = WebView(this)
        }
        return wv
    }

    private fun print(
        name: String, adapter: PrintDocumentAdapter,
        attrs: PrintAttributes
    ): PrintJob? {
        startService(Intent(this, PrintJobMonitorService::class.java))
        return mgr!!.print(name, adapter, attrs)
    }

    private class TpsReportContext internal constructor(var msg: String) {
        val reportDate: String
            get() = fmt.format(Date())
        val message: String
            get() = msg

        companion object {
            private val fmt: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        }
    }
}
