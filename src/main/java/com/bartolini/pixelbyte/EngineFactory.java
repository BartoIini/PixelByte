package com.bartolini.pixelbyte;

import com.bartolini.pixelbyte.core.Engine;
import com.bartolini.pixelbyte.core.EngineModule;
import com.bartolini.pixelbyte.core.UIUtils;
import com.bartolini.pixelbyte.ecs.SceneManager;
import com.bartolini.pixelbyte.modules.asset.AssetLoader;
import com.bartolini.pixelbyte.modules.asset.AssetManager;
import com.bartolini.pixelbyte.modules.input.Input;
import com.bartolini.pixelbyte.modules.scripting.ScriptManager;
import com.bartolini.pixelbyte.modules.terminal.Terminal;
import com.bartolini.pixelbyte.modules.terminal.ui.Autocomplete;
import com.bartolini.pixelbyte.modules.terminal.ui.SwingTerminalUI;
import com.bartolini.pixelbyte.modules.terminal.ui.color.DefaultColorScheme;
import com.bartolini.pixelbyte.modules.time.Time;

import java.awt.*;
import java.util.Objects;

/**
 * An <i>EngineFactory</i> is used to create {@linkplain Engine} instances equipped with commonly used
 * {@linkplain EngineModule EngineModules}.
 *
 * @author Bartolini
 * @version 1.0
 */
public class EngineFactory {

    private EngineFactory() {
    }

    /**
     * Creates an {@linkplain Engine} instance by specifying the {@linkplain SceneManager}, the path to the root
     * directory with assets, and a varargs of {@linkplain AssetLoader AssetLoaders}. The {@code Engine} will be
     * equipped with the following {@linkplain EngineModule EngineModules}:
     * <ul>
     *   <li>{@linkplain AssetManager} (with the specified AssetLoaders)</li>
     *   <li>{@linkplain Time}</li>
     *   <li>{@linkplain Terminal}</li>
     *   <li>{@linkplain Input}</li>
     *   <li>{@linkplain ScriptManager}</li>
     *
     * @param sceneManager the {@code SceneManager} used for the {@code Engine}.
     * @return an {@code Engine} instance equipped with common {@code EngineModules}, based on the
     * specified parameters.
     * @throws NullPointerException     if the specified {@code SceneManager}, assets root path, or any of the specified
     *                                  {@code AssetLoaders} are {@code null}.
     * @throws IllegalArgumentException if there are duplicates in the specified {@code AssetLoaders}, or if multiple
     *                                  of the specified {@code AssetLoaders} share supported file extensions.
     */
    public static Engine createEngineRuntime(SceneManager sceneManager, String assetsRoot, AssetLoader<?>... assetLoaders) {
        Objects.requireNonNull(sceneManager, "sceneManager must not be null");

        // Set default system LookAndFeel
        UIUtils.setSystemLookAndFeel();

        Engine engine = new Engine(sceneManager, 100);

        engine.addModule(new AssetManager(assetsRoot, assetLoaders));
        engine.addModule(new Time());
        engine.addModule(new Terminal(new SwingTerminalUI(
                "Terminal",
                new Dimension(800, 500),
                new Autocomplete(engine.getEnvironmentManager(), 20),
                new DefaultColorScheme())));
        engine.addModule(new Input());
        engine.addModule(new ScriptManager());

        return engine;
    }

    /**
     * Creates an {@linkplain Engine} instance by specifying a varargs of {@linkplain AssetLoader AssetLoaders}. The
     * {@code Engine} will be equipped with the following {@linkplain EngineModule EngineModules}:
     * <ul>
     *   <li>{@linkplain AssetManager} (with the specified AssetLoaders)</li>
     *   <li>{@linkplain Time}</li>
     *   <li>{@linkplain Terminal}</li>
     *   <li>{@linkplain Input}</li>
     *   <li>{@linkplain ScriptManager}</li>
     *
     * @return an {@code Engine} instance equipped with common {@code EngineModules}, based on the
     * specified parameters.
     * @throws NullPointerException     if the specified assets root path, or any of the specified {@code AssetLoaders}
     *                                  are {@code null}.
     * @throws IllegalArgumentException if there are duplicates in the specified {@code AssetLoaders}, or if multiple
     *                                  of the specified {@code AssetLoaders} share supported file extensions.
     */
    public static Engine createEngineRuntime(String assetsRoot, AssetLoader<?>... assetLoaders) {
        // Create a scene manager
        SceneManager sceneManager = new SceneManager();

        // Create an engine instance with the created sceneManager
        return createEngineRuntime(sceneManager, assetsRoot, assetLoaders);
    }
}