package com.example.data.remote

import com.example.data.model.BaseApiResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface AppsScriptService {

    @GET
    suspend fun executeAction(
        @Url url: String,
        @Query("action") action: String,
        @Query("noRek") noRek: String? = null,
        @Query("nis") nis: String? = null,
        @Query("password") password: String? = null,
        @Query("namaLengkap") namaLengkap: String? = null,
        @Query("nama") nama: String? = null,
        @Query("alamat") alamat: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("username") username: String? = null,
        @Query("tipe") tipe: String? = null,
        @Query("nominal") nominal: Long? = null,
        @Query("keterangan") keterangan: String? = null,
        @Query("tanggal") tanggal: String? = null,
        @Query("status") status: String? = null,
        @Query("id") id: String? = null,
        @Query("adminNote") adminNote: String? = null,
        @Query("reason") reason: String? = null
    ): Response<BaseApiResponse>

    companion object {
        fun create(): AppsScriptService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://script.google.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(AppsScriptService::class.java)
        }
    }
}
