package app.coinverse.content.room

import android.content.Context

object CoinverseDatabase {
    lateinit var database: CoinverseDatabaseBuilder
    fun init(context: Context) {
        database = CoinverseDatabaseBuilder.getAppDatabase(context)
    }
}