package com.example.chisa.backend.service

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

class FileReaderService {

    fun readFile(filePath: File, maxPages: Int = 5, maxChars: Int = 4000): String {
        if (!filePath.exists()) throw java.io.FileNotFoundException("File not found: $filePath")

        return when (filePath.extension.lowercase()) {
            "pdf" -> readPdf(filePath, maxPages, maxChars)
            "docx" -> readDocx(filePath, maxChars)
            "txt" -> readTxt(filePath, maxChars)
            else -> throw UnsupportedOperationException("Unsupported file type: ${filePath.extension}")
        }
    }

    private fun readTxt(file: File, maxChars: Int): String {
        return file.readText(charset = Charsets.UTF_8).take(maxChars).trim()
    }

    private fun readDocx(file: File, maxChars: Int): String {
        val zip = ZipFile(file)
        val entry = zip.getEntry("word/document.xml")
            ?: throw IllegalArgumentException("Invalid DOCX: no word/document.xml found")

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(zip.getInputStream(entry), "UTF-8")

        val parts = mutableListOf<String>()
        var currentLen = 0
        var inText = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "w:t" || parser.name == "t") inText = true
                }
                XmlPullParser.TEXT -> {
                    if (inText) {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            val remaining = maxChars - currentLen
                            if (remaining <= 0) break
                            val chunk = text.take(remaining)
                            parts.add(chunk)
                            currentLen += chunk.length
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "w:t" || parser.name == "t") inText = false
                }
            }
            parser.next()
        }

        zip.close()
        return parts.joinToString("\n\n").trim()
    }

    private fun readPdf(file: File, maxPages: Int, maxChars: Int): String {
        PDDocument.load(file).use { document ->
            if (document.isEncrypted) return ""
            val stripper = PDFTextStripper()
            stripper.startPage = 1
            stripper.endPage = minOf(maxPages, document.numberOfPages)
            return stripper.getText(document).take(maxChars).trim()
        }
    }
}
