package xigua.eventbus.poster

import android.os.Build
import androidx.annotation.RequiresApi
import xigua.eventbus.MyEventBus
import xigua.eventbus.PendingPost
import xigua.eventbus.SubscriberMethod
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * description ： TODO:类的作用
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/10 10:53
 */
class BackgroundPoster(val eventBus: MyEventBus) : Poster, Runnable {
    private val pendingPostQueue = LinkedBlockingQueue<PendingPost>()
    private var isRunning = false

    override fun enqueue(subscriberMethod: SubscriberMethod, event: Any) {
        synchronized(this) {
            val pendingPost = PendingPost(subscriberMethod, event)
            pendingPostQueue.add(pendingPost)
            if (!isRunning) {
                isRunning = true
                /**每次调用enqueue，就发送一次Message来通知主线程
                 * obtainMessage()可以复用Message,提高运行效率*/
                eventBus.executorService.execute(this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun run() {
        while (pendingPostQueue.isNotEmpty()) {
            while (true) {//不断取出pendingPost直到取完
                val pendingPost = pendingPostQueue.poll(1000, TimeUnit.MILLISECONDS)
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
}