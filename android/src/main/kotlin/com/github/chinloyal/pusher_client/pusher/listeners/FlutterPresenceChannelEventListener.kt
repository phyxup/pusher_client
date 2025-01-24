package com.github.chinloyal.pusher_client.pusher.listeners

import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService
import com.pusher.client.channel.PresenceChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.User
import java.lang.Exception

class FlutterPresenceChannelEventListener: FlutterBaseChannelEventListener(), PresenceChannelEventListener {
    companion object {
        val instance = FlutterPresenceChannelEventListener()
    }

    override fun onUsersInformationReceived(channelName: String, users: MutableSet<User>) {
        val eventData = JSONObject()
        eventData.put("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", null)
        eventData.put("data", users.toString())

        this.onEvent(PusherEvent(eventData.toString()))
    }

    override fun userUnsubscribed(channelName: String, user: User) {
        val eventData = JSONObject()
        eventData.put("event", Constants.MEMBER_REMOVED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", user.id)
        eventData.put("data", null)

        this.onEvent(PusherEvent(eventData.toString()))
    }

    override fun userSubscribed(channelName: String, user: User) {
        val eventData = JSONObject()
        eventData.put("event", Constants.MEMBER_ADDED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", user.id)
        eventData.put("data", null)

        this.onEvent(PusherEvent(eventData.toString()))
    }

    override fun onAuthenticationFailure(message: String, e: Exception) {
        PusherService.errorLog(message)
        if(PusherService.enableLogging) e.printStackTrace()
    }

    override fun onSubscriptionSucceeded(channelName: String) {
        val eventData = JSONObject()
        eventData.put("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", null)
        eventData.put("data", null)

        this.onEvent(PusherEvent(eventData.toString()))
        PusherService.debugLog("[PRESENCE] Subscribed: $channelName")
    }
}