package com.serwylo.retrowars.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
    open fun getButtons(): List<ButtonDefinition> {
        return listOf()
    }
}

class ButtonDefinition(
    val name: String,
    val icon: (sprites: UiAssets.Sprites) -> TextureRegion,
    val keys: List<Int>,
    val makeButton: () -> ControllerButton,
) {
    constructor(
        name: String,
        icon: (sprites: UiAssets.Sprites) -> TextureRegion,
        key: Int,
        makeButton: () -> ControllerButton,
    ): this(name, icon, listOf(key), makeButton)
}

class SoftController(uiAssets: UiAssets, layout: SoftControllerLayout, index: Int) {

    private val table = Table()
    private val buttonDefs = layout.getButtons()
    private val buttonStates = buttonDefs.associateBy({ it.name }, { it.makeButton() })
    private val buttonActors: Map<String, IconButton>

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

        val expectedButtons = buttonDefs.map { it.name }

        val missingButtons = expectedButtons.filter { buttonName ->
            val found = cellContents.any { row ->
                row.any { cell -> cell == buttonName }
            }

            !found
        }

        if (missingButtons.isNotEmpty()) {
            error("Incorrect controller layout specified. Missing buttons: ${missingButtons.joinToString(", ")}.")
        }

        val buttonSize = UI_SPACE * 15

        table.bottom().pad(UI_SPACE * 4)

        val actors = mutableMapOf<String, IconButton>()

        cellContents.onEach { row ->

            table.row()

            row.onEach { buttonContent ->

                if (buttonContent.isBlank()) {

                    table.add()

                } else if (buttonContent.matches(Regex("<-.*>"))) {

                    table.add().expandX()

                } else {

                    val buttonDef = buttonDefs.find { it.name == buttonContent } ?: error("Could not find button \"$buttonContent\" in controller layout.")
                    val button = IconButton(uiAssets.getSkin(), buttonDef.icon(uiAssets.getSprites()))
                    button.addAction(Actions.alpha(0.4f))
                    table.add(button).space(UI_SPACE * 2).size(buttonSize)
                    actors[buttonContent] = button

                    val listener = object: ClickListener() {

                        override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                            buttonStates[buttonContent]?.softKeyPress()
                            return true
                        }

                        override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                            buttonStates[buttonContent]?.softKeyRelease()
                        }

                    }

                    button.addListener(listener)

                }

            }

        }

        this.buttonActors = actors.toMap()
    }

    private fun getLayout(layouts: List<String>, index: Int): String = if (layouts.size >= index) {
        layouts[index]
    } else {
        Gdx.app.error(TAG, "Tried to use layout ${index}, but there are only ${layouts.size} available. Defaulting to 0.")
        layouts[0]
    }

    fun getActor() = table

    fun update(delta: Float) {
        buttonStates.onEach { (name, state) ->
            val def = buttonDefs.find { it.name == name } ?: error("Cannot update button $name.")

            if (def.keys.any { Gdx.input.isKeyPressed(it) }) {
                state.keyPress()
            } else {
                state.keyRelease()
            }

            state.update(delta)
        }
    }

    // TODO: Remove
    fun isPressed(button: String) = false

    fun trigger(button: String): Boolean {
        val state = buttonStates[button] ?: error("Could not find button \"$button\".")
        return state.trigger()
    }

    // TODO: Make private.
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

        buttonActors[button]!!.addListener(listener)

    }

}

interface ControllerButton {
    fun softKeyPress()
    fun softKeyRelease()

    fun keyPress()
    fun keyRelease()

    fun trigger(): Boolean

    fun update(delta: Float) {}
}

class ThrottledButton(
    private val timeBetweenTriggers: Float,
): ControllerButton {

    private var pressed: State = State.Released
    private var timeUntilNextTriggers: Float = 0f

    override fun softKeyPress() {
        if (pressed == State.Released) {
            pressed = State.SoftKeyPressed
        }
    }

    override fun keyPress() {
        if (pressed == State.Released) {
            pressed = State.KeyPressed
            timeUntilNextTriggers = 0f
        }
    }

    override fun softKeyRelease() {
        if (pressed == State.SoftKeyPressed) {
            pressed = State.Released
            timeUntilNextTriggers = 0f
        }
    }

    override fun keyRelease() {
        if (pressed == State.KeyPressed) {
            pressed = State.Released
            timeUntilNextTriggers = 0f
        }
    }

    override fun update(delta: Float) {
        timeUntilNextTriggers -= delta
    }

    /**
     * If [timeUntilNextTriggers] has reached zero, then reset it back to [timeBetweenTriggers] and
     * then return true.
     */
    override fun trigger() =
        if (pressed != State.Released && timeUntilNextTriggers <= 0f) {
            timeUntilNextTriggers = timeBetweenTriggers
            true
        } else {
            false
        }

    enum class State {
        SoftKeyPressed,
        KeyPressed,
        Released,
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

    /*override fun getButtons(sprites: UiAssets.Sprites) = listOf(
        Buttons.UP to sprites.buttonIcons.up,
        Buttons.DOWN to sprites.buttonIcons.down,
        Buttons.LEFT to sprites.buttonIcons.left,
        Buttons.RIGHT to sprites.buttonIcons.right,
    )*/

}

class AsteroidsSoftController: SoftControllerLayout() {
    fun getIcons(sprites: UiAssets.Sprites) = mapOf(
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

    fun getIcons(sprites: UiAssets.Sprites) = mapOf(
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

    override fun getButtons() = listOf(
        ButtonDefinition(
            Buttons.MOVE_CLOCKWISE,
            { sprites -> sprites.buttonIcons.rotate_clockwise },
            Input.Keys.RIGHT,
            { ThrottledButton(0.1f) },
        ),
        ButtonDefinition(
            Buttons.MOVE_COUNTER_CLOCKWISE,
            { sprites -> sprites.buttonIcons.rotate_counter_clockwise },
            Input.Keys.LEFT,
            { ThrottledButton(0.1f) },
        ),
        ButtonDefinition(
            Buttons.FIRE,
            { sprites -> sprites.buttonIcons.fire },
            Input.Keys.SPACE,
            { ThrottledButton(1 / 20f) },
        ),
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
