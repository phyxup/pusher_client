package com.github.chinloyal.pusher_client.core.utils

import com.pusher.client.util.ConnectionFactory
import com.google.gson.JsonObject

class JsonEncodedConnectionFactory : ConnectionFactory() {
    override fun getCharset(): String {
        return "UTF-8";
    }

    override fun getContentType(): String {
        return "application/json";
    }

    override fun getBody(): String {
        val data: JsonObject = JsonObject();
        data.addProperty("channel_name", channelName);
        data.addProperty("socket_id", socketId);

        return data.toString();
    }
}