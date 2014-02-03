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

package xonar.u1.service;

import java.io.IOException;
import xonar.u1.service.device.DeviceState;
import xonar.u1.service.device.ProtocolParser;
import xonar.u1.service.io.AmixerProxy;
import xonar.u1.service.io.DaemonProxy;
import xonar.u1.service.io.IPCListener;
import xonar.u1.service.ui.VolumeBar;
import xonar.u1.service.util.AlsaUtil;
import xonar.u1.service.util.IPCUtil;


public class U1ControlService implements IPCListener {

    private static final String AMIXER_PATH = "/usr/bin/amixer";
    private static final int INIT_ERROR_WAIT = 4000;
    private static final int CONNECT_WAIT = 2000;

    private DaemonProxy ipc;
    private AmixerProxy amixer;
    private VolumeBar bar;

    private final Object loopLock = new Object();

    @Override
    public void onInput(char _in) {

        DeviceState st = DeviceState.getState();
        float oldVol = st.getVolume();

        switch (ProtocolParser.parseCommand(_in)) {

            case ProtocolParser.INC:
                st.increaseVolume();
                break;

            case ProtocolParser.DEC:
                st.decreaseVolume();
                break;

            case ProtocolParser.MUT:
                st.setVolume(0);
                break;

            case ProtocolParser.DSC:
                AlsaUtil.setDefaultDevice(0) ;
                bar.disableVolumeBar();
                st.setStatus(DeviceState.DISCONNECTED);
                stopAmixer();
                System.out.println("U1 Disconnected");
                return;

            case ProtocolParser.CNC:
                wait(CONNECT_WAIT);
                if (initAmixer() && ipc.sendChar(st.getLedColor())) {
                    st.setStatus(DeviceState.CONNECTED);
                    amixer.setVolume((int) st.getVolume());
                    bar.enableVolumeBar();
                    AlsaUtil.setDefaultDevice(amixer.getDeviceId()) ;
                    System.out.println("U1 Connected");
                } else {
                    restartIOLoop();
                }
                return;

            default:
                return ;
                
        }

        bar.setVolume((int) st.getVolume());
        
        if (oldVol != st.getVolume()) {
            amixer.setVolume((int) st.getVolume());
            //System.out.println("Volume: " + (int) st.getVolume());
        }

        // Handle LED change
        
        char oldLed = st.getLedColor() ;
        char newLed = oldLed ;
        
        if (st.getVolume() == 0 
                && oldLed != DeviceState.LED_MUTE) {
            newLed = DeviceState.LED_MUTE ;
        } else if (st.getVolume() == DeviceState.MAX_VOLUME 
                && oldLed != DeviceState.LED_MAX) {
            newLed = DeviceState.LED_MAX ;
        } else if (st.getVolume() > 0 
                && st.getVolume() < DeviceState.MAX_VOLUME 
                && oldLed != DeviceState.LED_NORMAL) {
            newLed = DeviceState.LED_NORMAL ;
        }
        
        if (oldLed != newLed) {
            //System.out.println ("Changing LED Color...") ;
            st.setLedColor(newLed);
            if (!ipc.sendChar(st.getLedColor())) {
                System.err.println ("Error while changing LED Color") ;
                restartIOLoop() ;
            }
        }
        
    }

    @Override
    public void onConnectionError(Exception _ex
    ) {

        restartIOLoop();

    }

    private boolean initAmixer() {

        int devId;

        try {
            devId = AlsaUtil.findU1Id();
        } catch (IOException ex) {
            System.err.println("Exception while checking ALSA device ID.");
            return false;
        }

        if (devId < 0) {
            System.err.println("U1 device is not detected by ALSA.");
            return false;
        }

        amixer = new AmixerProxy(AMIXER_PATH, devId);
        amixer.start();

        return true;

    }

    private void stopAmixer() {

        if (amixer != null) {
            amixer.stop();
        }

        amixer = null;

    }

    private boolean initIPC() {

        int ipcPort = IPCUtil.getIPCPort();

        if (ipcPort <= 0) {
            System.err.println("Cannot get IPC port.");
            return false;
        }

        ipc = new DaemonProxy("127.0.0.1", ipcPort);
        try {
            ipc.open();
        } catch (IOException ex) {
            System.err.println("Cannot open IPC channel.");
            return false;
        }

        ipc.listenForInput(this);

        return true;

    }

    private void stopIPC() {

        try {
            if (ipc != null) {
                ipc.close();
            }
        } catch (IOException ex) {
        }

        ipc = null;

    }

    public void beginIOLoop() {

        DeviceState.getState().load();
        addShutdownHook();

        DeviceState st = DeviceState.getState();

        bar = new VolumeBar();

        while (true) {

            if (initAmixer() && initIPC() && ipc.sendChar(st.getLedColor())) {

                st.setStatus(DeviceState.CONNECTED);
                amixer.setVolume((int) st.getVolume());
                bar.enableVolumeBar();

                AlsaUtil.setDefaultDevice(amixer.getDeviceId()) ;
                
                try {
                    synchronized (loopLock) {
                        loopLock.wait();
                    }
                } catch (InterruptedException ex) {
                }

            } else {

                stopAmixer() ;
                System.err.println("Init Failed (Will try again...)");

                try {
                    Thread.sleep(INIT_ERROR_WAIT);
                } catch (InterruptedException ex) {
                }

            }

        }

    }

    private void restartIOLoop() {

        System.err.println("Will restart IO...");

        bar.disableVolumeBar();
        stopIPC();
        stopAmixer();

        synchronized (loopLock) {
            loopLock.notify();
        }
    }

    private void addShutdownHook() {
        
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                DeviceState.getState().save();
                AlsaUtil.setDefaultDevice(0) ;

            }

        });

    }

    private void wait(int _ms) {

        try {
            Thread.sleep(_ms);
        } catch (InterruptedException ex) {
        }

    }

}
