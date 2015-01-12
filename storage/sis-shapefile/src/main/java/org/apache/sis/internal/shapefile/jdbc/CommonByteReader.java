/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.shapefile.jdbc;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.logging.Level;

import org.apache.sis.internal.shapefile.AutoChecker;

/**
 * Common byte reader.
 * @author  Marc Le Bihan
 * @version 0.5
 * @param <InvalidFormatException> Invalid format Exception to throw in case of trouble.
 * @param <FNFException> File not found Exception to throw in case of missing file.
 * @since   0.5
 * @module
 */
public abstract class CommonByteReader<InvalidFormatException extends Exception, FNFException extends Exception> extends AutoChecker  implements AutoCloseable  {
    /** The File. */
    private File file;

    /** Input Stream on the DBF. */
    private FileInputStream fis;

    /** File channel on the file. */
    private FileChannel fc;

    /** Buffer reader. */
    private MappedByteBuffer byteBuffer;

    /** Indicates if the byte buffer is closed. */
    private boolean isClosed = false;

    /** Invalid Exception to throw in case of invalid file format. */
    private Class<InvalidFormatException> classInvalidFormatException;

    /** Invalid Exception to throw in case of file not found exception. */
    private Class<FNFException> classFNFException;

    /**
     * Create and open a byte reader based on a file.
     * @param f File.
     * @param invalidFormatException Invalid Exception to throw in case of invalid file format.
     * @param fileNotFoundException Invalid Exception to throw in case of file not found exception.
     * @throws FNFException if the file cannot be opened.
     * @throws InvalidFormatException if the file format is invalid.
     */
    public CommonByteReader(File f, Class<InvalidFormatException> invalidFormatException, Class<FNFException> fileNotFoundException) throws FNFException, InvalidFormatException {
        Objects.requireNonNull(f, "The file cannot be null.");
        classInvalidFormatException = invalidFormatException;
        classFNFException = fileNotFoundException;

        file = f;

        try {
            fis = new FileInputStream(file);
        }
        catch(FileNotFoundException e) {
            throwException(classInvalidFormatException, e.getMessage(), e);
            throw new RuntimeException("this place should not be reached.");
        }

        fc = fis.getChannel();

        try {
            int fsize = (int)fc.size();
            byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fsize);
        }
        catch(IOException e) {
            String message = format(Level.WARNING, "excp.reader_cannot_be_created", file.getAbsolutePath(), e.getMessage());
            throwException(classFNFException, message, e);
            throw new RuntimeException("this place should not be reached.");
        }
   }

    /**
     * Close the MappedByteReader.
     * @throws IOException if the close operation fails.
     */
    @Override
    public void close() throws IOException {
        if (fc != null)
            fc.close();

        if (fis != null)
            fis.close();

        isClosed = true;
    }

    /**
     * Returns the closed state of this binary reader.
     * @return true if it is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Returns the byte buffer.
     * @return Byte Buffer.
     */
    public MappedByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Return the file mapped.
     * @return File.
     */
    public File getFile() {
        return file;
    }
}
