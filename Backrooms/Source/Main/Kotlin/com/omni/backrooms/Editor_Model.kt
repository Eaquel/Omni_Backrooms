package com.omni.backrooms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class EditorVM @Inject constructor(
    private val repo: Settings_Repository
) : ViewModel() {

    fun saveLayout(layout: List<UiButtonLayout>) {
        viewModelScope.launch { repo.saveUiLayout(layout) }
    }
}

private data class DragBtn(
    val id      : String,
    val labelRes: Int,
    var offsetX : Float,
    var offsetY : Float
)

@Composable
fun Editor_Model(onSave: () -> Unit, vm: EditorVM = hiltViewModel()) {

    val buttons = remember {
        mutableStateListOf(
            DragBtn("joystick",   R.string.editor_btn_move,      80f,   400f),
            DragBtn("sprint",     R.string.editor_btn_sprint,    300f,  460f),
            DragBtn("interact",   R.string.editor_btn_interact,  900f,  400f),
            DragBtn("crouch",     R.string.editor_btn_crouch,    1000f, 460f),
            DragBtn("flashlight", R.string.editor_btn_flashlight,1100f, 400f)
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF080806).copy(alpha = 0.92f))
    ) {
        Crt_Overlay()

        Text(
            text     = stringResource(R.string.controls_ui_layout).uppercase(),
            color    = TextDim,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        )

        buttons.forEachIndexed { index, btn ->
            var ox by remember { mutableFloatStateOf(btn.offsetX) }
            var oy by remember { mutableFloatStateOf(btn.offsetY) }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }
                    .size(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MetalBg.copy(alpha = 0.88f))
                    .border(1.dp, YellowDim, RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            ox += drag.x
                            oy += drag.y
                            buttons[index] = btn.copy(offsetX = ox, offsetY = oy)
                        }
                    }
            ) {
                Text(
                    text      = stringResource(btn.labelRes),
                    color     = Yellow,
                    fontSize  = 9.sp,
                    fontWeight= FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(4.dp)
                )
            }
        }

        Omni_Button(
            text    = stringResource(R.string.controls_save_exit),
            onClick = {
                vm.saveLayout(buttons.map {
                    UiButtonLayout(buttonId = it.id, offset = Offset(it.offsetX, it.offsetY))
                })
                onSave()
            },
            width   = 200.dp,
            height  = 48.dp,
            modifier= Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
