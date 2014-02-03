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

public class ProtocolParser {

    public static final int INC = 1;
    public static final int DEC = -1;
    public static final int MUT = 0;
    public static final int NOOP = -100;
    public static final int DSC = 200;
    public static final int CNC = 300;

    private static char prevCmnd = 0 ;
    
    public static int parseCommand(char _cmnd) {

        int operation = NOOP;

        switch (_cmnd) {

            case '1':
                if (prevCmnd == '4') {
                    operation = INC;
                }
                if (prevCmnd == '2') {
                    operation = DEC;
                }
                break;

            case '2':
                if (prevCmnd == '1') {
                    operation = INC;
                }
                if (prevCmnd == '3') {
                    operation = DEC;
                }
                break;

            case '3':
                if (prevCmnd == '2') {
                    operation = INC;
                }
                if (prevCmnd == '4') {
                    operation = DEC;
                }
                break;

            case '4':
                if (prevCmnd == '3') {
                    operation = INC;
                }
                if (prevCmnd == '1') {
                    operation = DEC;
                }
                break;

            case '*':
                operation = MUT;
                break;

            case '!':
                operation = DSC;
                break;

            case '#':
                operation = CNC;
                break;

        }
        
        prevCmnd = _cmnd ;
        
        return operation ;
        
    }

}
