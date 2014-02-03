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

package xonar.u1.service.util;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public final class IPCUtil {

    private static final String PORT_FILE = "/var/tmp/xonaru1d.port";

    private IPCUtil() {
    }

    public static int getIPCPort() {

        int port;

        try (DataInputStream dis
                = new DataInputStream(new FileInputStream(PORT_FILE))) {
            
            int avail = dis.available();

            if (avail < 4) {
                return -1;
            }

            port = dis.readInt();

        } catch (IOException e) {
            return -1;
        }

        port = Integer.reverseBytes(port);

        return port;

    }

}
