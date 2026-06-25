package com.retail.retailstoremanagement.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Verifies webhook HMAC behavior against the official payOS sample. */
public final class PayOsSignatureSmokeTest {
    private PayOsSignatureSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String checksumKey = "1a54716c8f0efb2744fb28b6e38b25da7f67a925d98bc1c18bd8faaecadd7675";
        String json = "{"
                + "\"orderCode\":123,\"amount\":3000,\"description\":\"VQRIO123\","
                + "\"accountNumber\":\"12345678\",\"reference\":\"TF230204212323\","
                + "\"transactionDateTime\":\"2023-02-04 18:25:00\","
                + "\"currency\":\"VND\","
                + "\"paymentLinkId\":\"124c33293c43417ab7879e14c8d9eb18\","
                + "\"code\":\"00\",\"desc\":\"Thành công\","
                + "\"counterAccountBankId\":\"\",\"counterAccountBankName\":\"\","
                + "\"counterAccountName\":\"\",\"counterAccountNumber\":\"\","
                + "\"virtualAccountName\":\"\",\"virtualAccountNumber\":\"\"}";
        PayOsClient client = new PayOsClient("client-id", "api-key", checksumKey);
        JsonNode data = client.parse(json);
        String signature =
                "412e915d2871504ed31be63c8f62a149a4410d34c4c42affc9006ef9917eaa03";
        if (!client.verifyWebhook(data, signature)
                || client.verifyWebhook(data, signature.substring(0, 63) + "0")) {
            throw new IllegalStateException("payOS webhook signature validation failed.");
        }
        System.out.println("payOsSignatureSmoke=true");
    }
}
