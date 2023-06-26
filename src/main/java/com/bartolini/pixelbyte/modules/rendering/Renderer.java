package com.bartolini.pixelbyte.modules.rendering;

import com.bartolini.pixelbyte.core.EngineModule;
import com.bartolini.pixelbyte.core.ModuleInitializeException;
import com.bartolini.pixelbyte.ecs.Component;
import com.bartolini.pixelbyte.environment.Variable;
import com.bartolini.pixelbyte.logging.Logger;
import com.bartolini.pixelbyte.logging.LoggerFactory;
import com.bartolini.pixelbyte.modules.input.Input;
import com.bartolini.pixelbyte.modules.input.Key;
import com.bartolini.pixelbyte.modules.rendering.bitmap.Bitmap;
import com.bartolini.pixelbyte.modules.rendering.bitmap.BitmapGraphics;
import com.bartolini.pixelbyte.modules.rendering.bitmap.Colors;
import com.bartolini.pixelbyte.modules.rendering.bitmap.filter.PerPixelFilter;
import com.bartolini.pixelbyte.modules.rendering.bitmap.filter.PostProcessingFilter;
import com.bartolini.pixelbyte.modules.rendering.bitmap.font.BitmapFont;
import com.bartolini.pixelbyte.modules.rendering.components.camera.Camera;
import com.bartolini.pixelbyte.modules.rendering.components.transform.Transform;
import com.bartolini.pixelbyte.modules.rendering.util.FPSTracker;
import com.bartolini.pixelbyte.modules.rendering.util.ImageUtils;
import com.bartolini.pixelbyte.modules.time.Time;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A <i>Renderer</i> is an {@linkplain EngineModule} used to render and display content.
 *
 * @author Bartolini
 * @version 1.0
 */
public abstract class Renderer extends EngineModule {

    private static final Variable<Integer> varWidth;
    private static final Variable<Integer> varHeight;
    private static final Variable<Integer> varScale;

    static {
        varWidth = new Variable<>(
                "width", 16, true, 16, false, Integer.MAX_VALUE,
                "The width of the window");
        varHeight = new Variable<>(
                "height", 16, true, 16, false, Integer.MAX_VALUE,
                "The height of the window");
        varScale = new Variable<>(
                "scale", 1, true, 1, false, Integer.MAX_VALUE,
                "The pixel size in the window");
    }

    private final Logger logger = LoggerFactory.getLogger(this);
    private final String iconPath = "/icons/pixel_icon.png";
    private final String title;
    private final Variable<Boolean> varShowFps;
    private final FPSTracker fpsTracker = new FPSTracker();
    private final PostProcessingFilter noFocusFilter;

    private final AtomicLong totalTransformTime = new AtomicLong();
    private final AtomicLong totalClipTime = new AtomicLong();
    private final AtomicLong totalRenderTime = new AtomicLong();

    private boolean drawFocusGrabber = true;

    private JFrame frame;
    private Canvas canvas;
    private BufferedImage displayImage;
    private BufferStrategy bufferStrategy;

    private Bitmap frameBuffer;
    private BitmapGraphics graphics;
    private Camera camera;

    /**
     * Allocates a new {@code Renderer} by passing in the title, width, height, and pixel scale for the window.
     *
     * @param title  the title for the window.
     * @param width  the width for the window.
     * @param height the height for the window.
     * @param scale  the size of the pixels in the window.
     * @throws NullPointerException if the specified title is {@code null}.
     */
    public Renderer(String title, int width, int height, int scale) {
        super("Renderer", "renderer");

        // Reference the title
        this.title = Objects.requireNonNull(title, "title must not be null");

        // Add variables
        varWidth.setValue(width);
        getEnvironment().addVariable(varWidth);
        varHeight.setValue(height);
        getEnvironment().addVariable(varHeight);
        varScale.setValue(scale);
        getEnvironment().addVariable(varScale);

        getEnvironment().addVariable(this.varShowFps = new Variable<>(
                "show_fps", false,
                "Enables the display of FPS."));

        this.noFocusFilter = new PerPixelFilter(color -> Colors.darken(color, 0.6f));
    }

    /**
     * Returns the width of the framebuffer in pixels.
     *
     * @return the width of the framebuffer in pixels.
     */
    public static int getWidth() {
        return varWidth.getValue();
    }

    /**
     * Returns the height of the framebuffer in pixels.
     *
     * @return the height of the framebuffer in pixels.
     */
    public static int getHeight() {
        return varHeight.getValue();
    }

    /**
     * Returns the scale of the pixels on the screen.
     *
     * @return the scale of the pixels of the screen.
     */
    public static int getScale() {
        return varScale.getValue();
    }

    @Override
    public void initialize() throws ModuleInitializeException {
        // Check if rendering is supported in the current graphics environment
        if (GraphicsEnvironment.isHeadless()) {
            throw new ModuleInitializeException(this, "this graphics environment does not support rendering");
        }

        // Create frame and canvas
        frame = new JFrame(title);
        canvas = new Canvas();
        frame.add(canvas);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // Set the frame icon
        try {
            URL iconURL = getClass().getResource(iconPath);
            Objects.requireNonNull(iconURL, "could not load main window icon");
            frame.setIconImage(new ImageIcon(ImageIO.read(iconURL)).getImage());
        } catch (NullPointerException | IllegalArgumentException | IOException e) {
            logger.error("Could not load the main window icon.", e);
        }

        // Add variable change hooks
        varWidth.addChangeHook(integer -> resizeWindow());
        varHeight.addChangeHook(integer -> resizeWindow());
        varScale.addChangeHook(integer -> resizeWindow());

        // Resize the window
        resizeWindow();
    }

    @Override
    public void start() {
        // Show the window
        frame.setVisible(true);
    }

    @Override
    public void update(double deltaTime) {
        // Save the current frame as an image
        if (Input.isKeyPressed(Key.F11)) {
            ImageUtils.saveImage(displayImage);
        }

        // Search for an active camera
        Optional<Camera> optionalCamera =
                getScene().getComponents(Camera.class).stream().filter(Component::isActive).findFirst();

        // Return if no active camera was found
        if (optionalCamera.isEmpty()) {
            // Draw error message
            String mainMessage = "NO ACTIVE CAMERA IN THE CURRENT SCENE!";
            String secondaryMessage = "Add a camera to render the scene.";
            int stringHeight = graphics.getFontMetrics().stringHeight(mainMessage);
            graphics.clear(0xff000000);
            graphics.drawString(mainMessage,
                    frameBuffer.getWidth() / 2, frameBuffer.getHeight() / 2,
                    BitmapFont.TextAlignment.CENTER, 0xff0000);
            graphics.drawString(secondaryMessage,
                    frameBuffer.getWidth() / 2, 4 + stringHeight + frameBuffer.getHeight() / 2,
                    BitmapFont.TextAlignment.CENTER);

            // Update the canvas and return
            updateCanvas();
            return;
        }

        // Check if it's the same camera
        if (camera != optionalCamera.get()) {
            // ... if not then update
            camera = optionalCamera.get();
        }

        // Return if the found camera has no transform component attached
        Transform cameraTransform = camera.getOwner().getComponent(Transform.class);
        if (cameraTransform == null) {
            // Draw error message
            String mainMessage = "THE ACTIVE CAMERA HAS NO ATTACHED TRANSFORM COMPONENT!";
            String secondaryMessage = "Make sure to add the transform component to the camera.";
            int stringHeight = graphics.getFontMetrics().stringHeight(mainMessage);
            graphics.clear(0xff000000);
            graphics.drawString(mainMessage,
                    frameBuffer.getWidth() / 2, frameBuffer.getHeight() / 2,
                    BitmapFont.TextAlignment.CENTER, 0xff0000);
            graphics.drawString(secondaryMessage,
                    frameBuffer.getWidth() / 2, 4 + stringHeight + frameBuffer.getHeight() / 2,
                    BitmapFont.TextAlignment.CENTER);

            // Update the canvas and return
            updateCanvas();
            return;
        }

        // Clear the screen
        graphics.clear(camera.getBackgroundColor());

        // Render
        render(graphics, camera);

        // Apply post-processing filters
        camera.getPostProcessingFilters().forEach(filter -> filter.filter(frameBuffer));

        // Update the fps counter
        fpsTracker.update(deltaTime);

        // Draw statistics
        if (varShowFps.getValue()) {
            graphics.drawString(fpsTracker.getFPS() + " FPS",
                    frameBuffer.getWidth() - 10, 10, BitmapFont.TextAlignment.RIGHT, 0xffffff, 0xa0a0a0);
        }

        // Inform if not focused
        if (drawFocusGrabber && !canvas.hasFocus()) {
            // Darken the screen
            noFocusFilter.filter(frameBuffer);

            // Render message
            int color = Colors.getGray((int) (180 + (255 - 180) * (Math.sin(Time.getTime() * 10))));
            graphics.drawString("Click to focus!",
                    graphics.getWidth() / 2, graphics.getHeight() / 2, BitmapFont.TextAlignment.CENTER, color);
        }

        // Update the canvas
        if (Input.isKeyDown(Key.Q)) {
            return;
        }
        updateCanvas();
    }

    /**
     * Returns whether the drawing of the focus grabber is enabled.
     *
     * @return {@code true} if the drawing of the focus grabber is enabled; {@code false} otherwise.
     */
    public boolean isFocusGrabberEnabled() {
        return drawFocusGrabber;
    }

    /**
     * Enables or disables the drawing of the focus grabber.
     *
     * @param drawFocusGrabber if {@code true} the focus grabber will be drawn when the window is not focused.
     */
    public void enableFocusGrabber(boolean drawFocusGrabber) {
        this.drawFocusGrabber = drawFocusGrabber;
    }

    /**
     * Used to render the image.
     *
     * @param graphics the {@linkplain BitmapGraphics} instance used to draw to this screen.
     * @param camera   the currently active {@linkplain Camera}.
     */
    protected abstract void render(BitmapGraphics graphics, Camera camera);

    /**
     * Renders the framebuffer {@linkplain Bitmap} to the displayed {@linkplain Canvas}.
     * <p>
     * The rendering is performed by drawing the framebuffer {@code Bitmap} to the {@linkplain BufferStrategy} of the
     * {@code Canvas} using its {@linkplain Graphics} instance.
     * <p>
     * If needed, multiple renders will be performed, as described in {@linkplain BufferStrategy}.
     */
    private void updateCanvas() {
        // Render single frame
        do {
            // The following loop ensures that the contents of the drawing buffer
            // are consistent in case the underlying surface was recreated
            do {
                // Get a new graphics context every time through the loop
                // to make sure the strategy is validated
                Graphics imageGraphics = bufferStrategy.getDrawGraphics();

//                imageGraphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                // Render checker pattern (highlights transparent areas)
                for (int i = 0; i < frame.getWidth() - 8; i += 8) {
                    for (int j = 0; j < frame.getHeight() - 8; j += 8) {
                        if ((j + i) % 16 == 0) {
                            imageGraphics.setColor(Color.LIGHT_GRAY);
                        } else {
                            imageGraphics.setColor(Color.GRAY);
                        }
                        imageGraphics.fillRect(i, j, 8, 8);
                    }
                }
                // Render to graphics
                imageGraphics.drawImage(
                        displayImage, 0, 0,
                        varWidth.getValue() * varScale.getValue(), varHeight.getValue() * varScale.getValue(),
                        null);

                // Dispose the graphics
                imageGraphics.dispose();

                // Repeat the rendering if the drawing buffer contents
                // were restored
            } while (bufferStrategy.contentsRestored());

            // Display the buffer
            bufferStrategy.show();

            // Repeat the rendering if the drawing buffer was lost
        } while (bufferStrategy.contentsLost());
    }

    /**
     * Resizes the window. Should be called when changes to the width, height or scale are made.
     */
    private void resizeWindow() {
        // Resize canvas
        Dimension size = new Dimension(
                varWidth.getValue() * varScale.getValue(),
                varHeight.getValue() * varScale.getValue());
        canvas.setSize(size);
        canvas.setMinimumSize(size);
        canvas.setPreferredSize(size);
        canvas.setMaximumSize(size);

        // Resize frame
        frame.pack();
        frame.setLocationRelativeTo(null);

        // Update displayImage
        displayImage = new BufferedImage(varWidth.getValue(), varHeight.getValue(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) displayImage.getRaster().getDataBuffer()).getData();

        // Update BufferStrategy
        canvas.createBufferStrategy(2);
        bufferStrategy = canvas.getBufferStrategy();

        // Create the framebuffer Bitmap
        frameBuffer = new Bitmap(varWidth.getValue(), varHeight.getValue(), pixels);

        // Reference BitmapGraphics of the framebuffer
        graphics = frameBuffer.getGraphics();
    }
}