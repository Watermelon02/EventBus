package xigua.eventbus.poster

import xigua.eventbus.SubscriberMethod

/**
 * description ： EventBus中的Poster类
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/10 08:27
 */
interface Poster {
    fun enqueue(subscriberMethod: SubscriberMethod, event:Any)
}