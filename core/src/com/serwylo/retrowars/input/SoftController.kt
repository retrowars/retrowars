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

class ContinuousPressButton: ControllerButton {

    private var state = State.Released

    override fun softKeyPress() {
        state = State.SoftKeyPressed
    }

    override fun softKeyRelease() {
        if (state == State.SoftKeyPressed) {
            state = State.Released
        }
    }

    override fun keyPress() {
        state = State.KeyPressed
    }

    override fun keyRelease() {
        if (state == State.KeyPressed) {
            state = State.Released
        }
    }

    override fun trigger() = state == State.KeyPressed || state == State.SoftKeyPressed

    enum class State {
        SoftKeyPressed,
        KeyPressed,
        Released,
    }

}

class SingleShotButton: ControllerButton {

    private var pressed = false

    override fun softKeyPress() {
        pressed = true
    }

    override fun softKeyRelease() {
    }

    override fun keyPress() {
        pressed = true
    }

    override fun keyRelease() {
    }

    override fun trigger(): Boolean {
        if (pressed) {
            pressed = false
            return true
        }

        return false
    }

}

class ThrottledButton(timeBetweenTriggers: Float):
    DelayedThrottledButton(timeBetweenTriggers, timeBetweenTriggers)

open class DelayedThrottledButton(
    private val timeBeforeSecondTrigger: Float,
    private val timeBetweenTriggers: Float,
): ControllerButton {

    private var numTriggers: Int = 0
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
            numTriggers = 0
        }
    }

    override fun keyRelease() {
        if (pressed == State.KeyPressed) {
            pressed = State.Released
            timeUntilNextTriggers = 0f
            numTriggers = 0
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
            timeUntilNextTriggers = if (numTriggers == 0) timeBeforeSecondTrigger else timeBetweenTriggers
            numTriggers ++
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

    override fun getButtons() = listOf(
        ButtonDefinition(
            Buttons.UP,
            { sprites -> sprites.buttonIcons.up },
            Input.Keys.UP,
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.DOWN,
            { sprites -> sprites.buttonIcons.down },
            Input.Keys.DOWN,
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.LEFT,
            { sprites -> sprites.buttonIcons.left },
            Input.Keys.LEFT,
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.RIGHT,
            { sprites -> sprites.buttonIcons.right },
            Input.Keys.RIGHT,
            { SingleShotButton() },
        ),
    )

}

class AsteroidsSoftController: SoftControllerLayout() {
    override fun getButtons() = listOf(
        ButtonDefinition(
            Buttons.THRUST,
            { sprites -> sprites.buttonIcons.thrust },
            Input.Keys.UP,
            { ContinuousPressButton() },
        ),
        ButtonDefinition(
            Buttons.FIRE,
            { sprites -> sprites.buttonIcons.fire },
            Input.Keys.SPACE,
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.LEFT,
            { sprites -> sprites.buttonIcons.left },
            Input.Keys.LEFT,
            { ContinuousPressButton() },
        ),
        ButtonDefinition(
            Buttons.RIGHT,
            { sprites -> sprites.buttonIcons.right },
            Input.Keys.RIGHT,
            { ContinuousPressButton() },
        ),
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

class SpaceInvadersSoftController: SoftControllerLayout() {

    override fun getButtons() = listOf(
        ButtonDefinition(
            Buttons.LEFT,
            { sprites -> sprites.buttonIcons.left },
            Input.Keys.LEFT,
            { ContinuousPressButton() },
        ),
        ButtonDefinition(
            Buttons.RIGHT,
            { sprites -> sprites.buttonIcons.right },
            Input.Keys.RIGHT,
            { ContinuousPressButton() },
        ),
        ButtonDefinition(
            Buttons.FIRE,
            { sprites -> sprites.buttonIcons.fire },
            Input.Keys.SPACE,
            { SingleShotButton() },
        ),
    )

    override fun getLayouts() = listOf(
        """
        [ left ][ right ][<-->][ fire ]
        """,

        """
        [ fire ][<-->][ left ][ right ]
        """,
    )

    object Buttons {
        const val LEFT = "left"
        const val RIGHT = "right"
        const val FIRE = "fire"
    }

}

class TetrisSoftController: SoftControllerLayout() {

    /**
     * DAS is 23 frames out of approx 60.
     * Auto repeat rate is every 9 frames.
     * https://tetris.wiki/Tetris_(Game_Boy)#Timings
     */
    private val delayedAutoShift = 23f / 60f
    private val autoRepeatRate = 9f / 60f

    override fun getButtons() = listOf(
        ButtonDefinition(
            Buttons.LEFT,
            { sprites -> sprites.buttonIcons.left },
            Input.Keys.LEFT,
            { DelayedThrottledButton(delayedAutoShift, autoRepeatRate) },
        ),
        ButtonDefinition(
            Buttons.RIGHT,
            { sprites -> sprites.buttonIcons.right },
            Input.Keys.RIGHT,
            { DelayedThrottledButton(delayedAutoShift, autoRepeatRate) },
        ),
        ButtonDefinition(
            Buttons.ROTATE_CW,
            { sprites -> sprites.buttonIcons.rotate_clockwise },
            listOf(Input.Keys.A, Input.Keys.UP),
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.ROTATE_CCW,
            { sprites -> sprites.buttonIcons.rotate_counter_clockwise },
            Input.Keys.S,
            { SingleShotButton() },
        ),
        ButtonDefinition(
            Buttons.DOWN,
            { sprites -> sprites.buttonIcons.down },
            Input.Keys.DOWN,
            { ThrottledButton(1 / 20f) },
        ),
        ButtonDefinition(
            Buttons.DROP,
            { sprites -> sprites.buttonIcons.drop },
            Input.Keys.SPACE,
            { SingleShotButton() },
        ),
    )

    override fun getLayouts() = listOf(
        """
        [      ][       ][    ][   down     ][   drop    ]
        [ left ][ right ][<-->][ rotate_ccw ][ rotate_cw ]
        """,

        """
        [      ][       ][    ][ rotate_ccw ][ rotate_cw ]
        [ left ][ right ][<-->][   down     ][   drop    ]
        """,

        """
        [ left ][ right ][<-->][            ][           ]
        [ drop ][ down  ][    ][ rotate_ccw ][ rotate_cw ]
        """,

        """
        [ left ][ right ][<-->][ rotate_ccw ][ rotate_cw ]
        [ drop ][ down  ][    ][            ][           ]
        """,
    )

    object Buttons {
        const val LEFT = "left"
        const val RIGHT = "right"
        const val ROTATE_CW = "rotate_cw"
        const val ROTATE_CCW = "rotate_ccw"
        const val DROP = "drop"
        const val DOWN = "down"
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
