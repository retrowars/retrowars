package com.serwylo.retrowars.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.ui.IconButton

abstract class SoftControllerLayout {
    abstract fun getLayouts(): List<String>
    abstract fun getIcons(sprites: UiAssets.Sprites): Map<String, TextureRegion>
}

class SoftController(uiAssets: UiAssets, layout: SoftControllerLayout, index: Int) {

    private val table = Table()
    private val buttons: Map<String, IconButton>

    companion object {
        private const val TAG = "SoftController"
    }

    init {

        val cellContents = getLayout(layout.getLayouts(), index)
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                line.trimStart('[').trimEnd(']').split("][").map { cell ->
                    cell.trim()
                }
            }

        val expectedButtons = layout.getIcons(uiAssets.getSprites())

        val missingButtons = expectedButtons.keys.filter { button ->
            val found = cellContents.any { row ->
                row.any { cell -> cell == button }
            }

            !found
        }

        if (missingButtons.isNotEmpty()) {
            error("Incorrect controller layout specified. Missing buttons: ${missingButtons.joinToString(", ")}.")
        }

        val buttonSize = UI_SPACE * 15

        table.bottom().pad(UI_SPACE * 4)

        val buttons = mutableMapOf<String, IconButton>()

        cellContents.onEach { row ->

            table.row()

            row.onEach { buttonContent ->

                if (buttonContent.isBlank()) {

                    table.add()

                } else if (buttonContent.matches(Regex("<-.*>"))) {

                    table.add().expandX()

                } else {

                    val button = IconButton(uiAssets.getSkin(), expectedButtons[buttonContent]!!)
                    button.addAction(Actions.alpha(0.4f))
                    table.add(button).space(UI_SPACE * 2).size(buttonSize)
                    buttons[buttonContent] = button

                }

            }

        }

        this.buttons = buttons.toMap()
    }

    private fun getLayout(layouts: List<String>, index: Int): String = if (layouts.size >= index) {
        layouts[index]
    } else {
        Gdx.app.error(TAG, "Tried to use layout ${index}, but there are only ${layouts.size} available. Defaulting to 0.")
        layouts[0]
    }

    fun getActor() = table

    fun isPressed(button: String) = buttons[button]!!.isPressed

    fun listen(button: String, touchDown: () -> Unit, touchUp: () -> Unit) {

        val listener = object: ClickListener() {

            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                touchDown()
                return true
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                touchUp()
            }

        }

        buttons[button]!!.addListener(listener)

    }

}

class SnakeSoftController: SoftControllerLayout() {

    object Buttons {
        const val UP = "up"
        const val DOWN = "down"
        const val LEFT = "left"
        const val RIGHT = "right"
    }

    override fun getLayouts() = listOf(
        """
            [      ][  up  ][       ][<---------->]
            [ left ][ down ][ right ][<---------->]
            """,

        """
            [<---------->][      ][  up  ][       ]
            [<---------->][ left ][ down ][ right ]
            """,

        """
            [<--->][      ][  up  ][       ][<--->]
            [<--->][ left ][ down ][ right ][<--->]
            """,

        """
            [      ][       ][      ][    ][      ]
            [ left ][ right ][<---->][ up ][ down ]
            """,

        """
            [      ][       ][      ][      ][    ]
            [ left ][ right ][<---->][ down ][ up ]
            """,

        """
            [      ][    ][      ][      ][       ]
            [ left ][ up ][<---->][ down ][ right ]
            """,

        """
            [      ][      ][      ][    ][       ]
            [ left ][ down ][<---->][ up ][ right ]
            """,
    )

    override fun getIcons(sprites: UiAssets.Sprites) = mapOf(
        Buttons.UP to sprites.buttonIcons.up,
        Buttons.DOWN to sprites.buttonIcons.down,
        Buttons.LEFT to sprites.buttonIcons.left,
        Buttons.RIGHT to sprites.buttonIcons.right,
    )

}

class AsteroidsSoftController: SoftControllerLayout() {
    override fun getIcons(sprites: UiAssets.Sprites) = mapOf(
        Buttons.THRUST to sprites.buttonIcons.thrust,
        Buttons.FIRE to sprites.buttonIcons.fire,
        Buttons.LEFT to sprites.buttonIcons.left,
        Buttons.RIGHT to sprites.buttonIcons.right,
    )

    override fun getLayouts() = listOf(
        "[ left   ][ right  ][<---->][ fire   ][ thrust ]",
        "[ left   ][ right  ][<---->][ thrust ][ fire   ]",
        "[ fire   ][ thrust ][<---->][ left   ][ right  ]",
        "[ thrust ][ fire   ][<---->][ left   ][ right  ]",
        "[ left   ][ fire   ][<---->][ thrust ][ right  ]",
        "[ left   ][ thrust ][<---->][ fire   ][ right  ]",
    )

    object Buttons {
        const val THRUST = "thrust"
        const val FIRE = "fire"
        const val LEFT = "left"
        const val RIGHT = "right"
    }
}

class TetrisSoftController: SoftControllerLayout() {

    override fun getIcons(sprites: UiAssets.Sprites) = mapOf(
        Buttons.LEFT to sprites.buttonIcons.left,
        Buttons.RIGHT to sprites.buttonIcons.right,
        Buttons.ROTATE_CW to sprites.buttonIcons.rotate_clockwise,
        Buttons.ROTATE_CCW to sprites.buttonIcons.rotate_counter_clockwise,
        Buttons.DROP to sprites.buttonIcons.drop,
    )

    override fun getLayouts() = listOf(
        """
        [      ][       ][    ][            ][   drop    ]
        [ left ][ right ][<-->][ rotate_ccw ][ rotate_cw ]
        """,

        """
        [      ][       ][    ][ rotate_ccw ][ rotate_cw ]
        [ left ][ right ][<-->][            ][   drop    ]
        """,

        """
        [ left ][ right ][<-->][            ][           ]
        [ drop ][       ][    ][ rotate_ccw ][ rotate_cw ]
        """,

        """
        [ left ][ right ][<-->][ rotate_ccw ][ rotate_cw ]
        [ drop ][       ][    ][            ][           ]
        """,
    )

    object Buttons {
        const val LEFT = "left"
        const val RIGHT = "right"
        const val ROTATE_CW = "rotate_cw"
        const val ROTATE_CCW = "rotate_ccw"
        const val DROP = "drop"
    }

}

class TempestSoftController: SoftControllerLayout() {

    override fun getIcons(sprites: UiAssets.Sprites) = mapOf(
        Buttons.MOVE_CLOCKWISE to sprites.buttonIcons.rotate_clockwise,
        Buttons.MOVE_COUNTER_CLOCKWISE to sprites.buttonIcons.rotate_counter_clockwise,
        Buttons.FIRE to sprites.buttonIcons.button_x,
    )

    override fun getLayouts() = listOf(
        "[ move_counter_clockwise ][ move_clockwise ][<---->][ fire ]",
        "[ fire ][<---->][ move_counter_clockwise ][ move_clockwise ]",
        "[ move_counter_clockwise ][<---->][ move_clockwise ][ fire ]",
    )

    object Buttons {
        const val MOVE_CLOCKWISE = "move_clockwise"
        const val MOVE_COUNTER_CLOCKWISE = "move_counter_clockwise"
        const val FIRE = "fire"
    }

}
