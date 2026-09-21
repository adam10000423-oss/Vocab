package com.example.data.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

object AnkiPackageParser {
    suspend fun parse(context: Context, uri: Uri): ParsedExternalDeck = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "anki-import").apply { mkdirs() }
        val databaseFile = File(workDir, "collection-${System.nanoTime()}.anki2")
        try {
            var foundDatabase = false
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.substringAfterLast('/')
                        if (name == "collection.anki2" || name == "collection.anki21") {
                            databaseFile.outputStream().use { output -> zip.copyTo(output) }
                            foundDatabase = true
                            break
                        }
                        entry = zip.nextEntry
                    }
                }
            } ?: error("無法讀取 Anki 檔案")
            require(foundDatabase) {
                "此 .apkg 使用新版壓縮格式或不是有效的 Anki 牌組；請從 Anki 匯出相容格式或改用 CSV"
            }

            val cards = mutableListOf<ExternalCardCandidate>()
            SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery("SELECT flds FROM notes ORDER BY id LIMIT 5000", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val fields = cursor.getString(0)
                            .split('\u001f')
                            .map(ExternalDeckImporter::cleanCell)
                            .filter(String::isNotBlank)
                        if (fields.size >= 2) {
                            cards += ExternalCardCandidate(
                                word = fields[0],
                                definition = fields[1],
                                exampleSentence = fields.getOrNull(2).orEmpty(),
                                sourceName = "Anki"
                            )
                        }
                    }
                }
            }
            require(cards.isNotEmpty()) { "Anki 牌組裡找不到至少兩欄的文字卡片" }
            ParsedExternalDeck("Anki 匯入牌組", "Anki", cards)
        } finally {
            databaseFile.delete()
        }
    }
}
