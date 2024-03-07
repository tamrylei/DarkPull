package com.tengban.sdk.base.log;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 从Apache commons io拷贝过来的
 * 简单改写了一下, 忽略了编码, Android上默认UTF-8的, 暂时不对外使用
 */
/* package */ class ReversedLinesReader {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private final int blockSize;

    private final RandomAccessFile randomAccessFile;

    private final long totalByteLength;
    private final long totalBlockCount;

    private final byte[][] newLineSequences;
    private final int avoidNewlineSplitBufferSize;
    private final int byteDecrement;

    private FilePart currentFilePart;

    private boolean trailingNewlineOfFileSkipped = false;

    public ReversedLinesReader(final RandomAccessFile file, final int blockSize) throws IOException {
        this.blockSize = blockSize;

        // 默认UTF-8编码, 写死1, 实际不同编码还是有点差别
        byteDecrement = 1;

        newLineSequences = new byte[][] {
            "\r\n".getBytes(DEFAULT_CHARSET),
            "\n".getBytes(DEFAULT_CHARSET),
            "\r".getBytes(DEFAULT_CHARSET) };

        avoidNewlineSplitBufferSize = newLineSequences[0].length;

        // Open file
        randomAccessFile = file;
        totalByteLength = randomAccessFile.length();
        int lastBlockLength = (int) (totalByteLength % blockSize);
        if (lastBlockLength > 0) {
            totalBlockCount = totalByteLength / blockSize + 1;
        } else {
            totalBlockCount = totalByteLength / blockSize;
            if (totalByteLength > 0) {
                lastBlockLength = blockSize;
            }
        }
        currentFilePart = new FilePart(totalBlockCount, lastBlockLength, null);
    }

    /**
     * Returns the lines of the file from bottom to top.
     *
     * @return the next line or null if the start of the file is reached
     * @throws IOException  if an I/O error occurs
     */
    public String readLine() throws IOException {

        String line = currentFilePart.readLine();
        while (line == null) {
            currentFilePart = currentFilePart.rollOver();
            if (currentFilePart != null) {
                line = currentFilePart.readLine();
            } else {
                // no more fileparts: we're done, leave line set to null
                break;
            }
        }

        // aligned behaviour with BufferedReader that doesn't return a last, empty line
        if("".equals(line) && !trailingNewlineOfFileSkipped) {
            trailingNewlineOfFileSkipped = true;
            line = readLine();
        }

        return line;
    }

    private class FilePart {
        private final long no;

        private final byte[] data;

        private byte[] leftOver;

        private int currentLastBytePos;

        /**
         * ctor
         * @param no the part number
         * @param length its length
         * @param leftOverOfLastFilePart remainder
         * @throws IOException if there is a problem reading the file
         */
        private FilePart(final long no, final int length, final byte[] leftOverOfLastFilePart) throws IOException {
            this.no = no;
            final int dataLength = length + (leftOverOfLastFilePart != null ? leftOverOfLastFilePart.length : 0);
            this.data = new byte[dataLength];
            final long off = (no - 1) * blockSize;

            // read data
            if (no > 0 /* file not empty */) {
                randomAccessFile.seek(off);
                final int countRead = randomAccessFile.read(data, 0, length);
                if (countRead != length) {
                    throw new IllegalStateException("Count of requested bytes and actually read bytes don't match");
                }
            }
            // copy left over part into data arr
            if (leftOverOfLastFilePart != null) {
                System.arraycopy(leftOverOfLastFilePart, 0, data, length, leftOverOfLastFilePart.length);
            }
            this.currentLastBytePos = data.length - 1;
            this.leftOver = null;
        }

        /**
         * Handles block rollover
         *
         * @return the new FilePart or null
         * @throws IOException if there was a problem reading the file
         */
        private FilePart rollOver() throws IOException {

            if (currentLastBytePos > -1) {
                throw new IllegalStateException("Current currentLastCharPos unexpectedly positive... "
                        + "last readLine() should have returned something! currentLastCharPos=" + currentLastBytePos);
            }

            if (no > 1) {
                return new FilePart(no - 1, blockSize, leftOver);
            } else {
                // NO 1 was the last FilePart, we're finished
                if (leftOver != null) {
                    throw new IllegalStateException("Unexpected leftover of the last block: leftOverOfThisFilePart="
                            + new String(leftOver, DEFAULT_CHARSET));
                }
                return null;
            }
        }

        /**
         * Reads a line.
         *
         * @return the line or null
         * @throws IOException if there is an error reading from the file
         */
        private String readLine() throws IOException {

            String line = null;
            int newLineMatchByteCount;

            final boolean isLastFilePart = no == 1;

            int i = currentLastBytePos;
            while (i > -1) {

                if (!isLastFilePart && i < avoidNewlineSplitBufferSize) {
                    // avoidNewlineSplitBuffer: for all except the last file part we
                    // take a few bytes to the next file part to avoid splitting of newlines
                    createLeftOver();
                    break; // skip last few bytes and leave it to the next file part
                }

                // --- check for newline ---
                if ((newLineMatchByteCount = getNewLineMatchByteCount(data, i)) > 0 /* found newline */) {
                    final int lineStart = i + 1;
                    final int lineLengthBytes = currentLastBytePos - lineStart + 1;

                    if (lineLengthBytes < 0) {
                        throw new IllegalStateException("Unexpected negative line length="+lineLengthBytes);
                    }
                    final byte[] lineData = new byte[lineLengthBytes];
                    System.arraycopy(data, lineStart, lineData, 0, lineLengthBytes);

                    line = new String(lineData, DEFAULT_CHARSET);

                    currentLastBytePos = i - newLineMatchByteCount;
                    break; // found line
                }

                // --- move cursor ---
                i -= byteDecrement;

                // --- end of file part handling ---
                if (i < 0) {
                    createLeftOver();
                    break; // end of file part
                }
            }

            // --- last file part handling ---
            if (isLastFilePart && leftOver != null) {
                // there will be no line break anymore, this is the first line of the file
                line = new String(leftOver, DEFAULT_CHARSET);
                leftOver = null;
            }

            return line;
        }

        /**
         * Creates the buffer containing any left over bytes.
         */
        private void createLeftOver() {
            final int lineLengthBytes = currentLastBytePos + 1;
            if (lineLengthBytes > 0) {
                // create left over for next block
                leftOver = new byte[lineLengthBytes];
                System.arraycopy(data, 0, leftOver, 0, lineLengthBytes);
            } else {
                leftOver = null;
            }
            currentLastBytePos = -1;
        }

        /**
         * Finds the new-line sequence and return its length.
         *
         * @param data buffer to scan
         * @param i start offset in buffer
         * @return length of newline sequence or 0 if none found
         */
        private int getNewLineMatchByteCount(final byte[] data, final int i) {
            for (final byte[] newLineSequence : newLineSequences) {
                boolean match = true;
                for (int j = newLineSequence.length - 1; j >= 0; j--) {
                    final int k = i + j - (newLineSequence.length - 1);
                    match &= k >= 0 && data[k] == newLineSequence[j];
                }
                if (match) {
                    return newLineSequence.length;
                }
            }
            return 0;
        }
    }
}
