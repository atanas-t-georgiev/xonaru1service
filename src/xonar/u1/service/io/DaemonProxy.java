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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class DaemonProxy {

    private final String host;
    private final int port;

    private Socket socket;
    private InputStream is;
    private OutputStream os;

    private IPCListener listener;
    private IPCInputThread inputThread;

    public DaemonProxy(String _host, int _port) {
        host = _host;
        port = _port;
    }

    public void open() throws IOException {

        socket = new Socket(host, port);
        is = socket.getInputStream();
        os = socket.getOutputStream();

    }

    public void close() throws IOException {

        socket.close();

    }

    public void listenForInput(IPCListener _listener) {

        listener = _listener;
        inputThread = new IPCInputThread();
        inputThread.start();

    }

    public boolean sendChar(char _ch) {

        try {

            os.write(_ch);
            os.flush();

        } catch (IOException e) {
            return false;
        }

        return true;

    }

    private class IPCInputThread extends Thread {

        private IPCInputThread() {
            super("IPC_INPUT_READ_THREAD");
        }

        @Override
        public void run() {

            int in;

            while (true) {

                try {
                    in = is.read();
                    if (in < 0) {
                        throw new IOException("Stream Closed.");
                    }
                    //System.out.print((char) in);
                    listener.onInput((char) in);
                } catch (IOException ex) {
                    listener.onConnectionError(ex);
                    break;
                }

            }

        }

    }

}
