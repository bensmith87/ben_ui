package ben.ui.window;

import ben.ui.input.mouse.MouseButton;
import ben.ui.math.PmvMatrix;
import ben.ui.math.Rect;
import ben.ui.math.Vec2i;
import ben.ui.resource.GlResourceManager;
import ben.ui.resource.color.UiColors;
import ben.ui.resource.shader.FlatProgram;
import ben.ui.resource.shader.TextProgram;
import ben.ui.resource.shader.TextureProgram;
import ben.ui.resource.texture.UiTextures;
import ben.ui.widget.IWidget;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Main Window.
 * <p>
 *     Has an OpenGL context.
 * </p>
 */
@ThreadSafe
public final class MainWindow {

    /**
     * The Logger.
     */
    @Nonnull
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    /**
     * The number of frames per second for the animator.
     */
    private static final int FRAMES_PER_SECOND = 60;

    /**
     * The animator.
     */
    @Nonnull
    private final FPSAnimator animator;

    /**
     * The PMV Matrix.
     */
    @Nonnull
    private final PmvMatrix pmvMatrix = new PmvMatrix();

    /**
     * The OpenGL Resource Manager.
     */
    @Nonnull
    private final GlResourceManager glResourceManager = new GlResourceManager();

    /**
     * The GL window.
     */
    @Nonnull
    private final GLWindow glWindow;

    /**
     * The mouse listener.
     */
    @Nonnull
    private final MouseListener mouseListener;

    /**
     * The key listener.
     */
    @Nonnull
    private final KeyListener keyListener;

    /**
     * The root widget.
     */
    @Nullable
    private IWidget rootWidget;

    /**
     * Constructor.
     * @param width the width of the window in pixels
     * @param height the height of the window in pixels
     */
    public MainWindow(int width, int height) {
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(glp);

        mouseListener = new WindowMouseListener();
        keyListener = new WindowKeyListener();

        glWindow = GLWindow.create(caps);
        glWindow.setSize(width, height);
        glWindow.setVisible(true);

        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                LOGGER.info("Window destroy event");
                stop();
            }
        });

        glWindow.addGLEventListener(new GlEventHandler());
        glWindow.addMouseListener(mouseListener);
        glWindow.addKeyListener(keyListener);

        animator = new FPSAnimator(glWindow, FRAMES_PER_SECOND);
        animator.start();
    }

    /**
     * Set the root widget.
     * @param rootWidget the root widget
     */
    public void setRootWidget(@Nullable IWidget rootWidget) {
        this.rootWidget = rootWidget;
        if (rootWidget != null) {
            Vec2i size = new Vec2i(glWindow.getWidth(), glWindow.getHeight());
            rootWidget.setSize(size);
        }
    }

    /**
     * Get the root widget.
     * @return the root widget.
     */
    @Nullable
    public IWidget getRootWidget() {
        return rootWidget;
    }

    /**
     * Request focus.
     */
    public void requestFocus() {
        glWindow.requestFocus();
    }

    /**
     * Exit the application.
     */
    public void stop() {
        animator.stop();
        if (glWindow.isVisible()) {
            glWindow.destroy();
        }
    }

    /**
     * Invoke a runnable on the EDT thread.
     * @param runnable the runnable to invoke
     */
    public void invokeOnEdt(@Nonnull Runnable runnable) {
        glWindow.runOnEDTIfAvail(false, runnable);
    }

    /**
     * Get the position of the window.
     * @return the position of the window
     */
    @Nonnull
    public Vec2i getPosition() {
        return new Vec2i(glWindow.getX(), glWindow.getY());
    }

    /**
     * Get the mouse listener.
     * @return the mouse listener
     */
    @Nonnull
    public MouseListener getMouseListener() {
        return mouseListener;
    }

    /**
     * Get the key listener.
     * @return the key listener
     */
    @Nonnull
    public KeyListener getKeyListener() {
        return keyListener;
    }

    /**
     * The OpenGL Event Handler.
     */
    private class GlEventHandler implements GLEventListener {

        @Override
        public void init(@Nonnull GLAutoDrawable drawable) {
            LOGGER.info("Initialising the Window");
            GL2 gl = drawable.getGL().getGL2();
            drawable.setGL(GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2.class, gl, null));

            glResourceManager.getTextureManager().loadTexture(UiTextures.FONT, "/textures/font.png");

            glResourceManager.getShaderManager().addProgram(new FlatProgram(gl));
            glResourceManager.getShaderManager().addProgram(new TextureProgram(gl));
            glResourceManager.getShaderManager().addProgram(new TextProgram(gl));

            glResourceManager.getColorManager().loadColors(UiColors.class, "/colors/colors.xml");
        }

        @Override
        public void dispose(@Nonnull GLAutoDrawable drawable) {
            LOGGER.info("Disposing the Window");
        }

        @Override
        public void display(@Nonnull GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glDisable(GL.GL_SCISSOR_TEST);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            gl.glEnable(GL.GL_SCISSOR_TEST);

            gl.glEnable(GL2.GL_BLEND);
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

            if (rootWidget != null) {
                rootWidget.draw(gl, pmvMatrix, glResourceManager);
            }
        }

        @Override
        public void reshape(@Nonnull GLAutoDrawable drawable, int x, int y, int width, int height) {
            LOGGER.info("Reshaping the Window - " + width + "x" + height);
            GL2 gl = drawable.getGL().getGL2();
            gl.glViewport(0, 0, width, height);
            pmvMatrix.identity();
            pmvMatrix.orthographic(new Rect(0, 0, width, height));
            pmvMatrix.setScreenSize(new Vec2i(width, height));
            pmvMatrix.setScissorBox(new Rect(0, 0, width, height));
            if (rootWidget != null) {
                rootWidget.setSize(new Vec2i(width, height));
            }
        }
    }

    /**
     * The Window Mouse Listener.
     * <p>
     *     Forwards events to the Root Widget.
     * </p>
     */
    private class WindowMouseListener implements MouseListener {

        @Override
        public void mouseClicked(@Nonnull MouseEvent e) {
            MouseButton button = newtToNotNewt(e.getButton());
            if (rootWidget != null && button != null) {
                rootWidget.getMouseHandler().mouseClicked(button, new Vec2i(e.getX(), e.getY()));
            }
        }

        @Override
        public void mouseEntered(@Nonnull MouseEvent e) {
            if (rootWidget != null) {
                rootWidget.getMouseHandler().mouseEntered();
            }
        }

        @Override
        public void mouseExited(@Nonnull MouseEvent e) {
            if (rootWidget != null) {
                rootWidget.getMouseHandler().mouseExited();
            }
        }

        @Override
        public void mousePressed(@Nonnull MouseEvent e) {
            MouseButton button = newtToNotNewt(e.getButton());
            if (rootWidget != null && button != null) {
                rootWidget.getMouseHandler().mousePressed(button, new Vec2i(e.getX(), e.getY()));
            }
        }

        @Override
        public void mouseReleased(@Nonnull MouseEvent e) {
            MouseButton button = newtToNotNewt(e.getButton());
            if (rootWidget != null && button != null) {
                rootWidget.getMouseHandler().mouseReleased(button, new Vec2i(e.getX(), e.getY()));
            }
        }

        @Override
        public void mouseMoved(@Nonnull MouseEvent e) {
            if (rootWidget != null) {
                rootWidget.getMouseHandler().mouseMoved(new Vec2i(e.getX(), e.getY()));
            }
        }

        @Override
        public void mouseDragged(@Nonnull MouseEvent e) {
            if (rootWidget != null) {
                rootWidget.getMouseHandler().mouseDragged(new Vec2i(e.getX(), e.getY()));
            }
        }

        @Override
        public void mouseWheelMoved(@Nonnull MouseEvent e) {
            if (rootWidget != null) {
                rootWidget.getMouseHandler().mouseWheelMoved(e.getRotation()[1], new Vec2i(e.getX(), e.getY()));
            }
        }

        /**
         * Converts a NEWT button ID to an enum.
         * @param newtButton the NEWT button ID
         * @return the enum
         */
        @Nullable
        private MouseButton newtToNotNewt(int newtButton) {
            MouseButton button = null;
            switch (newtButton) {
                case MouseEvent.BUTTON1:
                    button = MouseButton.LEFT;
                    break;
                case MouseEvent.BUTTON2:
                    button = MouseButton.MIDDLE;
                    break;
                case MouseEvent.BUTTON3:
                    button = MouseButton.RIGHT;
                    break;
                default:
            }
            return button;
        }
    }

    /**
     * Window Key Listener.
     * <p>
     *     Forwards events to the Root Widget.
     * </p>
     */
    private class WindowKeyListener implements KeyListener {

        @Override
        public void keyPressed(@Nonnull KeyEvent e) {
            if (rootWidget != null) {
                rootWidget.getKeyHandler().keyPressed(e);
            }
        }

        @Override
        public void keyReleased(@Nonnull KeyEvent e) {
            if (rootWidget != null) {
                rootWidget.getKeyHandler().keyReleased(e);
            }
        }
    }
}
