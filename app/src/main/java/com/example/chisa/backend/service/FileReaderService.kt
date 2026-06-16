package com.example.chisa.backend.service

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

class FileReaderService {

    fun readFile(filePath: File, maxPages: Int = 5, maxChars: Int = 4000): String {
        Log.d("FileReaderService", "readFile 시작 | 경로=${filePath.absolutePath} | 크기=${filePath.length()}bytes | 확장자=${filePath.extension}")
        if (!filePath.exists()) {
            Log.e("FileReaderService", "파일 없음: ${filePath.absolutePath}")
            throw java.io.FileNotFoundException("File not found: $filePath")
        }

        return when (filePath.extension.lowercase()) {
            "pdf"  -> readPdf(filePath, maxPages, maxChars)
            "docx" -> readDocx(filePath, maxChars)
            "txt"  -> readTxt(filePath, maxChars)
            else   -> {
                Log.e("FileReaderService", "지원하지 않는 파일 형식: ${filePath.extension}")
                throw UnsupportedOperationException("Unsupported file type: ${filePath.extension}")
            }
        }
    }

    private fun readTxt(file: File, maxChars: Int): String {
        Log.d("FileReaderService", "TXT 읽기 시작: ${file.name}")
        val result = file.readText(charset = Charsets.UTF_8).take(maxChars).trim()
        Log.d("FileReaderService", "TXT 읽기 완료 | 추출 길이=${result.length}자")
        return result
    }

    private fun readDocx(file: File, maxChars: Int): String {
        Log.d("FileReaderService", "DOCX 읽기 시작: ${file.name}")
        val zip = ZipFile(file)
        val entry = zip.getEntry("word/document.xml")
            ?: run {
                Log.e("FileReaderService", "DOCX 파싱 실패: word/document.xml 없음")
                throw IllegalArgumentException("Invalid DOCX: no word/document.xml found")
            }

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
        val result = parts.joinToString("\n\n").trim()
        Log.d("FileReaderService", "DOCX 읽기 완료 | 추출 길이=${result.length}자")
        return result
    }

    private fun readPdf(file: File, maxPages: Int, maxChars: Int): String {
        Log.d("FileReaderService", "PDF 읽기 시작: ${file.name} | 크기=${file.length()}bytes")
        PDDocument.load(file).use { document ->
            Log.d("FileReaderService", "PDF 로드 완료 | 총 페이지=${document.numberOfPages} | 암호화=${document.isEncrypted}")
            if (document.isEncrypted) {
                Log.w("FileReaderService", "PDF 암호화되어 있어 텍스트 추출 불가")
                return ""
            }
            val stripper = PDFTextStripper()
            stripper.startPage = 1
            stripper.endPage = minOf(maxPages, document.numberOfPages)
            Log.d("FileReaderService", "PDF 텍스트 추출 시도 | 대상 페이지: 1~${stripper.endPage}")
            val result = stripper.getText(document).take(maxChars).trim()
            Log.d("FileReaderService", "PDF 텍스트 추출 완료 | 추출 길이=${result.length}자 | 미리보기='${result.take(100)}'")
            return result
        }
    }
}
