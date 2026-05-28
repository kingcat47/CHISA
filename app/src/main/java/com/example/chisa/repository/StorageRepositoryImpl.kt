package com.example.chisa.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.documentfile.provider.DocumentFile
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
    // colorForName
    //   폴더 이름의 해시값을 기반으로 색상 팔레트에서 색상을 선택한다.
    //   같은 이름은 항상 같은 색상을 반환한다.
    // ──────────────────────────────────────────────────────────────────────────
    private fun colorForName(name: String): Color =
        colorPalette[abs(name.hashCode()) % colorPalette.size]

    // ──────────────────────────────────────────────────────────────────────────
    // importFile
    //   파일 피커(OpenDocument)로 선택한 단일 파일을 앱 내부 저장소(filesDir/imports)로
    //   복사하고 실제 파일 경로를 가진 GridItem.File 로 반환한다.
    //
    //   URI 를 path 로 그대로 저장하면 File(path).parent 기반 폴더 필터링이 깨지므로
    //   반드시 복사 후 실제 경로를 저장해야 한다.
    //
    //   @param uri  OpenDocument 가 반환한 파일 URI
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun importFile(uri: Uri): GridItem.File = withContext(Dispatchers.IO) {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0) cursor.getString(col) else null
        } ?: "알 수 없는 파일"

        val destDir = File(context.filesDir, "imports")
        destDir.mkdirs()
        val destFile = File(destDir, name)

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }

        val today = dateFormatter.format(Date())
        GridItem.File(
            FileItem(
                id       = "imported_${destFile.absolutePath.hashCode()}",
                name     = name,
                date     = today,
                path     = destFile.absolutePath,
                metadata = "imported",
                color    = colorForExtension(name)
            )
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // importFolder
    //   OpenDocumentTree 로 얻은 트리 URI 를 순회해 앱 내부 저장소(filesDir/imports)로
    //   폴더 구조 전체를 복사하고 GridItem 목록으로 반환한다.
    //
    //   복사 결과물은 실제 파일 경로(absolutePath)를 가지므로
    //   기존 File(path).parent 기반 폴더 필터링과 완전히 호환된다.
    //
    //   @param uri  OpenDocumentTree 가 반환한 트리 URI
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun importFolder(uri: Uri): List<GridItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext emptyList()
        val destBase = File(context.filesDir, "imports")
        destBase.mkdirs()
        traverseDocumentTree(root, destBase)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // traverseDocumentTree (private)
    //   DocumentFile 트리를 재귀적으로 순회하며 GridItem 목록을 만든다.
    //
    //   - 디렉토리: destDir 아래에 동일한 이름의 디렉토리를 생성하고 FolderItem 추가.
    //              자식 항목을 재귀 호출로 처리한다.
    //   - 파일:    ContentResolver 로 InputStream 을 열어 destDir 에 복사 후 FileItem 추가.
    //              복사 실패 시 해당 파일은 조용히 건너뛴다.
    //
    //   @param doc     현재 처리 중인 DocumentFile (디렉토리 또는 파일)
    //   @param destDir 복사 대상 부모 디렉토리 (실제 File 경로)
    // ──────────────────────────────────────────────────────────────────────────
    private fun traverseDocumentTree(doc: DocumentFile, destDir: File): List<GridItem> {
        val result = mutableListOf<GridItem>()
        val name   = doc.name ?: return result
        val today  = dateFormatter.format(Date())

        if (doc.isDirectory) {
            val thisDir = File(destDir, name)
            thisDir.mkdirs()

            result.add(
                GridItem.Folder(
                    FolderItem(
                        id       = "imported_${thisDir.absolutePath.hashCode()}",
                        name     = name,
                        date     = today,
                        path     = thisDir.absolutePath,
                        metadata = "imported",
                        color    = colorForName(name)
                    )
                )
            )

            doc.listFiles().forEach { child ->
                result.addAll(traverseDocumentTree(child, thisDir))
            }
        } else {
            val destFile = File(destDir, name)

            try {
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                return result  // 복사 실패 시 이 파일은 건너뜀
            }

            result.add(
                GridItem.File(
                    FileItem(
                        id       = "imported_${destFile.absolutePath.hashCode()}",
                        name     = name,
                        date     = today,
                        path     = destFile.absolutePath,
                        metadata = "imported",
                        color    = colorForExtension(name)
                    )
                )
            )
        }

        return result
    }

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

    // ──────────────────────────────────────────────────────────────────────────
    // deleteItem
    //   파일 또는 폴더를 실제 저장소에서 삭제한다.
    //   폴더의 경우 deleteRecursively() 로 하위 항목까지 모두 제거한다.
    //
    //   주의: MediaStore 에서 조회된 외부 저장소 파일은 Android 10 이상에서
    //         MANAGE_EXTERNAL_STORAGE 권한이 없으면 삭제가 실패할 수 있다.
    //         앱 전용 디렉토리(filesDir)에 생성된 파일/폴더는 항상 삭제 가능하다.
    //
    //   @param item 삭제할 GridItem
    //   @return 삭제 성공 여부 (파일이 원래 없어도 true 반환)
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun deleteItem(item: GridItem): Boolean = withContext(Dispatchers.IO) {
        val path = when (item) {
            is GridItem.Folder -> item.item.path
            is GridItem.File   -> item.item.path
        }
        val target = File(path)

        // 파일이 존재하지 않으면 이미 삭제된 것으로 간주해 true 반환
        if (!target.exists()) return@withContext true

        // 폴더는 하위 내용 포함 재귀 삭제, 파일은 단순 삭제
        target.deleteRecursively()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // renameItem
    //   파일 또는 폴더의 이름을 변경한다.
    //
    //   파일의 경우:
    //     - 새 이름에 확장자가 없으면 기존 확장자를 자동으로 붙인다.
    //       예) "새파일"  →  "새파일.jpg"  (기존이 jpg 였다면)
    //     - 새 이름에 확장자가 있으면 그대로 사용한다.
    //
    //   폴더의 경우:
    //     - 같은 부모 경로 아래에서 디렉토리명만 변경한다.
    //
    //   실패(renameTo 가 false) 시 원본 GridItem 을 그대로 반환한다.
    //
    //   @param item    이름 변경 대상
    //   @param newName 새 이름 문자열
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun renameItem(item: GridItem, newName: String): GridItem =
        withContext(Dispatchers.IO) {
            when (item) {
                is GridItem.Folder -> {
                    val oldDir    = File(item.item.path)
                    val parentDir = oldDir.parentFile ?: return@withContext item
                    val newDir    = File(parentDir, newName)

                    if (oldDir.renameTo(newDir)) {
                        // 이름·경로가 바뀐 새 FolderItem 으로 교체
                        GridItem.Folder(item.item.copy(name = newName, path = newDir.absolutePath))
                    } else {
                        item  // rename 실패 시 원본 유지
                    }
                }
                is GridItem.File -> {
                    val oldFile    = File(item.item.path)
                    val parentDir  = oldFile.parentFile ?: return@withContext item

                    // 새 이름에 점(.)이 없으면 기존 확장자를 보존한다
                    val ext         = oldFile.extension
                    val newFileName = if (newName.contains(".") || ext.isEmpty()) newName
                                      else "$newName.$ext"
                    val newFile     = File(parentDir, newFileName)

                    if (oldFile.renameTo(newFile)) {
                        GridItem.File(item.item.copy(name = newFileName, path = newFile.absolutePath))
                    } else {
                        item
                    }
                }
            }
        }

    // ──────────────────────────────────────────────────────────────────────────
    // moveItem
    //   파일 또는 폴더를 대상 폴더 경로 아래로 이동한다.
    //
    //   File.renameTo() 를 사용하므로 같은 볼륨(파티션) 내에서만 동작한다.
    //   이동 후 GridItem 의 path 를 새 경로로 업데이트해서 반환한다.
    //
    //   실패 시 원본 GridItem 을 그대로 반환한다.
    //
    //   @param item         이동할 GridItem
    //   @param targetFolder 이동 대상 FolderItem
    // ──────────────────────────────────────────────────────────────────────────
    override suspend fun moveItem(item: GridItem, targetFolder: FolderItem): GridItem =
        withContext(Dispatchers.IO) {
            val targetDir = File(targetFolder.path)

            // 대상 폴더가 존재하지 않으면 미리 생성한다
            if (!targetDir.exists()) targetDir.mkdirs()

            when (item) {
                is GridItem.Folder -> {
                    val srcDir = File(item.item.path)
                    val dstDir = File(targetDir, srcDir.name)

                    if (srcDir.renameTo(dstDir)) {
                        GridItem.Folder(item.item.copy(path = dstDir.absolutePath))
                    } else {
                        item
                    }
                }
                is GridItem.File -> {
                    val srcFile = File(item.item.path)
                    val dstFile = File(targetDir, srcFile.name)

                    if (srcFile.renameTo(dstFile)) {
                        GridItem.File(item.item.copy(path = dstFile.absolutePath))
                    } else {
                        item
                    }
                }
            }
        }
}