/*    
 *  xonaru1service - Service to handle signals from xonaru1d.
 * 
 *  Copyright (C) 2014  Atanas Georgiev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package xonar.u1.service.ui;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.JWindow;
import xonar.u1.service.device.DeviceState;


public class VolumeBar extends JWindow {

    private static final int BAR_TIMEOUT = 1000;

    private static final int W_POS = 2;
    private static final int H_POS = 4;

    private static final int W_RAT = 4;
    private static final int H_RAT = 12;

    private final float barWidth;
    private final float barHeight;

    private final VolumePanel panel;
    private VolumeBarTimerThread timer;

    private final Object timerLock;

    public VolumeBar() {

        timerLock = new Object();

        GraphicsDevice gd = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        float sw = gd.getDisplayMode().getWidth();
        float sh = gd.getDisplayMode().getHeight();

        barWidth = sw / W_RAT;
        barHeight = sh / H_RAT;

        sw = sw - sw / W_POS;
        sh = sh - sh / H_POS;

        setSize((int)barWidth, (int)barHeight);
        setLocation((int)(sw - barWidth / 2f), (int)(sh - barHeight / 2f));

        setAlwaysOnTop(true);
        setFocusable(false);
        setType(Type.POPUP);

        panel = new VolumePanel();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);

        setVisible(false);

    }

    public void enableVolumeBar() {

        timer = new VolumeBarTimerThread();
        timer.start();

    }

    public void disableVolumeBar() {

        timer.interrupt();

    }

    public void setVolume(int _vol) {

        panel.setVolume(_vol);
        panel.repaint();
        setVisible(true);

        timer.setTimer(BAR_TIMEOUT);

        synchronized (timerLock) {
            timerLock.notify();
        }

    }

    private class VolumeBarTimerThread extends Thread {

        private VolumeBarTimerThread() {
            super("VOLUME_BAR_TIMER_THREAD");
        }

        private int timer;

        private synchronized int getTimer() {
            return timer;
        }

        private synchronized void setTimer(int _t) {
            timer = _t;
        }

        private synchronized void decreaseTimer(int _amount) {
            timer -= _amount;
        }

        @Override
        public void run() {

            while (!interrupted()) {

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    break;
                }

                decreaseTimer(10);
                if (getTimer() == 0) {
                    setVisible(false);

                    synchronized (timerLock) {
                        try {
                            timerLock.wait();
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }

                }

            }
            
            setVisible(false);

        }

    }

    private class VolumePanel extends Canvas {

        private int volume;

        private final float colW;
        private final float colH;

        private final float offsW;
        private final float offsH;

        private VolumePanel() {

            colW = barWidth / (DeviceState.MAX_VOLUME * 2 + 1);
            colH = barHeight * 0.8f;

            offsW = colW ;
            offsH = (barHeight - colH) / 2;

        }

        private void setVolume(int _volume) {
            volume = _volume;
            invalidate();
        }

        @Override
        public void update(Graphics g) {

            Graphics offgc;
            Image offscreen;

            offscreen = createImage((int)barWidth, (int)barHeight);
            offgc = offscreen.getGraphics();

            paint(offgc);

            g.drawImage(offscreen, 0, 0, this);

        }

        @Override
        public void paint(Graphics g) {

            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, (int)barWidth, (int)barHeight);

            float posW = offsW;
            for (int i = 1; i <= DeviceState.MAX_VOLUME; i++) {

                if (volume >= i) {
                    g2.setColor(Color.GREEN);
                } else {
                    g2.setColor(Color.DARK_GRAY);
                }

                if (volume == 0) {
                    g2.setColor(Color.RED);
                }

                g2.fillRect((int) posW, (int) offsH, (int) colW, (int) colH);
                posW += (colW * 2);
            }

        }

    }

}
