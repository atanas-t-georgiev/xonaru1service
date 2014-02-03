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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamTokenizer;


public final class AlsaUtil {

    private AlsaUtil() {
    }

    public static int findU1Id() throws IOException {

        Process process = Runtime.getRuntime().exec("/bin/sh -s");
        InputStream is = process.getInputStream();
        OutputStream os = process.getOutputStream();

        os.write("aplay -l | grep \"USB Advanced Audio Device\"\n".getBytes());
        os.write("echo -1\n".getBytes());
        os.write("echo -1\n".getBytes());
        os.flush();

        StreamTokenizer st = new StreamTokenizer(is);
        st.parseNumbers();

        st.nextToken();
        int res = st.nextToken();

        is.close();
        os.close();
        process.destroy();

        return (int) st.nval;

    }

    public static boolean setDefaultDevice(int _devId) {

        String home = System.getProperty("user.home");

        String data = "pcm.!default {\n"
                + " type hw\n"
                + " card " + _devId + "\n"
                + "}\n"
                + "\n"
                + "ctl.!default {\n"
                + " type hw\n"
                + " card " + _devId + "\n"
                + "}";

        try (FileOutputStream fos = new FileOutputStream(home + "/.asoundrc")) {

            fos.write(data.getBytes());

        } catch (IOException e) {
            return false;
        }

        return true;

    }

}
