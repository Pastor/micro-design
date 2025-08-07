package it.micro.simple;

public interface Topics {
    /**
     * Заказ создан
     */
    String CREATE_ORDER = "seller.order.create";
    /**
     * Топик в котором создаются инвойсы на покупку
     */
    String CREATE_INVOICE = "seller.invoice.create";
    /**
     * Топик в который помещается подготовленный для работы инвойс
     */
    String INVOICE_READY = "seller.invoice.ready";
    /**
     * Топик в который помещается информация о готовности к отгрузке
     */
    String DELIVERY_READY = "seller.delivery.ready";
}
