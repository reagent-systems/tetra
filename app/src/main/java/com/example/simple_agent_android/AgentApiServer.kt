package com.example.simple_agent_android

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.net.URLDecoder

class AgentApiServer(port: Int = 8080) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/getCurrentJson" -> {
                val json = BoundingBoxAccessibilityService.getInteractiveElementsJson()
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            }
            "/simulatePressAt" -> {
                val params = session.parameters
                val x = params["x"]?.firstOrNull()?.toIntOrNull()
                val y = params["y"]?.firstOrNull()?.toIntOrNull()
                return if (x != null && y != null) {
                    BoundingBoxAccessibilityService.simulatePressAt(x, y)
                    newFixedLengthResponse(Response.Status.OK, "application/json", JSONObject(mapOf("result" to "ok")).toString())
                } else {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", JSONObject(mapOf("error" to "Missing x or y")).toString())
                }
            }
            "/setTextAt" -> {
                val params = session.parameters
                val x = params["x"]?.firstOrNull()?.toIntOrNull()
                val y = params["y"]?.firstOrNull()?.toIntOrNull()
                val text = params["text"]?.firstOrNull()
                return if (x != null && y != null && text != null) {
                    val decodedText = URLDecoder.decode(text, "UTF-8")
                    BoundingBoxAccessibilityService.setTextAt(x, y, decodedText)
                    newFixedLengthResponse(Response.Status.OK, "application/json", JSONObject(mapOf("result" to "ok")).toString())
                } else {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", JSONObject(mapOf("error" to "Missing x, y, or text")).toString())
                }
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }
} 