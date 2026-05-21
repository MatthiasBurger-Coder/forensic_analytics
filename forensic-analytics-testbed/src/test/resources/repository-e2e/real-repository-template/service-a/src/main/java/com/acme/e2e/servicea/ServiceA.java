package com.acme.e2e.servicea;

import com.acme.e2e.serviceb.ServiceB;

public final class ServiceA {
    private final ServiceB serviceB = new ServiceB();

    public String greeting(String name) {
        return "Hello " + serviceB.normalize(name);
    }
}
