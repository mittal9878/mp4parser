/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.coremedia.iso;

import com.coremedia.iso.boxes.AbstractContainerBox;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * The most upper container for ISO Boxes. It is a container box that is a file.
 * Uses IsoBufferWrapper  to access the underlying file.
 */
public class IsoFile extends AbstractContainerBox {
    protected BoxParser boxParser = new PropertyBoxParserImpl();
    ReadableByteChannel byteChannel;

    public IsoFile(ReadableByteChannel byteChannel) {
        super(new byte[]{});
        this.byteChannel = byteChannel;
        boxParser = createBoxParser();

    }

    public IsoFile(ReadableByteChannel byteChannel, BoxParser boxParser) {
        this(byteChannel);
        this.boxParser = boxParser;
    }

    protected BoxParser createBoxParser() {
        return new PropertyBoxParserImpl();
    }


    @Override
    public void _parseDetails() {
        // there are no details to parse we should be just file
    }

    public void parse() throws IOException {

        boolean done = false;
        while (!done) {
            try {
                Box box = boxParser.parseBox(byteChannel, this);
                if (box != null) {
                    boxes.add(box);
                } else {
                    done = true;
                }
            } catch (EOFException e) {
                done = true;
            }
        }
    }


    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("IsoFile[");
        if (boxes == null) {
            buffer.append("unparsed");
        } else {
            for (int i = 0; i < boxes.size(); i++) {
                if (i > 0) {
                    buffer.append(";");
                }
                buffer.append(boxes.get(i).toString());
            }
        }
        buffer.append("]");
        return buffer.toString();
    }

    public static byte[] fourCCtoBytes(String fourCC) {
        byte[] result = new byte[4];
        if (fourCC != null) {
            for (int i = 0; i < Math.min(4, fourCC.length()); i++) {
                result[i] = (byte) fourCC.charAt(i);
            }
        }
        return result;
    }

    public static String bytesToFourCC(byte[] type) {
        byte[] result = new byte[]{0, 0, 0, 0};
        if (type != null) {
            System.arraycopy(type, 0, result, 0, Math.min(type.length, 4));
        }
        try {
            return new String(result, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new Error("Required character encoding is missing", e);
        }
    }


    @Override
    public long getNumOfBytesToFirstChild() {
        return 0;
    }

    @Override
    public long getSize() {
        long size = 0;
        for (Box box : boxes) {
            size += box.getSize();
        }
        return size;
    }

    @Override
    public IsoFile getIsoFile() {
        return this;
    }

    @Override
    protected long getHeaderSize() {
        return 0;
    }


    /**
     * Shortcut to get the MovieBox since it is often needed and present in
     * nearly all ISO 14496 files (at least if they are derived from MP4 ).
     *
     * @return the MovieBox or <code>null</code>
     */
    public MovieBox getMovieBox() {
        for (Box box : boxes) {
            if (box instanceof MovieBox) {
                return (MovieBox) box;
            }
        }
        return null;
    }
}