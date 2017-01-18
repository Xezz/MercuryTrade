package com.mercury.platform.ui.frame;

import com.mercury.platform.shared.pojo.FrameSettings;
import com.mercury.platform.ui.misc.AppThemeColor;
import com.mercury.platform.ui.misc.HideSettingsManager;
import org.pushingpixels.trident.Timeline;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Created by Константин on 26.12.2016.
 */
public abstract class ComponentFrame extends OverlaidFrame implements Packable {
    private final int HIDE_TIME = 200;
    private final int SHOW_TIME = 150;
    private final int BORDER_THICKNESS = 1;
    private int HIDE_DELAY = 1000;
    private float minOpacity = 1f;
    private float maxOpacity = 1f;

    protected int x;
    protected int y;
    protected boolean withinResizeSpace = false;

    private Timeline hideAnimation;
    private Timeline showAnimation;
    private Timer hideTimer;
    private HideEffectListener hideEffectListener;
    private boolean hideAnimationEnable = false;


    protected ComponentFrame(String title) {
        super(title);
    }

    @Override
    protected void initialize(){
        initAnimationTimers();

        this.addMouseListener(new ResizeMouseListener());
        this.addMouseMotionListener(new ResizeMouseMotionListener());
        HideSettingsManager.INSTANCE.registerFrame(this);

        FrameSettings frameSettings = configManager.getFrameSettings(this.getClass().getSimpleName());
        this.setLocation(frameSettings.getFrameLocation());
        this.setMinimumSize(new Dimension(frameSettings.getFrameSize().width,0));
        this.getRootPane().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppThemeColor.TRANSPARENT,2),
                BorderFactory.createLineBorder(AppThemeColor.BORDER, BORDER_THICKNESS)));


    }

    public void disableHideEffect(){
        this.setOpacity(maxOpacity);
        this.hideAnimationEnable = false;
        if(hideEffectListener != null) {
            this.removeMouseListener(hideEffectListener);
        }
    }
    public void enableHideEffect(int delay, int minOpacity, int maxOpacity){
        this.HIDE_DELAY = delay*1000;
        this.minOpacity = minOpacity/100f;
        this.maxOpacity = maxOpacity/100f;
        this.setOpacity(minOpacity/100f);
        hideEffectListener = new HideEffectListener();
        initAnimationTimers();
        this.addMouseListener(hideEffectListener);
        this.hideAnimationEnable = true;
    }

    /**
     * Standard pack() method does not take into account the maximum size of frame.
     */
    public void packFrame(){
        this.pack();
        FrameSettings frameSettings = configManager.getDefaultFramesSettings().get(this.getClass().getSimpleName());
        this.setSize(new Dimension(frameSettings.getFrameSize().width,this.getHeight()));
    }
    private void initAnimationTimers(){
        showAnimation = new Timeline(this);
        showAnimation.setDuration(SHOW_TIME);
        showAnimation.addPropertyToInterpolate("opacity", minOpacity, maxOpacity);

        hideAnimation = new Timeline(this);
        hideAnimation.setDuration(HIDE_TIME);
        hideAnimation.addPropertyToInterpolate("opacity",maxOpacity,minOpacity);
    }

    private class ResizeMouseMotionListener extends MouseMotionAdapter{
        @Override
        public void mouseDragged(MouseEvent e) {
            Point frameLocation = ComponentFrame.this.getLocation();
            ComponentFrame.this.setSize(new Dimension(e.getLocationOnScreen().x - frameLocation.x,ComponentFrame.this.getHeight()));
        }
    }
    private class ResizeMouseListener extends MouseAdapter{
        private Rectangle rightResizeRect = new Rectangle();
        @Override
        public void mousePressed(MouseEvent e) {
            withinResizeSpace = true;
            FrameSettings frameSettings = configManager.getDefaultFramesSettings().get(ComponentFrame.this.getClass().getSimpleName());
            ComponentFrame.this.setMinimumSize(new Dimension(frameSettings.getFrameSize().width,0));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            withinResizeSpace = false;
            if(hideAnimationEnable && !isMouseWithInFrame()) {
                hideTimer.start();
            }
            Dimension size = ComponentFrame.this.getSize();
            ComponentFrame.this.setMinimumSize(new Dimension(size.width,0));
            configManager.saveFrameSize(ComponentFrame.this.getClass().getSimpleName(),ComponentFrame.this.getSize());
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            int frameWidth = ComponentFrame.this.getWidth();
            int frameHeight = ComponentFrame.this.getHeight();
            Point frameLocation = ComponentFrame.this.getLocation();
            rightResizeRect = new Rectangle(
                    frameLocation.x + frameWidth - (BORDER_THICKNESS + 2),
                    frameLocation.y,BORDER_THICKNESS+2,frameHeight);
            if(rightResizeRect.getBounds().contains(e.getLocationOnScreen())) {
                ComponentFrame.this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            ComponentFrame.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Listeners for hide&show animation on frame
     */
    private class HideEffectListener extends MouseAdapter {
        public HideEffectListener(){
            hideTimer = new Timer(HIDE_DELAY,listener ->{
                showAnimation.abort();
                hideAnimation.play();
            });
            hideTimer.setRepeats(false);
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            ComponentFrame.this.repaint();
            hideTimer.stop();
            if(ComponentFrame.this.getOpacity() < maxOpacity) {
                showAnimation.play();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(!isMouseWithInFrame() && !withinResizeSpace){
                hideTimer.start();
            }
        }
    }

    /**
     * Frame Listeners to change the position on the screen.
     */
    public class DraggedFrameMotionListener extends MouseAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            e.translatePoint(ComponentFrame.this.getLocation().x - x,ComponentFrame.this.getLocation().y - y);
            ComponentFrame.this.setLocation(e.getX(),e.getY());
            configManager.saveFrameLocation(ComponentFrame.this.getClass().getSimpleName(),ComponentFrame.this.getLocationOnScreen());
        }
    }
    public class DraggedFrameMouseListener extends MouseAdapter{
        @Override
        public void mousePressed(MouseEvent e) {
            x = e.getX();
            y = e.getY();
        }
    }
}
