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

package xonar.u1.service.io;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class AmixerProxy {

    private final String execPath;
    private VolumeSetThread volumeThread;

    private final int devId;

    private final LinkedBlockingQueue<Integer> queue;

    public AmixerProxy(String _execPath, int _devId) {
        execPath = _execPath;
        devId = _devId;
        queue = new LinkedBlockingQueue<>();
    }

    public void start() {

        volumeThread = new VolumeSetThread();
        volumeThread.start();

    }

    public void stop() {

        volumeThread.interrupt();

    }

    public void setVolume(int _volume) {

        queue.add(_volume);

    }

    public int getDeviceId () {
        return devId ;
    }
    
    private class VolumeSetThread extends Thread {

        private VolumeSetThread() {
            super ("VOLUME_SET_QUEUE_THREAD") ;
        }
        
        @Override
        public void run() {

            Integer vol;

            while (!interrupted()) {

                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    break;
                }

                vol = queue.poll();
                if (vol == null) {
                    continue;
                }

                try {
                    Runtime.getRuntime()
                            .exec(execPath + " set -c " + devId + " PCM " + vol);
                } catch (IOException e) {
                    System.err.println ("Executing amixer failed.") ;
                }

            }
        }

    }

}
