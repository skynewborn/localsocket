package com.skynewborn.android.localsocket.server

import android.content.Context
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.skynewborn.android.localsocket.handler.HandlerFactory
import com.skynewborn.android.localsocket.handler.IRequestHandler
import com.skynewborn.android.localsocket.utils.Logger
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean

internal class Server(
    context: Context,
    private val socketName: String,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val handlerFactory = HandlerFactory.getInstance(context)
    private val coScope = CoroutineScope(defaultDispatcher + CoroutineName(TAG))
    private val running = AtomicBoolean()
    
    fun start() = coScope.launch { 
        if (running.get()) {
            // Already in started state
            return@launch
        }
        running.set(true)
        withContext(ioDispatcher) {
            Logger.i("Starting server...")
            try {
                LocalServerSocket(socketName).use { server ->
                    while (running.get()) {
                        if (!isActive) {
                            running.set(false)
                            break
                        }
                        server.accept().use { receiver ->
                            receiver.soTimeout = SO_TIMEOUT
                            receiver.inputStream.use { inputStream ->
                                val reqStr = readStream(inputStream)
                                Logger.i("Got request: $reqStr")
                                if (reqStr.isNotEmpty()) {
                                    receiver.outputStream.use { outputStream ->
                                        val request = Request(reqStr)
                                        val response = handleRequest(request)
                                        writeStream(outputStream, response.toString())
                                        Logger.i("Response sent.")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        onClose()
    }

    private suspend fun handleRequest(request: Request): Response = withContext(defaultDispatcher) {
        val serviceId = request.serviceId
        if (serviceId.isEmpty()) {
            return@withContext Response(
                serviceId,
                null,
                error =  IllegalArgumentException("Invalid request: $request")
            )
        }
        val handler = handlerFactory.getHandler(serviceId)
        if (handler != null) {
            return@withContext handleSync(request, handler)
        }
        return@withContext Response(
            serviceId,
            null,
            error =  IllegalArgumentException("Unsupported service ID: $serviceId")
        )
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun handleSync(request: Request, handler: IRequestHandler): Response = suspendCancellableCoroutine { 
        try {
            handler.handleRequest(request.data, object : IRequestHandler.Callback {
                override fun onHandled(response: String) {
                    if (it.isActive) {
                        val res = Response(request.serviceId, response)
                        it.resume(res, ::onCancellation)
                    }
                }

                override fun onError(error: Throwable) {
                    if (it.isActive) {
                        val res = Response(request.serviceId, null, error)
                        it.resume(res, ::onCancellation)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            if (it.isActive) {
                val res = Response(request.serviceId, null, e)
                it.resume(res, ::onCancellation)
            }
        }
        it.invokeOnCancellation(::onCancellation)
    }
    
    private fun onCancellation(t: Throwable?) {
        t?.printStackTrace()
    }

    private fun onClose() {
        coScope.cancel()
        Logger.i("Server stopped.")
    }
    
    fun close() = coScope.launch { 
        Logger.i("Stopping server...")
        running.set(false)
        signalClose()
    }
    
    private suspend fun signalClose() = withContext(ioDispatcher) {
        Logger.i("Sending close signal...")
        try {
            sendRequest(socketName, "", false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "LocalSocketServer"
        private const val SO_TIMEOUT = 1000 // Socket timeout in mills

        @kotlin.jvm.Throws(IOException::class)
        private fun readStream(inputStream: InputStream): String {
            var inputLine: String?
            val sb = StringBuilder()
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            while (bufferedReader.readLine().also { inputLine = it } != null) {
                sb.append(inputLine)
            }
            return sb.toString()
        }

        @kotlin.jvm.Throws(IOException::class)
        private fun writeStream(outputStream: OutputStream, message: String) {
            val outputStreamWriter = OutputStreamWriter(outputStream)
            val bufferedWriter = BufferedWriter(outputStreamWriter)
            val printWriter = PrintWriter(bufferedWriter)
            printWriter.print(message)
            printWriter.flush()
        }
        
        @kotlin.jvm.Throws(IOException::class)
        fun sendRequest(socketName: String, serviceId: String, data: String,
                        blocking: Boolean = true): String {
            val req = Request(serviceId, data)
            val res = Response(sendRequest(socketName, req.toString(), blocking))
            if (res.code == 0) {
                return res.data!!
            }
            return res.error ?: "Unknown error!"
        }
        
        @kotlin.jvm.Throws(IOException::class)
        fun sendRequest(socketName: String, request: String, blocking: Boolean = true): String {
            LocalSocket().use { sender ->
                sender.connect(LocalSocketAddress(socketName))
                sender.soTimeout = SO_TIMEOUT
                sender.outputStream.use { outputStream ->
                    writeStream(outputStream, request)
                    sender.shutdownOutput()
                    sender.inputStream.use { inputStream ->
                        val response = if (blocking)
                            readStream(inputStream)
                        else
                            ""
                        sender.shutdownInput()
                        return response
                    }
                }
            }
        }
    }
}