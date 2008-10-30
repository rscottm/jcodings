/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to 
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.jcodings;

import org.jcodings.ascii.AsciiTables;
import org.jcodings.exception.EncodingException;
import org.jcodings.exception.ErrorMessages;
import org.jcodings.exception.IllegalCharacterException;
import org.jcodings.specific.ASCIIEncoding;

public abstract class MultiByteEncoding extends AbstractEncoding {

    protected final int EncLen[];

    protected static final int A = -1; // ACCEPT
    protected static final int F = -2; // FAILURE

    protected final int Trans[][];
    protected final int TransZero[];

    protected MultiByteEncoding(int minLength, int maxLength, int[]EncLen, int[][]Trans, short[]CTypeTable) {
        super(minLength, maxLength, CTypeTable);
        this.EncLen = EncLen;
        this.Trans = Trans;
        this.TransZero = Trans != null ? Trans[0] : null;
    }

    @Override
    public int length(byte c) {        
        return EncLen[c & 0xff];       
    }

    protected final int safeLengthForUptoFour(byte[]bytes, int p ,int end) {
        int b = bytes[p] & 0xff;
        int s = TransZero[b];
        if (s < 0) {
            if (s == A) return 1;
            throw IllegalCharacterException.INSTANCE;
        }
        return lengthForTwoUptoFour(bytes, p, end, b, s);
    }

    private int lengthForTwoUptoFour(byte[]bytes, int p, int end, int b, int s) {
        if (++p == end) return -(EncLen[b] - 1);
        s = Trans[s][bytes[p] & 0xff];
        if (s < 0) {
            if (s == A) return 2;
            throw IllegalCharacterException.INSTANCE;
        }
        return lengthForThreeUptoFour(bytes, p, end, b, s);
    }

    private int lengthForThreeUptoFour(byte[]bytes, int p, int end, int b, int s) {
        if (++p == end) return -(EncLen[b] - 2);
        s = Trans[s][bytes[p] & 0xff];
        if (s < 0) {
            if (s == A) return 3;
            throw IllegalCharacterException.INSTANCE;
        }
        if (++p == end) return -(EncLen[b] - 3);
        s = Trans[s][bytes[p] & 0xff];
        if (s == A) return 4;
        throw IllegalCharacterException.INSTANCE;  
    }

    protected final int safeLengthForUptoThree(byte[]bytes, int p, int end) {
        int b = bytes[p] & 0xff;
        int s = TransZero[b];
        if (s < 0) {
            if (s == A) return 1;
            throw IllegalCharacterException.INSTANCE;
        }
        return lengthForTwoUptoThree(bytes, p, end, b, s);
    }

    private int lengthForTwoUptoThree(byte[]bytes, int p, int end, int b, int s) {
        if (++p == end) return -(EncLen[b] - 1);
        s = Trans[s][bytes[p] & 0xff];
        if (s < 0) {
            if (s == A) return 2;
            throw IllegalCharacterException.INSTANCE;
        }
        return lengthForThree(bytes, p, end, b, s);
    }

    private int lengthForThree(byte[]bytes, int p, int end, int b, int s) {
        if (++p == end) return -(EncLen[b] - 2);
        s = Trans[s][bytes[p] & 0xff];
        if (s == A) return 3;
        throw IllegalCharacterException.INSTANCE;
    }     

    protected final int safeLengthForUptoTwo(byte[]bytes, int p, int end) {
        int b = bytes[p] & 0xff;
        int s = TransZero[b];
        
        if (s < 0) {
            if (s == A) return 1;
            throw IllegalCharacterException.INSTANCE;
        }
        return lengthForTwo(bytes, p, end, b, s);
    }    

    private int lengthForTwo(byte[]bytes, int p, int end, int b, int s) {
        if (++p == end) return -(EncLen[b] - 1);
        s = Trans[s][bytes[p] & 0xff];
        if (s == A) return 2;
        throw IllegalCharacterException.INSTANCE;
    }

    protected final int mbnMbcToCode(byte[]bytes, int p, int end) {
        int len = length(bytes, p, end);
        int n = bytes[p++] & 0xff;
        if (len == 1) return n;
        
        for (int i=1; i<len; i++) {
            if (p >= end) break;
            int c = bytes[p++] & 0xff;
            n <<= 8;
            n += c;
        }
        return n;
    }
    
    protected final int mbnMbcCaseFold(int flag, byte[]bytes, IntHolder pp, int end, byte[]lower) {
        int p = pp.value;
        int lowerP = 0;
        
        if (isAscii(bytes[p] & 0xff)) {
            lower[lowerP] = AsciiTables.ToLowerCaseTable[bytes[p] & 0xff];
            pp.value++;
            return 1;
        } else {
            int len = length(bytes, p, end);
            for (int i=0; i<len; i++) {
                lower[lowerP++] = bytes[p++];
            }
            pp.value += len;
            return len; /* return byte length of converted to lower char */
        }
    }

    protected final int mb2CodeToMbcLength(int code) {
        return ((code & 0xff00) != 0) ? 2 : 1;
    }
    
    protected final int mb4CodeToMbcLength(int code) {
        if ((code & 0xff000000) != 0) {
            return 4;
        } else if ((code & 0xff0000) != 0) {
            return 3;
        } else if ((code & 0xff00) != 0) {
            return 2;
        } else { 
            return 1;
        }
    }

    protected final int mb2CodeToMbc(int code, byte[]bytes, int p) {
        int p_ = p;
        if ((code & 0xff00) != 0) {
            bytes[p_++] = (byte)((code >>> 8) & 0xff);
        }
        bytes[p_++] = (byte)(code & 0xff);
        
        if (length(bytes, p, p_) != (p_ - p)) throw new EncodingException(ErrorMessages.ERR_INVALID_CODE_POINT_VALUE);
        return p_ - p;
    }

    protected final int mb4CodeToMbc(int code, byte[]bytes, int p) {        
        int p_ = p;        
        if ((code & 0xff000000) != 0)           bytes[p_++] = (byte)((code >>> 24) & 0xff);
        if ((code & 0xff0000) != 0 || p_ != p)  bytes[p_++] = (byte)((code >>> 16) & 0xff);
        if ((code & 0xff00) != 0 || p_ != p)    bytes[p_++] = (byte)((code >>> 8) & 0xff);
        bytes[p_++] = (byte)(code & 0xff);
        
        if (length(bytes, p, p_) != (p_ - p)) throw new EncodingException(ErrorMessages.ERR_INVALID_CODE_POINT_VALUE);
        return p_ - p;
    }

    protected final boolean mb2IsCodeCType(int code, int ctype) {
        if (code < 128) {            
            return isCodeCTypeInternal(code, ctype); // configured with ascii
        } else {
            if (isWordGraphPrint(ctype)) {
                return codeToMbcLength(code) > 1;
            }
        }
        return false;
    }

    protected final boolean mb4IsCodeCType(int code, int ctype) {
        return mb2IsCodeCType(code, ctype);
    }
}