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
        return qrImageUrl(transferNote(orderId), amount);
    }

    /** Same QR generator with an arbitrary transfer-note — used for EAUT Pay wallet top-up requests (see WalletTopupServlet), whose correlation signal is "NAPVI{requestId}" rather than an order. */
    public static String qrImageUrl(String transferNote, BigDecimal amount) {
        String bankId = AppConfig.get("vietqr.bankId");
        String accountNo = AppConfig.get("vietqr.accountNo");
        String accountName = URLEncoder.encode(AppConfig.get("vietqr.accountName"), StandardCharsets.UTF_8);

        return "https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-compact2.png"
                + "?amount=" + amount.toBigInteger()
                + "&addInfo=" + transferNote
                + "&accountName=" + accountName;
    }
}
