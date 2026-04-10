package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class SimpleQrCodeGenerator {

    private static final int VERSION_4_SIZE = 33;
    private static final int DATA_CODEWORDS = 80;
    private static final int ECC_CODEWORDS = 20;
    private static final int FORMAT_BITS_L_MASK_0 = 0x77C4;

    private SimpleQrCodeGenerator() {
    }

    public static BufferedImage generate(String content, int scale, int marginModules) {
        byte[] utf8 = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        if (utf8.length > 78) {
            throw new IllegalArgumentException("QR payload too long for the built-in generator.");
        }

        boolean[][] modules = new boolean[VERSION_4_SIZE][VERSION_4_SIZE];
        boolean[][] functionModules = new boolean[VERSION_4_SIZE][VERSION_4_SIZE];

        drawFunctionPatterns(modules, functionModules);
        byte[] codewords = buildCodewords(utf8);
        placeDataBits(modules, functionModules, codewords);
        drawFormatBits(modules, functionModules);

        return renderImage(modules, Math.max(2, scale), Math.max(2, marginModules));
    }

    private static void drawFunctionPatterns(boolean[][] modules, boolean[][] functionModules) {
        drawFinderPattern(modules, functionModules, 0, 0);
        drawFinderPattern(modules, functionModules, VERSION_4_SIZE - 7, 0);
        drawFinderPattern(modules, functionModules, 0, VERSION_4_SIZE - 7);
        drawAlignmentPattern(modules, functionModules, 26, 26);

        for (int index = 8; index < VERSION_4_SIZE - 8; index++) {
            setFunctionModule(modules, functionModules, index, 6, index % 2 == 0);
            setFunctionModule(modules, functionModules, 6, index, index % 2 == 0);
        }

        reserveFormatAreas(modules, functionModules);
        setFunctionModule(modules, functionModules, 8, VERSION_4_SIZE - 8, true);
    }

    private static void drawFinderPattern(boolean[][] modules, boolean[][] functionModules, int x, int y) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int xx = x + dx;
                int yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= VERSION_4_SIZE || yy >= VERSION_4_SIZE) {
                    continue;
                }

                boolean black = dx >= 0
                        && dx <= 6
                        && dy >= 0
                        && dy <= 6
                        && (dx == 0
                                || dx == 6
                                || dy == 0
                                || dy == 6
                                || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
                setFunctionModule(modules, functionModules, xx, yy, black);
            }
        }
    }

    private static void drawAlignmentPattern(boolean[][] modules, boolean[][] functionModules, int centerX, int centerY) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dy));
                setFunctionModule(modules, functionModules, centerX + dx, centerY + dy, distance != 1);
            }
        }
    }

    private static void reserveFormatAreas(boolean[][] modules, boolean[][] functionModules) {
        int[][] primary = {
                { 8, 0 }, { 8, 1 }, { 8, 2 }, { 8, 3 }, { 8, 4 }, { 8, 5 }, { 8, 7 }, { 8, 8 }, { 7, 8 },
                { 5, 8 }, { 4, 8 }, { 3, 8 }, { 2, 8 }, { 1, 8 }, { 0, 8 }
        };
        int[][] secondary = {
                { VERSION_4_SIZE - 1, 8 }, { VERSION_4_SIZE - 2, 8 }, { VERSION_4_SIZE - 3, 8 }, { VERSION_4_SIZE - 4, 8 },
                { VERSION_4_SIZE - 5, 8 }, { VERSION_4_SIZE - 6, 8 }, { VERSION_4_SIZE - 7, 8 }, { 8, VERSION_4_SIZE - 8 },
                { 8, VERSION_4_SIZE - 7 }, { 8, VERSION_4_SIZE - 6 }, { 8, VERSION_4_SIZE - 5 }, { 8, VERSION_4_SIZE - 4 },
                { 8, VERSION_4_SIZE - 3 }, { 8, VERSION_4_SIZE - 2 }, { 8, VERSION_4_SIZE - 1 }
        };

        for (int[] coordinate : primary) {
            setFunctionModule(modules, functionModules, coordinate[0], coordinate[1], false);
        }
        for (int[] coordinate : secondary) {
            setFunctionModule(modules, functionModules, coordinate[0], coordinate[1], false);
        }
    }

    private static byte[] buildCodewords(byte[] data) {
        BitBuffer buffer = new BitBuffer();
        buffer.appendBits(0b0100, 4);
        buffer.appendBits(data.length, 8);
        for (byte value : data) {
            buffer.appendBits(value & 0xFF, 8);
        }

        int capacityBits = DATA_CODEWORDS * 8;
        buffer.appendBits(0, Math.min(4, capacityBits - buffer.length()));
        buffer.appendToByteBoundary();

        byte[] dataCodewords = Arrays.copyOf(buffer.toByteArray(), DATA_CODEWORDS);
        int padIndex = buffer.toByteArray().length;
        int[] padBytes = { 0xEC, 0x11 };
        for (int index = padIndex; index < DATA_CODEWORDS; index++) {
            dataCodewords[index] = (byte) padBytes[(index - padIndex) % padBytes.length];
        }

        byte[] ecc = computeEcc(dataCodewords, ECC_CODEWORDS);
        byte[] result = new byte[dataCodewords.length + ecc.length];
        System.arraycopy(dataCodewords, 0, result, 0, dataCodewords.length);
        System.arraycopy(ecc, 0, result, dataCodewords.length, ecc.length);
        return result;
    }

    private static byte[] computeEcc(byte[] dataCodewords, int degree) {
        int[] generator = buildGeneratorPolynomial(degree);
        int[] remainder = new int[degree];

        for (byte codeword : dataCodewords) {
            int factor = (codeword & 0xFF) ^ remainder[0];
            System.arraycopy(remainder, 1, remainder, 0, degree - 1);
            remainder[degree - 1] = 0;
            for (int index = 0; index < degree; index++) {
                remainder[index] ^= multiplyGf(generator[index], factor);
            }
        }

        byte[] ecc = new byte[degree];
        for (int index = 0; index < degree; index++) {
            ecc[index] = (byte) remainder[index];
        }
        return ecc;
    }

    private static int[] buildGeneratorPolynomial(int degree) {
        int[] polynomial = { 1 };
        int root = 1;

        for (int iteration = 0; iteration < degree; iteration++) {
            int[] next = new int[polynomial.length + 1];
            for (int index = 0; index < polynomial.length; index++) {
                next[index] ^= polynomial[index];
                next[index + 1] ^= multiplyGf(polynomial[index], root);
            }
            polynomial = next;
            root = multiplyGf(root, 0x02);
        }

        return Arrays.copyOfRange(polynomial, 1, polynomial.length);
    }

    private static int multiplyGf(int x, int y) {
        int result = 0;
        int a = x;
        int b = y;
        while (b != 0) {
            if ((b & 1) != 0) {
                result ^= a;
            }
            a <<= 1;
            if ((a & 0x100) != 0) {
                a ^= 0x11D;
            }
            b >>>= 1;
        }
        return result;
    }

    private static void placeDataBits(boolean[][] modules, boolean[][] functionModules, byte[] codewords) {
        int bitIndex = 0;
        boolean upward = true;

        for (int right = VERSION_4_SIZE - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right--;
            }

            for (int offset = 0; offset < VERSION_4_SIZE; offset++) {
                int y = upward ? VERSION_4_SIZE - 1 - offset : offset;
                for (int delta = 0; delta < 2; delta++) {
                    int x = right - delta;
                    if (functionModules[y][x]) {
                        continue;
                    }

                    boolean bit = false;
                    if (bitIndex < codewords.length * 8) {
                        bit = ((codewords[bitIndex >>> 3] >>> (7 - (bitIndex & 7))) & 1) != 0;
                        bitIndex++;
                    }

                    if (((x + y) & 1) == 0) {
                        bit = !bit;
                    }
                    modules[y][x] = bit;
                }
            }
            upward = !upward;
        }
    }

    private static void drawFormatBits(boolean[][] modules, boolean[][] functionModules) {
        int[][] primary = {
                { 8, 0 }, { 8, 1 }, { 8, 2 }, { 8, 3 }, { 8, 4 }, { 8, 5 }, { 8, 7 }, { 8, 8 }, { 7, 8 },
                { 5, 8 }, { 4, 8 }, { 3, 8 }, { 2, 8 }, { 1, 8 }, { 0, 8 }
        };
        int[][] secondary = {
                { VERSION_4_SIZE - 1, 8 }, { VERSION_4_SIZE - 2, 8 }, { VERSION_4_SIZE - 3, 8 }, { VERSION_4_SIZE - 4, 8 },
                { VERSION_4_SIZE - 5, 8 }, { VERSION_4_SIZE - 6, 8 }, { VERSION_4_SIZE - 7, 8 }, { 8, VERSION_4_SIZE - 8 },
                { 8, VERSION_4_SIZE - 7 }, { 8, VERSION_4_SIZE - 6 }, { 8, VERSION_4_SIZE - 5 }, { 8, VERSION_4_SIZE - 4 },
                { 8, VERSION_4_SIZE - 3 }, { 8, VERSION_4_SIZE - 2 }, { 8, VERSION_4_SIZE - 1 }
        };

        for (int bit = 0; bit < 15; bit++) {
            boolean value = ((FORMAT_BITS_L_MASK_0 >>> bit) & 1) != 0;
            setFunctionModule(modules, functionModules, primary[bit][0], primary[bit][1], value);
            setFunctionModule(modules, functionModules, secondary[bit][0], secondary[bit][1], value);
        }
    }

    private static void setFunctionModule(boolean[][] modules, boolean[][] functionModules, int x, int y, boolean black) {
        modules[y][x] = black;
        functionModules[y][x] = true;
    }

    private static BufferedImage renderImage(boolean[][] modules, int scale, int marginModules) {
        int imageSize = (VERSION_4_SIZE + (marginModules * 2)) * scale;
        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, imageSize, imageSize);
            graphics.setColor(Color.BLACK);

            for (int y = 0; y < VERSION_4_SIZE; y++) {
                for (int x = 0; x < VERSION_4_SIZE; x++) {
                    if (!modules[y][x]) {
                        continue;
                    }

                    int drawX = (x + marginModules) * scale;
                    int drawY = (y + marginModules) * scale;
                    graphics.fillRect(drawX, drawY, scale, scale);
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static final class BitBuffer {

        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private int currentByte;
        private int bitCount;

        public void appendBits(int value, int length) {
            for (int index = length - 1; index >= 0; index--) {
                currentByte = (currentByte << 1) | ((value >>> index) & 1);
                bitCount++;
                if (bitCount == 8) {
                    bytes.write(currentByte);
                    currentByte = 0;
                    bitCount = 0;
                }
            }
        }

        public void appendToByteBoundary() {
            if (bitCount == 0) {
                return;
            }
            appendBits(0, 8 - bitCount);
        }

        public int length() {
            return (bytes.size() * 8) + bitCount;
        }

        public byte[] toByteArray() {
            byte[] completed = bytes.toByteArray();
            if (bitCount == 0) {
                return completed;
            }

            byte[] withPartial = Arrays.copyOf(completed, completed.length + 1);
            withPartial[withPartial.length - 1] = (byte) (currentByte << (8 - bitCount));
            return withPartial;
        }
    }
}
