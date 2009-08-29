/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.netty.handler.codec.http2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default factory giving Attribute and FileUpload according to constructor
 *
 * Attribute and FileUpload could be :<br>
 * - MemoryAttribute, DiskAttribute or MixedAttribute<br>
 * - MemoryFileUpload, DiskFileUpload or MixedFileUpload<br>
 * according to the constructor.
 *
 * @author frederic bregier
 *
 */
public class DefaultHttpDataFactory implements HttpDataFactory {
    /**
     * Proposed default MINSIZE as 16 KB.
     */
    public static long MINSIZE = 0x4000;

    private boolean useDisk = false;

    private boolean checkSize = false;

    private long minSize = 0L;

    /**
     * Keep all HttpDatas until cleanAllHttpDatas() is called.
     */
    private List<FileHttpData> fileToDelete = new ArrayList<FileHttpData>();

    /**
     * HttpData will be in memory if less than default size (16KB).
     * The type will be Mixed.
     */
    public DefaultHttpDataFactory() {
        useDisk = false;
        checkSize = true;
        this.minSize = MINSIZE;
    }

    /**
     * HttpData will be always on Disk if useDisk is True, else always in Memory if False
     * @param useDisk
     */
    public DefaultHttpDataFactory(boolean useDisk) {
        this.useDisk = useDisk;
        checkSize = false;
    }

    /**
     * HttpData will be on Disk if the size of the file is greater than minSize, else it
     * will be in memory. The type will be Mixed.
     * @param minSize
     */
    public DefaultHttpDataFactory(long minSize) {
        useDisk = false;
        checkSize = true;
        this.minSize = minSize;
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.http2.HttpDataFactory#createAttribute(java.lang.String)
     */
    @Override
    public Attribute createAttribute(String name) throws NullPointerException,
            IllegalArgumentException {
        if (useDisk) {
            Attribute attribute = new DiskAttribute(name);
            fileToDelete.add(attribute);
            return attribute;
        } else if (checkSize) {
            Attribute attribute = new MixedAttribute(name, minSize);
            fileToDelete.add(attribute);
            return attribute;
        }
        return new MemoryAttribute(name);
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.http2.HttpDataFactory#createAttribute(java.lang.String, java.lang.String)
     */
    public Attribute createAttribute(String name, String value)
            throws NullPointerException, IllegalArgumentException {
        if (useDisk) {
            Attribute attribute;
            try {
                attribute = new DiskAttribute(name, value);
            } catch (IOException e) {
                // revert to Mixed mode
                attribute = new MixedAttribute(name, value, minSize);
            }
            fileToDelete.add(attribute);
            return attribute;
        } else if (checkSize) {
            Attribute attribute = new MixedAttribute(name, value, minSize);
            fileToDelete.add(attribute);
            return attribute;
        }
        try {
            return new MemoryAttribute(name, value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.http2.HttpDataFactory#createFileUpload(java.lang.String, java.lang.String, java.lang.String)
     */
    public FileUpload createFileUpload(String name, String filename,
            String contentType, String contentTransferEncoding, String charset,
            long size) throws NullPointerException, IllegalArgumentException {
        if (useDisk) {
            FileUpload fileUpload = new DiskFileUpload(name, filename, contentType,
                    contentTransferEncoding, charset, size);
            fileToDelete.add(fileUpload);
            return fileUpload;
        } else if (checkSize) {
            FileUpload fileUpload = new MixedFileUpload(name, filename, contentType,
                    contentTransferEncoding, charset, size, minSize);
            fileToDelete.add(fileUpload);
            return fileUpload;
        }
        return new MemoryFileUpload(name, filename, contentType,
                contentTransferEncoding, charset, size);
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.http2.HttpDataFactory#removeHttpDataFromClean(org.jboss.netty.handler.codec.http2.HttpData)
     */
    @Override
    public void removeHttpDataFromClean(HttpData data) {
        if (data instanceof FileHttpData) {
            fileToDelete.remove(data);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.http2.HttpDataFactory#cleanAllHttpData()
     */
    @Override
    public void cleanAllHttpDatas() {
        for (FileHttpData data: fileToDelete) {
            data.delete();
        }
        fileToDelete.clear();
    }

}
