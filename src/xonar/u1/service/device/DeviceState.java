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

package xonar.u1.service.device;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class DeviceState {

    public static final String STATE_FILE = ".xonaru1cfg";

    public static final int MAX_VOLUME = 20;
    public static final float STEP = 0.5f;

    public static final char LED_MUTE = 'A';
    public static final char LED_NORMAL = 'D';
    public static final char LED_MAX = 'S';

    public static final char DISCONNECTED = '!';
    public static final char CONNECTED = '#';
    public static final char UNDEFINED = '?';

    private float volume;
    private char ledColor;
    private char status = UNDEFINED;

    private static DeviceState state;

    public static DeviceState getState() {
        if (state == null) {
            state = new DeviceState();
        }
        return state;
    }

    private DeviceState() {
        volume = 0;
        ledColor = LED_MUTE;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float _volume) {
        if (_volume < 0) {
            volume = 0;
        } else if (_volume > MAX_VOLUME) {
            volume = MAX_VOLUME;
        } else {
            volume = _volume;
        }
    }

    public void increaseVolume() {
        float newvol = getVolume() + STEP;
        setVolume(newvol);
    }

    public void decreaseVolume() {
        float newvol = getVolume() - STEP;
        setVolume(newvol);
    }

    public char getLedColor() {
        return ledColor;
    }

    public void setLedColor(char _ledColor) {
        switch (_ledColor) {
            case LED_MUTE:
            case LED_NORMAL:
            case LED_MAX:
                ledColor = _ledColor;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char _status) {
        switch (_status) {
            case CONNECTED:
            case DISCONNECTED:
            case UNDEFINED:
                status = _status;
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void save() {

        String home = System.getProperty("user.home");

        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(home + "/" + STATE_FILE)
        )) {
            dos.writeFloat(volume);
            dos.writeChar(ledColor);
        } catch (IOException e) {
        }

    }

    public void load() {

        String home = System.getProperty("user.home");

        try (DataInputStream dis = new DataInputStream(
                new FileInputStream(home + "/" + STATE_FILE)
        )) {
            volume = dis.readFloat();
            ledColor = dis.readChar();
        } catch (IOException e) {
        }

    }

}
