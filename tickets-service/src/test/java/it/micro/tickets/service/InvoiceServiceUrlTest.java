package it.micro.tickets.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceServiceUrlTest {

    @Test
    void getPaymentUrl_buildsExpectedUrl() {
        WebClient.Builder builder = Mockito.mock(WebClient.Builder.class);
        InvoiceService service = new InvoiceService(builder);
        ReflectionTestUtils.setField(service, "paymentsServiceUrl", "http://payments-service:8081");

        Long invoiceId = 55L;
        Long userId = 42L;
        Long totalAmount = 3000L;

        String url = service.getPaymentUrl(invoiceId, userId, totalAmount);
        assertThat(url).isEqualTo("http://payments-service:8081/api/payments/process?invoiceId=55&userId=42&totalAmount=3000&paymentMethod=CARD");
    }
}
