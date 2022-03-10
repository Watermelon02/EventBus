package xigua.eventbus.poster

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.RequiresApi
import xigua.eventbus.MyEventBus
import xigua.eventbus.PendingPost
import xigua.eventbus.SubscriberMethod
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * description ： MainThreadMode的Poster，继承了Handler(参数为主线程的looper).通过Handler来让主线程调用方法
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/10 08:28
 */
class MainThreadPoster(val eventBus: MyEventBus, looper: Looper) : Poster, Handler(looper) {
    private val pendingPostQueue = LinkedBlockingQueue<PendingPost>()//使用LinkedBlockingQueue,保证线程同步
    private var isRunning = false

    override fun enqueue(subscriberMethod: SubscriberMethod, event: Any) {
        synchronized(this) {
            val pendingPost = PendingPost(subscriberMethod, event)
            pendingPostQueue.add(pendingPost)
            if (!isRunning) {
                isRunning = true
                /**每次调用enqueue，就发送一次Message来通知主线程
                 * obtainMessage()可以复用Message,提高运行效率*/
                sendMessage(obtainMessage())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        while (true) {//不断取出pendingPost直到取完
            val pendingPost = pendingPostQueue.poll(10, TimeUnit.MILLISECONDS)
            if (pendingPost == null) {
                isRunning = false
                return
            }
            val method = pendingPost.subscriberMethod.method
            val subscriber = pendingPost.subscriberMethod.subscriber
            val event = pendingPost.event
            eventBus.invokeSubscriber(method, subscriber, event)
        }
    }
}