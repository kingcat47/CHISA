package com.example.chisa.repository

import android.content.Context
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import com.example.chisa.model.FileItem
import com.example.chisa.util.colorForExtension
import com.example.chisa.model.FolderItem
import com.example.chisa.model.GridItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ──────────────────────────────────────────────────────────────────────────────
// StorageRepositoryImpl
//   Android MediaStore API 를 사용해 디바이스 저장소의 파일/폴더를 조회한다.
//
//   [폴더 조회 방식]
//     MediaStore.Images.Media 에서 BUCKET_ID 기준으로 중복을 제거해
//     미디어가 존재하는 디렉토리(버킷)를 FolderItem 으로 변환한다.
//
//   [파일 조회 방식]
//     Images / Video / Audio 세 가지 URI 에서 각각 파일을 조회하고 합산한다.
//
//   @param context ApplicationContext 를 주입받아 사용 (Activity 참조 금지)
// ──────────────────────────────────────────────────────────────────────────────
class StorageRepositoryImpl(private val context: Context) : StorageRepository {

    // 폴더/파일에 랜덤하게 부여할 색상 팔레트
    private val colorPalette = listOf(
        Color(0xFF4FC3F7), // 하늘
        Color(0xFFFFB74D), // 주황
        Color(0xFFBA68C8), // 보라
        Color(0xFF81C784), // 초록
        Color(0xFFE57373), // 빨강
        Color(0xFF4DB6AC), // 청록
        Color(0xFFF06292), // 핑크
    )

    // 날짜 포맷: MediaStore 에서 받아온 timestamp → "yyyy-MM-dd" 문자열로 변환
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ──────────────────────────────────────────────────────────────────────────
    // loadAllItems
    //   폴더 목록 + 파일 목록을 합산해서 반환한다.
    //   Dispatchers.IO 에서 실행해 메인 스레드 블로킹을 방지한다.
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun loadAllItems(): List<GridItem> = withContext(Dispatchers.IO) {
        val folders = loadFolders()
        val files   = loadFiles()
        folders + files
    }

    // ──────────────────────────────────────────────────────────────────────────
    // loadFolders (private)
    //   MediaStore.Images.Media 에서 미디어 버킷(폴더)을 조회한다.
    //   같은 BUCKET_ID 가 여러 행에 걸쳐 나올 수 있으므로 seenIds 로 중복 제거한다.
    // ──────────────────────────────────────────────────────────────────────────
    private fun loadFolders(): List<GridItem.Folder> {
        val folders     = mutableListOf<GridItem.Folder>()
        val seenBuckets = mutableSetOf<Long>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val colBucketId   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val colBucketName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val colDateMod    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val colData       = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(colBucketId)

                // 이미 처리한 버킷은 건너뜀 (중복 제거)
                if (!seenBuckets.add(bucketId)) continue

                val name = cursor.getString(colBucketName) ?: continue
                // DATA 컬럼에서 파일 경로를 읽어 상위 디렉토리 경로 추출
                val path = cursor.getString(colData)
                    ?.let { File(it).parent }
                    ?: continue

                // DATE_MODIFIED 는 초(seconds) 단위 → 밀리초로 변환
                val dateMs   = cursor.getLong(colDateMod) * 1000L
                val dateStr  = dateFormatter.format(Date(dateMs))

                folders.add(
                    GridItem.Folder(
                        FolderItem(
                            id       = "bucket_$bucketId",
                            name     = name,
                            date     = dateStr,
                            path     = path,
                            metadata = "media folder",
                            color    = colorForName(name)
                        )
                    )
                )
            }
        }

        return folders
    }

    // ──────────────────────────────────────────────────────────────────────────
    // loadFiles (private)
    //   이미지 / 영상 / 오디오 URI 에서 파일 목록을 조회해 합산한다.
    // ──────────────────────────────────────────────────────────────────────────
    private fun loadFiles(): List<GridItem.File> {
        val mediaUris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "image",
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI  to "video",
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI  to "audio"
        )

        return mediaUris.flatMap { (uri, type) -> queryMediaFiles(uri, type) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // queryMediaFiles (private)
    //   단일 MediaStore URI 에서 파일 목록을 조회하는 공통 쿼리 함수.
    //   이미지/영상/오디오 모두 같은 컬럼 구조를 공유하므로 재사용한다.
    //
    //   @param uri      조회할 MediaStore URI (Images / Video / Audio)
    //   @param fileType 파일 종류 레이블 (metadata 필드에 저장)
    // ──────────────────────────────────────────────────────────────────────────
    private fun queryMediaFiles(
        uri: android.net.Uri,
        fileType: String
    ): List<GridItem.File> {
        val files = mutableListOf<GridItem.File>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATA
        )

        context.contentResolver.query(
            uri,
            projection,
            null, null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val colId       = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val colName     = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val colDateMod  = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val colData     = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val id      = cursor.getLong(colId)
                val name    = cursor.getString(colName) ?: continue
                val path    = cursor.getString(colData) ?: continue
                val dateMs  = cursor.getLong(colDateMod) * 1000L
                val dateStr = dateFormatter.format(Date(dateMs))

                files.add(
                    GridItem.File(
                        FileItem(
                            id       = "${fileType}_$id",
                            name     = name,
                            date     = dateStr,
                            path     = path,
                            metadata = fileType,
                            color    = colorForExtension(name)
                        )
                    )
                )
            }
        }

        return files
    }

    // ──────────────────────────────────────────────────────────────────────────
    // colorForName
    //   폴더 이름의 해시값을 기반으로 색상 팔레트에서 색상을 선택한다.
    //   같은 이름은 항상 같은 색상을 반환한다.
    // ──────────────────────────────────────────────────────────────────────────
    private fun colorForName(name: String): Color =
        colorPalette[abs(name.hashCode()) % colorPalette.size]

    // ──────────────────────────────────────────────────────────────────────────
    // createFolder
    //   앱 내부 저장소(filesDir)에 실제 디렉토리를 생성하고 GridItem.Folder 로 반환한다.
    //   외부 저장소는 Android 10 이후 별도 권한이 필요하므로 앱 전용 디렉토리를 사용한다.
    //
    //   @param name   폴더 이름
    //   @param color  사용자가 선택한 폴더 색상
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun createFolder(name: String, color: Color): GridItem.Folder =
        withContext(Dispatchers.IO) {
            val dir   = File(context.filesDir, "folders/$name")
            dir.mkdirs()

            val today = dateFormatter.format(Date())

            GridItem.Folder(
                FolderItem(
                    id       = "created_${name.hashCode()}",
                    name     = name,
                    date     = today,
                    path     = dir.absolutePath,
                    metadata = "created",
                    color    = color
                )
            )
        }
}