package com.tengban.sdk.base.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Murmur3Hash {

    public interface HashCode {
        int toInt();
        long toLong();
        byte[] toBytes();
    }

    public abstract Murmur3Hash putByte(byte b);
    public abstract Murmur3Hash putBytes(byte[] bytes);
    public abstract Murmur3Hash putBytes(byte[] bytes, int off, int len);
    public abstract Murmur3Hash putShort(short s);
    public abstract Murmur3Hash putInt(int i);
    public abstract Murmur3Hash putLong(long l);
    public abstract Murmur3Hash putFloat(float f);
    public abstract Murmur3Hash putDouble(double d);
    public abstract Murmur3Hash putBoolean(boolean b);
    public abstract Murmur3Hash putChar(char c);
    public abstract Murmur3Hash putString(String string);

    public abstract Murmur3Hash reset();
    public abstract HashCode hash();

    public static Murmur3Hash hash32() {
        return new Murmur3Hash32();
    }

    public static Murmur3Hash hash128() {
        return new Murmur3Hash128();
    }

    static class IntHashCode implements HashCode {

        final int hash;

        IntHashCode(int hash) {
            this.hash = hash;
        }

        @Override
        public int toInt() {
            return hash;
        }

        @Override
        public long toLong() {
            return hash & 0xffffffffL;
        }

        @Override
        public byte[] toBytes() {
            return new byte[] {
                    (byte) hash,
                    (byte) (hash >> 8),
                    (byte) (hash >> 16),
                    (byte) (hash >> 24)
            };
        }
    }

    static class BytesHashCode implements HashCode {

        final byte[] bytes;

        BytesHashCode(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int toInt() {
            if(bytes.length >= 4) {
                return (bytes[0] & 0xFF)
                        | ((bytes[1] & 0xFF) << 8)
                        | ((bytes[2] & 0xFF) << 16)
                        | ((bytes[3] & 0xFF) << 24);
            }

            return 0;
        }

        @Override
        public long toLong() {
            if(bytes.length >= 8) {
                long retVal = (bytes[0] & 0xFF);
                for (int i = 1; i < Math.min(bytes.length, 8); i++) {
                    retVal |= (bytes[i] & 0xFFL) << (i * 8);
                }
                return retVal;
            }

            return 0L;
        }

        @Override
        public byte[] toBytes() {
            return bytes;
        }
    }

    static abstract class Murmur3HashBase extends Murmur3Hash {

        private final ByteBuffer buffer;
        private final int bufferSize;
        private final int chunkSize;

        Murmur3HashBase(int chunkSize) {
            this(chunkSize, chunkSize);
        }

        Murmur3HashBase(int chunkSize, int bufferSize) {
            this.buffer = ByteBuffer.allocate(bufferSize + 7).order(ByteOrder.LITTLE_ENDIAN);
            this.bufferSize = bufferSize;
            this.chunkSize = chunkSize;
        }

        @Override
        public final Murmur3Hash putByte(byte b) {
            buffer.put(b);
            munchIfFull();
            return this;
        }

        @Override
        public final Murmur3Hash putBytes(byte[] bytes) {
            return putBytes(bytes, 0, bytes.length);
        }

        @Override
        public final Murmur3Hash putBytes(byte[] bytes, int off, int len) {
            return putBytes(ByteBuffer.wrap(bytes, off, len).order(ByteOrder.LITTLE_ENDIAN));
        }

        @Override
        public final Murmur3Hash putShort(short s) {
            buffer.putShort(s);
            munchIfFull();
            return this;
        }

        @Override
        public final Murmur3Hash putInt(int i) {
            buffer.putInt(i);
            munchIfFull();
            return this;
        }

        @Override
        public final Murmur3Hash putLong(long l) {
            buffer.putLong(l);
            munchIfFull();
            return this;
        }

        @Override
        public final Murmur3Hash putDouble(double d) {
            return putLong(Double.doubleToRawLongBits(d));
        }

        @Override
        public final Murmur3Hash putFloat(float f) {
            return putInt(Float.floatToRawIntBits(f));
        }

        @Override
        public final Murmur3Hash putBoolean(boolean b) {
            return putByte(b ? (byte) 1 : (byte) 0);
        }

        @Override
        public final Murmur3Hash putChar(char c) {
            buffer.putChar(c);
            munchIfFull();
            return this;
        }

        @Override
        public final Murmur3Hash putString(String string) {
            for (int i = 0; i < string.length(); i++) {
                putChar(string.charAt(i));
            }
            return this;
        }

        @Override
        public Murmur3Hash reset() {
            buffer.clear();

            doReset();

            return this;
        }

        @Override
        public HashCode hash() {
            munch();
            buffer.flip();
            if (buffer.remaining() > 0) {
                processRemaining(buffer);
            }

            return makeHash();
        }

        protected abstract void doReset();

        protected abstract HashCode makeHash();

        protected abstract void process(ByteBuffer bb);

        protected void processRemaining(ByteBuffer bb) {
            bb.position(bb.limit()); // move at the end
            bb.limit(chunkSize + 7); // get ready to pad with longs
            while (bb.position() < chunkSize) {
                bb.putLong(0);
            }
            bb.limit(chunkSize);
            bb.flip();
            process(bb);
        }

        private Murmur3HashBase putBytes(ByteBuffer readBuffer) {
            // If we have room for all of it, this is easy
            if (readBuffer.remaining() <= buffer.remaining()) {
                buffer.put(readBuffer);
                munchIfFull();
                return this;
            }

            // First add just enough to fill buffer size, and munch that
            int bytesToCopy = bufferSize - buffer.position();
            for (int i = 0; i < bytesToCopy; i++) {
                buffer.put(readBuffer.get());
            }
            munch(); // buffer becomes empty here, since chunkSize divides bufferSize

            // Now process directly from the rest of the input buffer
            while (readBuffer.remaining() >= chunkSize) {
                process(readBuffer);
            }

            // Finally stick the remainder back in our usual buffer
            buffer.put(readBuffer);
            return this;
        }

        private void munchIfFull() {
            if (buffer.remaining() < 8) {
                // buffer is full; not enough room for a primitive. We have at least one full chunk.
                munch();
            }
        }

        private void munch() {
            buffer.flip();
            while (buffer.remaining() >= chunkSize) {
                // we could limit the buffer to ensure process() does not read more than
                // chunkSize number of bytes, but we trust the implementations
                process(buffer);
            }
            buffer.compact(); // preserve any remaining data that do not make a full chunk
        }

        public static int toInt(byte value) {
            return value & 0xFF;
        }
    }

    static class Murmur3Hash32 extends Murmur3HashBase {

        private static final int C1 = 0xcc9e2d51;
        private static final int C2 = 0x1b873593;

        private static final int CHUNK_SIZE = 4;
        private int h1;
        private int length;

        Murmur3Hash32() {
            super(CHUNK_SIZE);

            this.h1 = 0;
            this.length = 0;
        }

        @Override
        protected void process(ByteBuffer bb) {
            int k1 = mixK1(bb.getInt());
            h1 = mixH1(h1, k1);
            length += CHUNK_SIZE;
        }

        @Override
        protected void processRemaining(ByteBuffer bb) {
            length += bb.remaining();
            int k1 = 0;
            for (int i = 0; bb.hasRemaining(); i += 8) {
                k1 ^= toInt(bb.get()) << i;
            }
            h1 ^= mixK1(k1);
        }

        @Override
        protected void doReset() {
            this.h1 = 0;
            this.length = 0;
        }

        @Override
        public HashCode makeHash() {
            final int hash = fmix(h1, length);
            return new IntHashCode(hash);
        }

        private static int mixK1(int k1) {
            k1 *= C1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= C2;
            return k1;
        }

        private static int mixH1(int h1, int k1) {
            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
            return h1;
        }

        private static int fmix(int h1, int length) {
            h1 ^= length;
            h1 ^= h1 >>> 16;
            h1 *= 0x85ebca6b;
            h1 ^= h1 >>> 13;
            h1 *= 0xc2b2ae35;
            h1 ^= h1 >>> 16;
            return h1;
        }
    }

    static class Murmur3Hash128 extends Murmur3HashBase {

        private static final int CHUNK_SIZE = 16;
        private static final long C1 = 0x87c37b91114253d5L;
        private static final long C2 = 0x4cf5ad432745937fL;

        private long h1;
        private long h2;
        private int length;

        Murmur3Hash128() {
            super(CHUNK_SIZE);

            this.h1 = 0;
            this.h2 = 0;
            this.length = 0;
        }

        @Override
        protected void process(ByteBuffer bb) {
            long k1 = bb.getLong();
            long k2 = bb.getLong();
            bmix64(k1, k2);
            length += CHUNK_SIZE;
        }

        @Override
        protected void processRemaining(ByteBuffer bb) {
            long k1 = 0;
            long k2 = 0;
            length += bb.remaining();
            switch (bb.remaining()) {
                case 15:
                    k2 ^= (long) toInt(bb.get(14)) << 48; // fall through
                case 14:
                    k2 ^= (long) toInt(bb.get(13)) << 40; // fall through
                case 13:
                    k2 ^= (long) toInt(bb.get(12)) << 32; // fall through
                case 12:
                    k2 ^= (long) toInt(bb.get(11)) << 24; // fall through
                case 11:
                    k2 ^= (long) toInt(bb.get(10)) << 16; // fall through
                case 10:
                    k2 ^= (long) toInt(bb.get(9)) << 8; // fall through
                case 9:
                    k2 ^= (long) toInt(bb.get(8)); // fall through
                case 8:
                    k1 ^= bb.getLong();
                    break;
                case 7:
                    k1 ^= (long) toInt(bb.get(6)) << 48; // fall through
                case 6:
                    k1 ^= (long) toInt(bb.get(5)) << 40; // fall through
                case 5:
                    k1 ^= (long) toInt(bb.get(4)) << 32; // fall through
                case 4:
                    k1 ^= (long) toInt(bb.get(3)) << 24; // fall through
                case 3:
                    k1 ^= (long) toInt(bb.get(2)) << 16; // fall through
                case 2:
                    k1 ^= (long) toInt(bb.get(1)) << 8; // fall through
                case 1:
                    k1 ^= (long) toInt(bb.get(0));
                    break;
                default:
                    throw new AssertionError("Should never get here.");
            }
            h1 ^= mixK1(k1);
            h2 ^= mixK2(k2);
        }

        @Override
        protected void doReset() {
            this.h1 = 0;
            this.h2 = 0;
            this.length = 0;
        }

        @Override
        public HashCode makeHash() {
            h1 ^= length;
            h2 ^= length;

            h1 += h2;
            h2 += h1;

            h1 = fmix64(h1);
            h2 = fmix64(h2);

            h1 += h2;
            h2 += h1;

            return new BytesHashCode(ByteBuffer.wrap(new byte[CHUNK_SIZE])
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(h1)
                    .putLong(h2)
                    .array());
        }

        private void bmix64(long k1, long k2) {
            h1 ^= mixK1(k1);

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            h2 ^= mixK2(k2);

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        private static long fmix64(long k) {
            k ^= k >>> 33;
            k *= 0xff51afd7ed558ccdL;
            k ^= k >>> 33;
            k *= 0xc4ceb9fe1a85ec53L;
            k ^= k >>> 33;
            return k;
        }

        private static long mixK1(long k1) {
            k1 *= C1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= C2;
            return k1;
        }

        private static long mixK2(long k2) {
            k2 *= C2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= C1;
            return k2;
        }
    }
}
