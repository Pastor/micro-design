package it.micro.saga.simple;

@SuppressWarnings("InterfaceIsType")
public interface Constants {
  String ORDER_PRODUCT = "product";
  String ORDER_PRODUCT_TYPE = "product-type";
  String ORDER_PRODUCT_ID = "product-id";
  String ORDER_ADDITIONS = "additions";
  String ORDER_ADDITIONS_PRICES = "additions-prices";
  String ORDER_PRODUCT_PRICE = "product-price";
  String ORDER_SUM = "sum";
  String PAYMENT_METHOD = "payment-method";
  String PAYMENT_RESULT = "payment-status";
  String PAYMENT_ID = "payment-id";
  String WAREHOUSE_PRODUCT = "warehouse-product";
  String WAREHOUSE_ADDITIONS = "warehouse-additions";

  String ACTION_ORDER = "order";
  String ACTION_CREATE = "create";
  String ACTION_READY = "ready";
  String ACTION_ROLLBACK = "rollback";

  /**
   * Заказ создан
   */
  String TOPIC_CREATE_ORDER = "seller.order.create";
  /**
   * Топик в котором создаются инвойсы на покупку
   */
  String TOPIC_CREATE_INVOICE = "seller.invoice.create";
  /**
   * Топик в который помещается подготовленный для работы инвойс
   */
  String TOPIC_INVOICE_READY = "seller.invoice.ready";
  /**
   * Топик в который помещается информация о готовности к отгрузке
   */
  String TOPIC_DELIVERY_READY = "seller.delivery.ready";
  String TOPIC_TRANSACTION_REQUEST_RESULT = "seller.transaction.result.create";

  String SERVICE_MARKET = "market";
  String SERVICE_INVOICE = "invoice";
  String SERVICE_WAREHOUSE = "warehouse";
  String SERVICE_OUTPOST = "outpost";
  String SERVICE_PAYMENT = "payment";
}
