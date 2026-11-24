package com.payflow.fixtures.badmod.billing;

import com.payflow.fixtures.badmod.orders.OrderService;
import com.payflow.fixtures.badmod.orders.internal.Secret;

public class BillingService {
    private OrderService orders;   // ciclo: orders -> billing -> orders
    private Secret secret;         // acesso a tipo interno de outro módulo
}
