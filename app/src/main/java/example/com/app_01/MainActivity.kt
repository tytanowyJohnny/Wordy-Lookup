package example.com.app_01

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), View.OnTouchListener, TextView.OnEditorActionListener {


    /*
    ButterKnife
     */
    @BindView(R.id.mainInput)
    lateinit var mainInput: TextInputLayout

    @BindView(R.id.lookupButton)
    lateinit var lookupButton: Button

    @BindView(R.id.coordinatorLayout)
    lateinit var coordinatorLayout: CoordinatorLayout

    @BindView(R.id.definitionTextView)
    lateinit var definitionTextView: TextView

    @BindView(R.id.contentProgressBar)
    lateinit var progressBar: ProgressBar

    /*
    Variables
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // ButterKnife
        ButterKnife.bind(this)

        coordinatorLayout.setOnTouchListener(this)
        mainInput.editText?.setOnEditorActionListener(this)

        // set up Rx
        RxView.clicks(lookupButton).subscribe {

            lookupButtonClick(mainInput.editText?.text.toString())

        }

    }

    private fun lookupButtonClick(s: String) {

        // Check internet connection
        if(checkConnection() != null) {

            // show progressbar
            progressBar.visibility = View.VISIBLE

            Observable.just(s)
                    .subscribeOn(Schedulers.io())
                    .map{

                        // network request in the background..
                        oxfordRequest(it)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object: Observer<String> {
                        override fun onComplete() {

                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onNext(t: String) {

                            // hide progressbar
                            progressBar.visibility = View.INVISIBLE

                            if (t == "404") { // word not found error

                                Snackbar.make(coordinatorLayout, resources.getText(R.string.word_not_found_error), Snackbar.LENGTH_SHORT).show()
                            } else {

                                val resultJSON = JSONObject(t)

                                // Get "definition" field from JSON (ugly way)
                                val JSON_results = resultJSON.getJSONArray("results")
                                val JSON_lexical = JSON_results.getJSONObject(0).getJSONArray("lexicalEntries")
                                val JSON_entries = JSON_lexical.getJSONObject(0).getJSONArray("entries")
                                val JSON_senses = JSON_entries.getJSONObject(0).getJSONArray("senses")
                                val JSON_definitions = JSON_senses.getJSONObject(0).getJSONArray("definitions")



                                Log.i("JSON", JSON_definitions.getString(0))

                                var tempString: String = ""

                                for (i in 0..(JSON_definitions.length() - 1)) {

                                    tempString += "${i + 1}: ${JSON_definitions.getString(i)}\n"
                                }

                                definitionTextView.text = tempString

                            }
                        }

                        override fun onError(e: Throwable) {

                            e.printStackTrace()
                            Snackbar.make(coordinatorLayout, resources.getText(R.string.server_error), Snackbar.LENGTH_SHORT).show()

                        }

                    })

        } else {

            Snackbar.make(coordinatorLayout, resources.getText(R.string.internet_error), Snackbar.LENGTH_SHORT).show()
        }

    }

    private fun oxfordRequest(s: String) : String {

        // local vals
        val oxfordAppID: String = "d152ef74"
        val oxfordAppKEY: String = "0f2153750b709f4ffabb848a8eda2448"
        val oxfordAppBASE_URL: String = "https://od-api.oxforddictionaries.com/api/v1/entries/"
        val oxfordAppSourceLang: String = "en"

        Log.i("ASYNC", "Step 1")

        // compose URL with desired word
        val url: URL = URL(oxfordAppBASE_URL + oxfordAppSourceLang +
                "/" + s.toLowerCase())



        Log.i("ASYNC", "Step 2, URL: " + url)

        // Open URL connection
        val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection

        // Set URL connection properties
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Accept", "application/json")
        urlConnection.setRequestProperty("app_id", oxfordAppID)
        urlConnection.setRequestProperty("app_key", oxfordAppKEY)


        Log.i("ASYNC", "Response code: " + urlConnection.responseCode)

        // check for 404 error code
        if(urlConnection.responseCode == 404) // word not found
            return Integer.toString(urlConnection.responseCode)

        val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))

        val sBuilder = StringBuilder()

        var line: String?

        do {

            line = reader.readLine()
            sBuilder.append(line + "\n")
        } while (line != null)


        return sBuilder.toString()

    }

    private fun View.hideKeyboard() {

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken,0)
    }

    private fun checkConnection() : NetworkInfo? {

        val cm: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetInfo: NetworkInfo? = cm.activeNetworkInfo

        return activeNetInfo
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_about -> {

                val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)

                // set title
                alertDialogBuilder.setTitle(resources.getText(R.string.about_title))

                // set content
                alertDialogBuilder
                        .setMessage(resources.getText(R.string.about_content))
                        .setCancelable(true)

                // create alert dialog
                val alertDialog: AlertDialog = alertDialogBuilder.create()

                // show it
                alertDialog.show()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {

        if(p0 == mainInput.editText) {

            p0?.hideKeyboard()

            val tempText = mainInput.editText?.text.toString()

            lookupButtonClick(tempText)
        }
        return true
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {

        p0?.performClick()

        // Hide keyboard from windows
        p0?.hideKeyboard()

        return true
    }


}

