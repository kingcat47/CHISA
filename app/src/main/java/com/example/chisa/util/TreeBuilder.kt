package com.example.chisa.util

import com.example.chisa.model.GridItem
import java.io.File

// ──────────────────────────────────────────────────────────────────────────────
// buildTextTree
//   allItems(GridItem 리스트)를 LLM에 넘길 텍스트 트리로 변환한다.
//
//   출력 예시:
//     root
//     ├── 문서
//     │   ├── 보고서.pdf
//     │   └── 계획서.docx
//     └── 사진
//         └── vacation.jpg
//
//   guessFilePos() 가 이 트리를 참고해 새 파일의 적절한 위치를 추천한다.
// ──────────────────────────────────────────────────────────────────────────────
fun buildTextTree(items: List<GridItem>): String {
    val folderPaths = items
        .filterIsInstance<GridItem.Folder>()
        .map { it.item.path }
        .toSet()

    fun parentPathOf(item: GridItem): String = when (item) {
        is GridItem.Folder -> File(item.item.path).parent ?: ""
        is GridItem.File   -> File(item.item.path).parent ?: ""
    }

    fun nameOf(item: GridItem): String = when (item) {
        is GridItem.Folder -> item.item.name
        is GridItem.File   -> item.item.name
    }

    fun childrenOf(parentPath: String): List<GridItem> =
        items.filter { parentPathOf(it) == parentPath }

    fun buildLines(scope: List<GridItem>, prefix: String): List<String> {
        val lines = mutableListOf<String>()
        scope.forEachIndexed { index, item ->
            val isLast    = index == scope.lastIndex
            val connector = if (isLast) "└── " else "├── "
            val childPfx  = prefix + if (isLast) "    " else "│   "
            lines.add("$prefix$connector${nameOf(item)}")
            if (item is GridItem.Folder) {
                lines.addAll(buildLines(childrenOf(item.item.path), childPfx))
            }
        }
        return lines
    }

    val rootItems = items.filter { parentPathOf(it) !in folderPaths }
    return (listOf("root") + buildLines(rootItems, "")).joinToString("\n")
}
