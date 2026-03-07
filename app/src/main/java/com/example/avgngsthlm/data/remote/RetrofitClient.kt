package com.example.avgngsthlm.data.remote

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Samsung blocks system DNS for background apps on mobile data.
     * Try the system resolver first; fall back to a hardcoded IP for
     * api.resrobot.se if DNS lookup fails.
     *
     * To refresh the fallback IP: nslookup api.resrobot.se
     */
    private val dnsFallback = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname).ifEmpty { throw UnknownHostException(hostname) }
            } catch (_: UnknownHostException) {
                listOf(
                    InetAddress.getByAddress(
                        hostname,
                        byteArrayOf(94.toByte(), 185.toByte(), 84.toByte(), 185.toByte())
                    )
                )
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .dns(dnsFallback)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** ResRobot – avgångstavla och platssökning */
    val slApiService: SLApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SLApiService::class.java)
    }
}
