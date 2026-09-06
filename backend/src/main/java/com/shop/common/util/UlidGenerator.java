package com.shop.common.util;

import java.security.SecureRandom;
import java.time.Instant;

public final class UlidGenerator {
    // ULID 생성기
    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidGenerator() {
    }

    /**
     * ULID 생성
     * 
     * @return ULID
     */
    public static String nextUlid() {
        long timestamp = Instant.now().toEpochMilli();
        byte[] randomness = new byte[10];
        RANDOM.nextBytes(randomness);

        char[] chars = new char[26];
        encodeTimestamp(timestamp, chars);
        encodeRandomness(randomness, chars);
        return new String(chars);
    }

    /**
     * 타임스탬프 인코딩
     * 
     * @param timestamp 타임스탬프
     * @param out       출력 배열
     */
    private static void encodeTimestamp(long timestamp, char[] out) {
        for (int i = 9; i >= 0; i--) {
            out[i] = CROCKFORD_BASE32[(int) (timestamp & 31)];
            timestamp >>>= 5;
        }
    }

    /**
     * 랜덤성 인코딩
     * 
     * @param randomness 랜덤성
     * @param out        출력 배열
     */
    private static void encodeRandomness(byte[] randomness, char[] out) {
        int bitBuffer = 0;
        int bitsInBuffer = 0;
        int outIndex = 10;

        for (byte value : randomness) {
            bitBuffer = (bitBuffer << 8) | (value & 0xFF);
            bitsInBuffer += 8;
            while (bitsInBuffer >= 5 && outIndex < 26) {
                bitsInBuffer -= 5;
                int idx = (bitBuffer >> bitsInBuffer) & 31;
                out[outIndex++] = CROCKFORD_BASE32[idx];
            }
        }

        if (outIndex < 26 && bitsInBuffer > 0) {
            int idx = (bitBuffer << (5 - bitsInBuffer)) & 31;
            out[outIndex] = CROCKFORD_BASE32[idx];
        }
    }
}
