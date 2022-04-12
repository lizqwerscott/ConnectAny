package com.flydog.connectany.http

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
//import io.ktor.server.engine.*
import io.ktor.server.netty.*

class HttpServer constructor(portI: Int){
    val server = embeddedServer(Netty, port = portI) {
        routing {
            get("/") {
                call.respondText("Hello World");
            }

            get("/connect") {

            }
        }
    };

    fun start() {
        server.start(wait = true);
    }

    fun stop() {
        server.stop(1000, 1000)
    }
}