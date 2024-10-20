package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.ScissorStack;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtendedScrollContainer<C extends Component> extends ScrollContainer<C> {

    protected TriConsumer<ExtendedScrollContainer<?>, Double, Double> scrolledTo = (container, prevOffset, currentOffset) -> {};

    protected boolean oppositeScrollbar = false;
    protected boolean strictMouseScrolling = false;

    private Insets customClippingInsets = Insets.none();

    public ExtendedScrollContainer(ScrollDirection direction, Sizing horizontalSizing, Sizing verticalSizing, C child) {
        super(direction, horizontalSizing, verticalSizing, child);
    }

    @Nullable
    public Runnable scrollToAfterLayout = null;

    public double getProgress() {
        return this.scrollOffset / this.maxScroll;
    }

    public ExtendedScrollContainer<C> scrollToAfterLayout(double progress) {
        this.scrollToAfterLayout = () -> this.scrollOffset = (double)this.maxScroll * Math.clamp(progress, 0, 1);

        return this;
    }

    @Override
    public void layout(Size space) {
        super.layout(space);

        if (this.scrollToAfterLayout != null) {
            this.scrollToAfterLayout.run();
            this.scrollToAfterLayout = null;
        }
    }

    public ExtendedScrollContainer<C> customClippingInsets(Insets insets) {
        this.customClippingInsets = insets;

        return this;
    }

    public ExtendedScrollContainer<C> oppositeScrollbar(boolean value) {
        this.oppositeScrollbar = value;

        return this;
    }

    public boolean oppositeScrollbar() {
        return this.oppositeScrollbar;
    }

    public ExtendedScrollContainer<C> strictMouseScrolling(boolean value){
        this.strictMouseScrolling = value;

        return this;
    }

    @Override
    protected boolean isInScrollbar(double mouseX, double mouseY) {
        if(this.oppositeScrollbar) {
            return this.isInBoundingBox(mouseX, mouseY) && this.direction.choose(mouseY, mouseX) <= (this.x + scrollbarThiccness);
        } else {
            return super.isInScrollbar(mouseX, mouseY);
        }
    }

    @Override
    protected Size calculateChildSpace(Size thisSpace) {
        final var padding = this.padding.get();

        return Size.of(
                Mth.lerpInt(this.horizontalSizing.get().contentFactor(), this.width - padding.horizontal() - horizontalScrollbarOffset(), thisSpace.width() - padding.horizontal()),
                Mth.lerpInt(this.verticalSizing.get().contentFactor(), this.height - padding.vertical() - verticalScrollbarOffset(), thisSpace.height() - padding.vertical())
        );
    }

    @Override
    protected int childMountX() {
        return (int) super.childMountX() + horizontalScrollbarOffset();
    }

    @Override
    protected int childMountY() {
        return (int) super.childMountY() + verticalScrollbarOffset();
    }

    private int horizontalScrollbarOffset() {
        return (oppositeScrollbar && this.direction == ScrollDirection.VERTICAL) ? scrollbarThiccness : 0;
    }

    private int verticalScrollbarOffset() {
        return (oppositeScrollbar && this.direction == ScrollDirection.HORIZONTAL) ? scrollbarThiccness : 0;
    }

    public double currentScrollOffset() {
        return this.scrollOffset;
    }

    public ExtendedScrollContainer<C> scrollTo(double scrollOffset) {
        this.scrollOffset = Mth.clamp(scrollOffset, 0, this.maxScroll);

        return this;
    }

    public ExtendedScrollContainer<C> scrolledToCallback(TriConsumer<ExtendedScrollContainer<?>, Double, Double> consumer) {
        this.scrolledTo = consumer;

        return this;
    }

    @Override
    protected void scrollBy(double offset, boolean instant, boolean showScrollbar) {
        var prevOffset = this.scrollOffset;

        super.scrollBy(offset, instant, showScrollbar);

        scrolledTo.accept(this, prevOffset, this.scrollOffset);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if(this.strictMouseScrolling && !this.isInScrollbar(this.x + mouseX, this.y + mouseY)) return false;

        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    protected void drawChildren(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta, List<? extends Component> children) {
        if (this.customClippingInsets != Insets.none()) {
            ScissorStack.push(
                    this.x + customClippingInsets.left(),
                    this.y + customClippingInsets.top(),
                    this.width - customClippingInsets.horizontal(),
                    this.height - customClippingInsets.vertical(),
                    context.pose());
        }

        super.drawChildren(context, mouseX, mouseY, partialTicks, delta, children);

        if (this.customClippingInsets != Insets.none()) {
            ScissorStack.pop();
        }
    }
}
