package com.tulip.service.impl;

import com.tulip.config.payment.VnpayConfig;
import com.tulip.dto.request.VnpayRequest;
import com.tulip.entity.Order;
import com.tulip.entity.PaymentStatus;
import com.tulip.repository.OrderRepository;
import com.tulip.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tulip.util.VnpayUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnpayServiceImpl implements VnpayService {

    @Autowired
    private VnpayConfig vnpayConfig;
    
    @Autowired
    private OrderRepository orderRepository;

    @Override
    public String createPayment(VnpayRequest paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        String vnp_TxnRef = VnpayUtil.getRandomNumber(8);
        return createPaymentWithTxnRef(paymentRequest, vnp_TxnRef, request);
    }

    @Override
    public String createPaymentWithTxnRef(VnpayRequest paymentRequest, String vnpTxnRef, HttpServletRequest request) throws UnsupportedEncodingException {
        
        // Lấy giá trị trực tiếp trong hàm để đảm bảo Config đã được load
        String vnp_Version = vnpayConfig.getVnp_Version();
        String vnp_Command = vnpayConfig.getVnp_Command();
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();
        String vnp_OrderType = vnpayConfig.getOrderType();
        String vnp_ReturnUrl = vnpayConfig.getVnp_ReturnUrl();

        long amount;
        try {
            amount = Long.parseLong(paymentRequest.getAmount()) * 100L;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }

        String vnp_IpAddr = VnpayUtil.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_BankCode", "NCB");
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", paymentRequest.getOrderInfo() != null ? paymentRequest.getOrderInfo() : "Thanh toan don hang:" + vnpTxnRef);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnp_SecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        return vnpayConfig.getVnp_PayUrl() + "?" + query.toString();
    }

    @Override
    @Transactional
    public Long handlePaymentCallback(HttpServletRequest request) {
        try {
            // Lấy các tham số từ VNPAY
            Map<String, String> vnpParams = new HashMap<>();
            Enumeration<String> params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                String paramValue = request.getParameter(paramName);
                if (paramValue != null && !paramValue.isEmpty()) {
                    vnpParams.put(paramName, paramValue);
                }
            }

            // Lấy các thông tin quan trọng
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            String vnp_TransactionNo = vnpParams.get("vnp_TransactionNo");
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");

            // Xóa vnp_SecureHash khỏi params để verify
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            // Verify signature
            if (!verifySignature(vnpParams, vnp_SecureHash)) {
                return null; // Signature không hợp lệ
            }

            // Tìm Order theo vnpTxnRef
            Order order = orderRepository.findByVnpTxnRef(vnp_TxnRef);
            if (order == null) {
                return null; // Không tìm thấy Order
            }

            // Cập nhật Order dựa trên response code
            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setTransactionId(vnp_TransactionNo);
                // Có thể cập nhật order status thành CONFIRMED nếu muốn
                // order.setStatus(Order.OrderStatus.CONFIRMED);
            } else {
                // Thanh toán thất bại
                order.setPaymentStatus(PaymentStatus.FAILED);
            }

            orderRepository.save(order);
            return order.getId();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verify signature từ VNPAY callback
     */
    private boolean verifySignature(Map<String, String> vnpParams, String vnp_SecureHash) {
        try {
            // Sắp xếp params theo alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Build hash data
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    hashData.append('&');
                }
            }
            if (hashData.length() > 0) {
                hashData.setLength(hashData.length() - 1); // Xóa ký tự & cuối cùng
            }

            // Tính toán hash
            String calculatedHash = VnpayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
            
            // So sánh với hash từ VNPAY
            return calculatedHash.equals(vnp_SecureHash);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}