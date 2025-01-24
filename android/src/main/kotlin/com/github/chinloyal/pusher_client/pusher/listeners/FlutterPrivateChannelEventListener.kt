package com.github.chinloyal.pusher_client.pusher.listeners

import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.enableLogging
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.errorLog
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import java.lang.Exception
import org.json.JSONObject

class FlutterPrivateChannelEventListener: FlutterBaseChannelEventListener(), PrivateChannelEventListener {
    companion object {
        val instance = FlutterPrivateChannelEventListener()
    }

    override fun onAuthenticationFailure(message: String, e: Exception) {
        errorLog(message)
        if(enableLogging) e.printStackTrace()
    }

    override fun onSubscriptionSucceeded(channelName: String) {
        val eventData = JSONObject()
        eventData.put("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", null)
        eventData.put("data", null)

        this.onEvent(PusherEvent(eventData))
        PusherService.debugLog("[PRIVATE] Subscribed: $channelName")
    }
}