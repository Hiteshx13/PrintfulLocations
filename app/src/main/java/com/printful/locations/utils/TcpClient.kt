package com.printful.locations.utils

import android.util.Log
import java.io.*
import java.net.InetAddress
import java.net.Socket


private const val SERVER_PORT = 6111
private const val SERVER_IP: String = "ios-test.printful.lv"

class TcpClient(var listener: OnMessageReceived?) {
    private var mServerMessage: String? = null
    private var mRun = false
    private var mBufferOut: PrintWriter? = null
    private var mBufferIn: BufferedReader? = null

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String) {
        val runnable = Runnable {
            if (mBufferOut != null) {
                Log.d(TAG, "Sending: $message")
                mBufferOut!!.println(message)
                mBufferOut!!.flush()
            }
        }
        val thread = Thread(runnable)
        thread.start()
    }

    /**
     * Close the connection and release the members
     */
    fun stopClient() {
        mRun = false
        if (mBufferOut != null) {
            mBufferOut!!.flush()
            mBufferOut!!.close()
        }
        listener = null
        mBufferIn = null
        mBufferOut = null
        mServerMessage = null
    }

    fun run() {
        mRun = true
        try {
            val serverAddr =
                InetAddress.getByName(SERVER_IP)
            Log.d("TCP Client", "C: Connecting...")

            val socket =
                Socket(serverAddr, SERVER_PORT)
            try {
                mBufferOut = PrintWriter(
                    BufferedWriter(OutputStreamWriter(socket.getOutputStream())),
                    true
                )
                mBufferIn =
                    BufferedReader(InputStreamReader(socket.getInputStream()))
                while (mRun) {
                    mServerMessage = mBufferIn!!.readLine()
                    if (mServerMessage != null && listener != null) {
                        //call the method messageReceived from MyActivity class
                        listener!!.messageReceived(mServerMessage)
                    }
                }
                Log.d(
                    "RESPONSE FROM SERVER",
                    "S: Received Message: '$mServerMessage'"
                )
            } catch (e: Exception) {
                Log.e("TCP", "S: Error", e)
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            Log.e("TCP", "C: Error", e)
        }
    }

    interface OnMessageReceived {
        fun messageReceived(message: String?)
    }

    companion object {
        val TAG = TcpClient::class.java.simpleName

    }
}