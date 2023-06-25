package com.bartolini.pixelbyte.modules.rendering.components.camera;

import com.bartolini.pixelbyte.ecs.Component;
import com.bartolini.pixelbyte.modules.rendering.bitmap.filter.PostProcessingFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A <i>Camera</i> is a {@linkplain Component}, which is used for transforming the view, as well as managing
 * {@linkplain PostProcessingFilter PostProcessingFilters}.
 *
 * @author Bartolini
 * @version 1.0
 */
public class Camera extends Component {

    private final List<PostProcessingFilter> postProcessingFilters = new ArrayList<>();

    private int backgroundColor;

    /**
     * Allocates a new {@code Camera} by passing in the {@code backgroundColor} used when clearing the screen and a
     * varargs of {@linkplain PostProcessingFilter PostProcessingFilters}.
     *
     * @param backgroundColor the background color used when rendering the view from this {@code Camera}.
     * @param filters         a varargs of {@code PostProcessingFilters} to be applied to the view of this
     *                        {@code Camera}.
     * @throws NullPointerException if any of the specified {@code PostProcessingFilters} is {@code null}.
     */
    public Camera(int backgroundColor, PostProcessingFilter... filters) {
        this.backgroundColor = backgroundColor;
        this.postProcessingFilters.addAll(List.of(filters));
    }

    /**
     * Allocates a new {@code Camera}. This constructor has the same effect as
     * {@linkplain #Camera(int, PostProcessingFilter...) Camera} {@code (0xff000000)}.
     */
    public Camera() {
        this(0xff000000);
    }

    /**
     * Adds the specified {@linkplain PostProcessingFilter} to this {@code Camera}.
     *
     * @param filter the {@code PostProcessingFilter} to be added to this {@code Camera}.
     * @throws NullPointerException if the specified {@code PostProcessingFilter} is {@code null}.
     */
    public void addPostProcessingFilter(PostProcessingFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        postProcessingFilters.add(filter);
    }

    /**
     * Removes the specified {@linkplain PostProcessingFilter} from this {@code Camera}.
     *
     * @param filter the {@code PostProcessingFilter} to be removed from this {@code Camera}.
     * @throws NullPointerException if the specified {@code PostProcessingFilter} is {@code null}.
     */
    public void removePostProcessingFilter(PostProcessingFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        postProcessingFilters.remove(filter);
    }

    /**
     * Returns an unmodifiable {@linkplain List} of the {@linkplain PostProcessingFilter PostProcessingFilters} added to
     * this {@code Camera}.
     *
     * @return an unmodifiable {@code List} of the {@code PostProcessingFilters} added to this {@code Camera}.
     */
    public List<PostProcessingFilter> getPostProcessingFilters() {
        return Collections.unmodifiableList(postProcessingFilters);
    }

    /**
     * Returns the background color used for this {@code Camera's} view.
     *
     * @return the background color used for this {@code Camera's} view.
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color used for this {@code Camera's} view.
     *
     * @param backgroundColor the new background color to be used for this {@code Camera's} view.
     */
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}