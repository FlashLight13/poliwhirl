package org.example.poliwhirl

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.antonpotapov.Poliwhirl
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.ImageViewTarget
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    val poliwhirl = Poliwhirl()
    var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageUrl.adapter = ArrayAdapter(this, R.layout.v_text, arrayOf(
                ImageData("Instagram", "https://instagram-brand.com/wp-content/uploads/2016/11/app-icon2.png"),
                ImageData("Habrahabr", "https://pbs.twimg.com/profile_images/583482649924218881/HbQGtx57.jpg"),
                ImageData("Netflix", "https://cdn.vox-cdn.com/thumbor/PgkEvDUnSUA_85lvxA3Hu4dxrs8=/800x0/filters:no_upscale()/cdn.vox-cdn.com/uploads/chorus_asset/file/6678919/10478570_10152214521608870_2744465531652776073_n.0.png"),
                ImageData("Google Maps", "https://vignette.wikia.nocookie.net/logopedia/images/e/e1/Googlemapslogo2014.png/revision/latest?cb=20150309221525"))
        )
        imageUrl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Glide.with(this@MainActivity)
                        .asBitmap()
                        .load((parent!!.adapter.getItem(position) as ImageData).url)
                        .into(object : ImageViewTarget<Bitmap>(picture) {
                            override fun setResource(resource: Bitmap?) {
                                this@MainActivity.bitmap = resource
                                this.view.setImageBitmap(resource)
                            }
                        })
            }
        }

        borderSizeDivMul.setText(poliwhirl.verticalBorderSizeMul.toString())
        borderSizeDivMul.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                poliwhirl.setBorderSizeDivideMultiplier(Math.max(1, parse(s.toString())))
            }
        })

        accuracy.setText(poliwhirl.accuracy.toString())
        accuracy.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                poliwhirl.setAccuracy(Math.max(1, parse(s.toString())))
            }
        })

        minAvailableDistance.setText(poliwhirl.minAvailableDistance.toString())
        minAvailableDistance.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable?) {
                poliwhirl.setMinAvailableColorDistance(Math.max(0, parse(s.toString())))
            }
        })

        calculateColor.setOnClickListener { updatePoliwhirl() }
        clearColor.setOnClickListener { pictureBackground.setBackgroundColor(Color.TRANSPARENT) }
    }

    private fun updatePoliwhirl() {
        val bitmap = bitmap
        if (bitmap != null) {

            poliwhirl.generateAsync(bitmap, object : Poliwhirl.Callback {
                override fun foundColor(color: Int) {
                    pictureBackground.setBackgroundColor(color)
                }
            })

            poliwhirl.generateOnExecutor(bitmap, object : Poliwhirl.Callback {
                override fun foundColor(color: Int) {
                    pictureBackground.setBackgroundColor(color)
                }
            }, Executors.newSingleThreadExecutor())
        }
    }

    private fun parse(text: String): Int = try {
        Integer.parseInt(text)
    } catch (e: NumberFormatException) {
        0
    }

    private open class TextWatcherAdapter : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private data class ImageData(val name: String, val url: String) {
        override fun toString(): String = name
    }
}
