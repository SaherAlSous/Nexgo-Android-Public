package com.nexgo.haydenm.intentcaller;

import java.util.List;

public class ResponseParser {

    private String packetID;
    private PacketDataDTO packetData;

    public String getPacketID() {
        return packetID;
    }

    public void setPacketID(String packetID) {
        this.packetID = packetID;
    }

    public PacketDataDTO getPacketData() {
        return packetData;
    }

    public void setPacketData(PacketDataDTO packetData) {
        this.packetData = packetData;
    }

    public static class PacketDataDTO {
        private String batchID;
        private String transactionID;
        private String transactionType;
        private String baseAmount;
        private String tipAmount;
        private String cashbackAmount;
        private String processedAmount;
        private String resultCode;
        private String hostMessage;
        private String cardNumber;
        private String cardIssuer;
        private String cardDataEntry;
        private String referenceNumber;
        private String authorizationCode;
        private String accountId;
        private String hostResponse;
        private String applicationVersion;
        private String fwVersion;
        private List<String> emvTags;
        private String cardHolderName;
        private String traceNo;
        private String signature;
        private String merchantId;
        private String snapAvailableBal;

        public String getBatchID() {
            return batchID;
        }

        public void setBatchID(String batchID) {
            this.batchID = batchID;
        }

        public String getTransactionID() {
            return transactionID;
        }

        public void setTransactionID(String transactionID) {
            this.transactionID = transactionID;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public String getBaseAmount() {
            return baseAmount;
        }

        public void setBaseAmount(String baseAmount) {
            this.baseAmount = baseAmount;
        }

        public String getTipAmount() {
            return tipAmount;
        }

        public void setTipAmount(String tipAmount) {
            this.tipAmount = tipAmount;
        }

        public String getCashbackAmount() {
            return cashbackAmount;
        }

        public void setCashbackAmount(String cashbackAmount) {
            this.cashbackAmount = cashbackAmount;
        }

        public String getProcessedAmount() {
            return processedAmount;
        }

        public void setProcessedAmount(String processedAmount) {
            this.processedAmount = processedAmount;
        }

        public String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public String getHostMessage() {
            return hostMessage;
        }

        public void setHostMessage(String hostMessage) {
            this.hostMessage = hostMessage;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public String getCardIssuer() {
            return cardIssuer;
        }

        public void setCardIssuer(String cardIssuer) {
            this.cardIssuer = cardIssuer;
        }

        public String getCardDataEntry() {
            return cardDataEntry;
        }

        public void setCardDataEntry(String cardDataEntry) {
            this.cardDataEntry = cardDataEntry;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public String getAuthorizationCode() {
            return authorizationCode;
        }

        public void setAuthorizationCode(String authorizationCode) {
            this.authorizationCode = authorizationCode;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getHostResponse() {
            return hostResponse;
        }

        public void setHostResponse(String hostResponse) {
            this.hostResponse = hostResponse;
        }

        public String getApplicationVersion() {
            return applicationVersion;
        }

        public void setApplicationVersion(String applicationVersion) {
            this.applicationVersion = applicationVersion;
        }

        public String getFwVersion() {
            return fwVersion;
        }

        public void setFwVersion(String fwVersion) {
            this.fwVersion = fwVersion;
        }

        public List<String> getEmvTags() {
            return emvTags;
        }

        public void setEmvTags(List<String> emvTags) {
            this.emvTags = emvTags;
        }

        public String getCardHolderName() {
            return cardHolderName;
        }

        public void setCardHolderName(String cardHolderName) {
            this.cardHolderName = cardHolderName;
        }

        public String getTraceNo() {
            return traceNo;
        }

        public void setTraceNo(String traceNo) {
            this.traceNo = traceNo;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }

        public String getSnapAvailableBal() {
            return snapAvailableBal;
        }

        public void setSnapAvailableBal(String snapAvailableBal) {
            this.snapAvailableBal = snapAvailableBal;
        }
    }
}
