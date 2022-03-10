package xigua.eventbus

/**
 * description ： EventBus中用来将SubscriptionPost和Event类封装在一起，方便统一放入缓存队列
 * author : Watermelon02
 * email : 1446157077@qq.com
 * date : 2022/3/10 09:21
 */
class PendingPost(val subscriberMethod: SubscriberMethod,val event:Any)