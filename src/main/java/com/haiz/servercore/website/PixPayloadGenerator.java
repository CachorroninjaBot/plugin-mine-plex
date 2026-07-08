package com.haiz.servercore.website;

public final class PixPayloadGenerator {

    private static final String PIX_KEY = "40b028d0-7ae8-4622-9f84-11cc4b2172e7";

    public static String generate(double amount, String txid, String description) {
        String amountStr = String.format("%.2f", amount);
        String pixKeyClean = PIX_KEY.replace("-", "");

        String payload = "000201"
            + "26" + buildTLV("00", "br.gov.bcb.pix") + buildTLV("01", pixKeyClean)
            + (description != null ? buildTLV("02", description.substring(0, Math.min(description.length(), 72))) : "")
            + "52040000"
            + "5303986"
            + "54" + padTLV(amountStr.length()) + amountStr
            + "5802BR"
            + "59" + padTLV("Minepex Legends".length()) + "Minepex Legends"
            + "60" + padTLV("SAO PAULO".length()) + "SAO PAULO"
            + "62" + buildTLV("05", txid.substring(0, Math.min(txid.length(), 25)))
            + "6304";

        return payload + crc16CCITT(payload);
    }

    private static String buildTLV(String id, String value) {
        return id + padTLV(value.length()) + value;
    }

    private static String padTLV(int len) {
        return String.format("%02d", len);
    }

    private static String crc16CCITT(String str) {
        int crc = 0xFFFF;
        for (int i = 0; i < str.length(); i++) {
            crc ^= str.charAt(i) << 8;
            for (int j = 0; j < 8; j++) {
                crc = (crc & 0x8000) != 0 ? ((crc << 1) ^ 0x1021) : (crc << 1);
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
