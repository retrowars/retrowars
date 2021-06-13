package com.serwylo.retrowars.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.serwylo.beatgame.ui.UI_SPACE
import com.serwylo.retrowars.UiAssets
import com.serwylo.retrowars.games.tetris.ButtonState
import com.serwylo.retrowars.ui.IconButton

open class SoftController(uiAssets: UiAssets, layout: String, expectedButtons: Map<String, TextureRegion>) {

    private val table = Table()
    private val buttons: Map<String, IconButton>

    companion object {
        private const val TAG = "SoftController"

        @JvmStatic
        protected fun getLayout(layouts: List<String>, index: Int): String = if (layouts.size >= index) {
            layouts[index]
        } else {
            Gdx.app.error(TAG, "Tried to use layout ${index}, but there are only ${layouts.size} available. Defaulting to 0.")
            layouts[0]
        }
    }

    init {

        val cellContents = layout
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                line.trimStart('[').trimEnd(']').split("][").map { cell ->
                    cell.trim()
                }
            }

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

class SnakeSoftController(layout: Int, uiAssets: UiAssets): SoftController(
    uiAssets,
    getLayout(layouts, layout),
    mapOf(
        Buttons.UP to uiAssets.getSprites().buttonIcons.up,
        Buttons.DOWN to uiAssets.getSprites().buttonIcons.down,
        Buttons.LEFT to uiAssets.getSprites().buttonIcons.left,
        Buttons.RIGHT to uiAssets.getSprites().buttonIcons.right,
    )
) {

    companion object {

        val layouts = listOf(
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

    }

    object Buttons {
        const val UP = "up"
        const val DOWN = "down"
        const val LEFT = "left"
        const val RIGHT = "right"
    }

}

class AsteroidsSoftController(layout: Int, uiAssets: UiAssets): SoftController(
    uiAssets,
    getLayout(layouts, layout),
    mapOf(
        Buttons.THRUST to uiAssets.getSprites().buttonIcons.thrust,
        Buttons.FIRE to uiAssets.getSprites().buttonIcons.button_x,
        Buttons.LEFT to uiAssets.getSprites().buttonIcons.left,
        Buttons.RIGHT to uiAssets.getSprites().buttonIcons.right,
    )
) {

    companion object {

        val layouts = listOf(
            "[ left   ][ right  ][<---->][ fire   ][ thrust ]",
            "[ left   ][ right  ][<---->][ thrust ][ fire   ]",
            "[ fire   ][ thrust ][<---->][ left   ][ right  ]",
            "[ thrust ][ fire   ][<---->][ left   ][ right  ]",
            "[ left   ][ fire   ][<---->][ thrust ][ right  ]",
            "[ left   ][ thrust ][<---->][ fire   ][ right  ]",
        )

    }

    object Buttons {
        const val THRUST = "thrust"
        const val FIRE = "fire"
        const val LEFT = "left"
        const val RIGHT = "right"
    }

}

class TetrisSoftController(layout: Int, uiAssets: UiAssets): SoftController(
    uiAssets,
    getLayout(layouts, layout),
    mapOf(
        Buttons.LEFT to uiAssets.getSprites().buttonIcons.left,
        Buttons.RIGHT to uiAssets.getSprites().buttonIcons.right,
        Buttons.ROTATE_CW to uiAssets.getSprites().buttonIcons.rotate_clockwise,
        Buttons.ROTATE_CCW to uiAssets.getSprites().buttonIcons.rotate_counter_clockwise,
        Buttons.DROP to uiAssets.getSprites().buttonIcons.drop,
    )
) {

    companion object {

        val layouts = listOf(
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

    }

    object Buttons {
        const val LEFT = "left"
        const val RIGHT = "right"
        const val ROTATE_CW = "rotate_cw"
        const val ROTATE_CCW = "rotate_ccw"
        const val DROP = "drop"
    }

}
