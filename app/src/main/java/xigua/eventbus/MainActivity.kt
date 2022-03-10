package xigua.eventbus

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {
    lateinit var text: TextView
    lateinit var button: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        text = findViewById(R.id.text1)
        button = findViewById(R.id.button)
        val eventBus = MyEventBus.getDefault()
        eventBus.register(this)
        button.setOnClickListener {
            Log.d("testTag", "1:${Thread.activeCount()}")
            Thread(object :Runnable{
                override fun run() {
                    eventBus.post(MessageEvent("clicked"))
                }
            }).start()
        }
    }

    @Subscribe(ThreadMode.MAIN)
    fun click(messageEvent: MessageEvent) {
        text.text = messageEvent.string
    }

    class MessageEvent(val string: String)
}