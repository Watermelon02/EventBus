package xigua.eventbus

import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import xigua.eventbus.poster.BackgroundPoster
import xigua.eventbus.poster.MainThreadPoster
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * description ： TODO:类的作用
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/9 09:11
 */
@RequiresApi(Build.VERSION_CODES.O)//反射需要的版本
class MyEventBus() {
    @Volatile
    private var eventBus: MyEventBus? = null

    //发送事件时根据EventType在这里获取方法；使用CopyOnWriteArrayList，可能是为了防止多线程时重复写入相同方法
    private val subscribeMethodByEventType =
        HashMap<Any, CopyOnWriteArrayList<SubscriberMethod>>()

    //在unRegister时使用,获取subscriber对应的所有eventType
    private val typesBySubscriber =
        HashMap<Any, ArrayList<Any>>()
    private lateinit var currentThreadState: PostingThreadState
    val executorService = ThreadPoolExecutor(0, Integer.MAX_VALUE,
        60L, TimeUnit.SECONDS,
        SynchronousQueue<Runnable>()
    )//eventBus的默认线程池，在backgroundMode和AsyncMode会用到
    private val mainThreadPoster = MainThreadPoster(this, Looper.getMainLooper())
    private val backgroundPoster = BackgroundPoster(this)

    init {
        //ThreadLocal每个线程会操作自己的本地内存中的currentThreadState，可以避免因为多线程造成的同步问题
        ThreadLocal.withInitial { currentThreadState = PostingThreadState(isMainThread(), false) }
    }


    fun getDefault(): MyEventBus {//DCL,这里只实现了通过getDefault来获取EventBus的方法，所以各种配置都是默认固定的配置
        if (eventBus == null) {
            synchronized(this) {
                return MyEventBus()
            }
        } else return eventBus!!
    }

    fun register(subscriber: Any) {
        val methods = findSubscriberMethods(subscriber)
        for (method in methods) subscribe(subscriber, method)
    }

    private fun subscribe(subscriber: Any, subscriberMethod: SubscriberMethod) {
        val eventType = subscriberMethod.eventType
        //从缓存中根据EventType获取订阅方法ArrayList，如果不存在则创建并放入缓存
        var subscriberMethods = subscribeMethodByEventType[eventType]
        if (subscriberMethods == null) {
            subscriberMethods = CopyOnWriteArrayList()
            subscribeMethodByEventType[eventType] = subscriberMethods
        }
        subscriberMethods.add(subscriberMethod)
        //从缓存中根据subscriber获取订阅方法ArrayList，如果不存在则创建并放入缓存
        var subscribedEvents = typesBySubscriber[subscriber]
        if (subscribedEvents == null) {
            subscribedEvents = ArrayList()
        }
        subscribedEvents.add(eventType)
    }

    private fun findSubscriberMethods(subscriber: Any): ArrayList<SubscriberMethod> {
        val subscriberMethods = ArrayList<SubscriberMethod>()//带有Subscribe注解的方法array
        val methods = subscriber::class.java.declaredMethods
        for (method in methods) {
            if (method.getAnnotation(Subscribe::class.java) != null) {
                //通过SubscriberMethod来存储相关信息，降低反射带来的性能损耗
                val eventType = method.parameters[0]::class
                val threadMode = method.getAnnotation(Subscribe::class.java)!!.threadMode
                val subscriberMethod = SubscriberMethod(subscriber, method, eventType, threadMode)
                subscriberMethods.add(subscriberMethod)
            }
        }
        return subscriberMethods
    }

    fun post(event: Any) {
        val eventQueue = currentThreadState.eventQueue
        eventQueue.add(event)
        if (!currentThreadState.isPosting) {
            currentThreadState.isMainThread = isMainThread()
            currentThreadState.isPosting = true
            while (eventQueue.isNotEmpty()) {
                try {
                    while (eventQueue.isNotEmpty()) {
                        postSingleEvent(eventQueue[0], currentThreadState)
                        eventQueue.remove(0)
                    }
                } finally {
                    currentThreadState.isPosting = false
                    currentThreadState.isMainThread = false
                }
            }
        }
    }

    private fun postSingleEvent(event: Any, currentThreadState: PostingThreadState) {
        val subscriberMethods = subscribeMethodByEventType[event::class.java]
        subscriberMethods?.let {
            for (subscriberMethod in subscriberMethods) {
                runByThreadMode(subscriberMethod, currentThreadState.isMainThread, event)
            }
        }
    }

    private fun runByThreadMode(
        subscriberMethod: SubscriberMethod,
        isMainThread: Boolean,
        event: Any
    ) {
        when (subscriberMethod.threadMode) {
            ThreadMode.MAIN -> {
                if (isMainThread) {
                    invokeSubscriber(subscriberMethod.method, subscriberMethod.subscriber, event)
                } else {
                    mainThreadPoster.enqueue(subscriberMethod, event)
                }
            }
            ThreadMode.POSTING -> {
            }
            ThreadMode.BACKGROUND -> {
                if (isMainThread) {
                    backgroundPoster.enqueue(subscriberMethod,event)
                }else{
                    invokeSubscriber(subscriberMethod.method, subscriberMethod.subscriber, event)
                }
            }
            ThreadMode.ASYNC -> {
            }
        }
    }

    fun invokeSubscriber(method: Method, subscriber: Any, event: Any) {
        method.invoke(subscriber, event)
    }

    private fun unRegister() {

    }

    private fun isMainThread(): Boolean {//根据Looper信息判断当前线程是否是主线程
        return Looper.getMainLooper() == Looper.myLooper()
    }

    inner class PostingThreadState(var isMainThread: Boolean, var isPosting: Boolean) {
        /**
         * 用来存储eventBus当前线程的信息
         * 内部保存一个eventQueue*/
        val eventQueue = ArrayList<Any>()
    }
}

annotation class Subscribe(val threadMode: ThreadMode)//订阅方法注解
enum class ThreadMode { MAIN, POSTING, BACKGROUND, ASYNC }//订阅方法的线程模式
class SubscriberMethod(
    val subscriber: Any,
    val method: Method,
    val eventType: Any,
    val threadMode: ThreadMode
)
