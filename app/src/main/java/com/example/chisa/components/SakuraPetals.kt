package com.example.chisa.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────────────────────
// SakuraPetal (data class)
//   꽃잎 하나의 고정 속성. 앱 시작 시 remember 로 한 번 생성되고 이후 변경되지 않는다.
//
//   xFraction : 화면 가로 기준 시작 x 위치 비율 (0.0 ~ 1.0)
//   speed     : 낙하 속도 (애니메이션 duration 에 반비례 — 빠를수록 짧은 duration)
//   size      : 꽃잎 크기(px)
//   angle     : 꽃잎 기울기 초기 각도 (0 ~ 360)
//   phase     : 애니메이션 위상 오프셋 (0.0 ~ 1.0) — 같은 속도여도 서로 다른 위치에서 시작
//   swayAmp   : 좌우 흔들림 진폭 (px)
// ──────────────────────────────────────────────────────────────────────────────
private data class SakuraPetal(
    val xFraction : Float,
    val speed     : Int,
    val size      : Float,
    val angle     : Float,
    val phase     : Float,
    val swayAmp   : Float
)

// 꽃잎 개수
private const val PETAL_COUNT = 18

// ──────────────────────────────────────────────────────────────────────────────
// SakuraPetals
//   벚꽃 꽃잎이 화면 위에서 아래로 흩날리는 애니메이션 컴포넌트.
//   isSakuraTheme 가 true 일 때 MainActivity 에서 콘텐츠 위에 오버레이로 배치한다.
//
//   구현 방식:
//     - PETAL_COUNT 개의 꽃잎을 rememberInfiniteTransition 으로 각각 독립 애니메이션
//     - progress(0.0 → 1.0) 가 낙하 위치를 결정한다 (y = height * progress)
//     - sin() 함수로 좌우 흔들림 효과를 추가한다
//     - Canvas 위에 타원(drawOval)으로 꽃잎 형태를 표현하고 rotate 로 기울인다
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun SakuraPetals(modifier: Modifier = Modifier) {
    // 꽃잎 속성은 컴포지션 내에서 고정 (리컴포즈 시 재생성 방지)
    val petals = remember {
        val random = kotlin.random.Random(seed = 42)
        List(PETAL_COUNT) {
            SakuraPetal(
                xFraction = random.nextFloat(),
                speed     = random.nextInt(6000, 12000),  // 6~12초
                size      = random.nextFloat() * 14f + 8f, // 8~22px
                angle     = random.nextFloat() * 360f,
                phase     = random.nextFloat(),
                swayAmp   = random.nextFloat() * 60f + 20f // 20~80px
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sakura")

    // 꽃잎마다 progress 값을 개별 애니메이션으로 구동한다
    // phase 오프셋을 initialValue 로 줘서 처음부터 화면 전체에 골고루 퍼진 것처럼 보이게 한다
    val progresses = petals.map { petal ->
        val progress by infiniteTransition.animateFloat(
            initialValue   = petal.phase,
            targetValue    = petal.phase + 1f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(durationMillis = petal.speed, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "petal"
        )
        progress
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        petals.forEachIndexed { index, petal ->
            // progress 를 0~1 범위로 정규화 (phase 오프셋 제거)
            val normalizedProgress = progresses[index] % 1f

            val x = petal.xFraction * size.width +
                    sin(normalizedProgress * Math.PI.toFloat() * 4) * petal.swayAmp
            val y = normalizedProgress * (size.height + petal.size * 2) - petal.size

            rotate(
                degrees = petal.angle + normalizedProgress * 360f,
                pivot   = Offset(x, y)
            ) {
                drawOval(
                    color  = Color(0xCCF48FB1), // 벚꽃 핑크, 약간 투명
                    topLeft = Offset(x - petal.size / 2, y - petal.size / 4),
                    size   = androidx.compose.ui.geometry.Size(petal.size, petal.size / 2)
                )
            }
        }
    }
}
