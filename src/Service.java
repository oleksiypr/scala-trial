import java.util.UUID;

public class Service {

    static class KillBillClientException extends RuntimeException {}

    static class HttpForbiddenException extends RuntimeException {
        public HttpForbiddenException(String message) {
            super(message);
        }
    }

    static class KillbillPaymentMethodApi {

        public PaymentMethod getPaymentMethod(UUID paymentMethodId, Object o) {
            return new PaymentMethod();
        }

        public void deletePaymentMethod(UUID paymentMethodId, Object o) {
            throw new KillBillClientException();
        }
    }

    static class PaymentMethod {
        public UUID getAccountId() {
            return UUID.randomUUID();
        }
    }

    KillbillPaymentMethodApi killbillPaymentMethodApi = new KillbillPaymentMethodApi();

    public void deleteAccountPaymentMethod(UUID accountId, UUID paymentMethodId) {
        try {
            PaymentMethod paymentMethod = killbillPaymentMethodApi.getPaymentMethod(paymentMethodId, null);
            if (!isAllowed(accountId, paymentMethod)) {
                throw new HttpForbiddenException("Payment method doesn't belong to provided " +
                        "account or account doesn't exist.");
            }
            killbillPaymentMethodApi.deletePaymentMethod(paymentMethodId,null);
        } catch (KillBillClientException e) {
            throw e;
        }
    }

    private boolean isAllowed(UUID accountId, PaymentMethod paymentMethod) {
        return paymentMethod.getAccountId().equals(accountId);
    }

    App.Forbidden x = App.Forbidden.apply("aaa");
}
