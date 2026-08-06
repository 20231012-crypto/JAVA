package com.eaut.canteen.util;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class VietQRUtil {

    private VietQRUtil() {
    }

    /** The transfer-note customers/staff must keep unedited — the only order-correlation signal we have. */
    public static String transferNote(int orderId) {
        return "DH" + orderId;
    }

    public static String qrImageUrl(int orderId, BigDecimal amount) {
        String bankId = AppConfig.get("vietqr.bankId");
        String accountNo = AppConfig.get("vietqr.accountNo");
        String accountName = URLEncoder.encode(AppConfig.get("vietqr.accountName"), StandardCharsets.UTF_8);

        return "https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-compact2.png"
                + "?amount=" + amount.toBigInteger()
                + "&addInfo=" + transferNote(orderId)
                + "&accountName=" + accountName;
    }
}
