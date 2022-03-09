package xigua.eventbus

import android.os.Build
import androidx.annotation.RequiresApi
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList

/**
 * description ： TODO:类的作用
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/9 09:11
 */
@RequiresApi(Build.VERSION_CODES.O)//反射需要的版本
class MyEventBus {
    @Volatile
    private var eventBus: MyEventBus? = null

    //发送事件时根据EventType在这里获取方法；使用CopyOnWriteArrayList，可能是为了防止多线程时重复写入相同方法
    private val subscribeMethodByEventType =
        HashMap<Any, CopyOnWriteArrayList<SubscriberMethod>>()

    //在unRegister时使用,获取subscriber对应的所有eventType
    private val typesBySubscriber =
        HashMap<Any, ArrayList<Any>>()
    /*private val currentThreadState:PostingThreadState*/

    /*constructor() {
        currentThreadState = PostingThreadState()
    }*/


    fun getDefault(): MyEventBus {//DCL
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
                val subscriberMethod = SubscriberMethod(method, eventType, threadMode)
                subscriberMethods.add(subscriberMethod)
            }
        }
        return subscriberMethods
    }

    fun post(event: Any) {
        val eventType = event::class.java
        val subscribedMethods = subscribeMethodByEventType[eventType]
        if (subscribedMethods == null) {
            throw Exception("MyEventBus.post: No such eventType error")
        } else {
            for (method in subscribedMethods) {
                runByThreadMode(method.method, method.threadMode, event)
            }
        }
    }

    private fun runByThreadMode(method: Method,threadMode: ThreadMode, event: Any) {
        when (threadMode) {
            ThreadMode.MAIN -> invokeOnMainThread(method,event)
            ThreadMode.POSTING -> {
            }
            ThreadMode.BACKGROUND -> {
            }
            ThreadMode.ASYNC -> {
            }
        }
    }

    private fun invokeOnMainThread(method: Method, event: Any) {

    }

    fun unRegister() {

    }

    inner class PostingThreadState(var isMainThread:Boolean,var isPosting:Boolean){
        /**
         * 用来存储eventBus当前线程的信息
         * 内部保存一个eventQueue*/
        val eventQueue = ArrayList<SubscriberMethod>()
    }
}

annotation class Subscribe(val threadMode: ThreadMode)//订阅方法注解
enum class ThreadMode { MAIN, POSTING, BACKGROUND, ASYNC }//订阅方法的线程模式
class SubscriberMethod(val method: Method, val eventType: Any, val threadMode: ThreadMode)
