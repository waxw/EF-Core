package com.miyako.demo

import com.miyako.core.debugLog
import com.miyako.data.DataRepository
import com.miyako.data.DataState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class NetRepository : DataRepository() {
  val networkJson = Json { ignoreUnknownKeys = true }

  val retrofit by lazy {
    val logger = HttpLoggingInterceptor.Logger { message ->
      "retrofitBack: $message".debugLog("Retrofit")
    }
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .addInterceptor(HttpLoggingInterceptor(logger).apply {
        level = HttpLoggingInterceptor.Level.BODY
      })
      .build()

    Retrofit.Builder()
      .baseUrl("https://www.wanandroid.com")
      .client(okHttpClient)
      .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType())) // should add it at last.
      .build()
  }

  suspend fun requestArticles(): DataState<NetResult<ArticlePageDto>> {
    "requestArticles".debugLog()
    return requestData {
      val service = retrofit.create(INetService::class.java)
      service.getArticle(0, 10).apply {
        "get size: ${this.data.size}".debugLog()
      }
    }
  }
}


interface INetService {
  @GET("/article/list/{page}/json")
  suspend fun getArticle(
    @Path("page") page: Int,
    @Query("page_size") pageSize: Int
  ): NetResult<ArticlePageDto>
}

@Serializable
data class NetResult<T>(
  @SerialName("errorCode")
  val errorCode: Int,
  @SerialName("errorMsg")
  val errorMsg: String,
  @SerialName("data")
  val data: T,
)

@Serializable
data class ArticlePageDto(
  @SerialName("curPage")
  val curPage: Int,
  @SerialName("datas")
  val datas: List<Article>,
  @SerialName("offset")
  val offset: Int,
  @SerialName("over")
  val over: Boolean,
  @SerialName("pageCount")
  val pageCount: Int,
  @SerialName("size")
  val size: Int,
  @SerialName("total")
  val total: Int
)

@Serializable
data class Article(
  @SerialName("title")
  val title: String,
)

