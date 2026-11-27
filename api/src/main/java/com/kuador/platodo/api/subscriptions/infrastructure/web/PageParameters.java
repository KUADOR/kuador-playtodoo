package com.kuador.platodo.api.subscriptions.infrastructure.web;

import com.kuador.platodo.api.subscriptions.application.port.out.Plans.PageRequest;

/** Pagination of the contract: {@code page} zero-based, {@code size} from 1 to {@value #MAX_SIZE}, 20 by default. */
final class PageParameters {

    static final String DEFAULT_SIZE = "20";
    static final String FIRST_PAGE = "0";

    private static final int MAX_SIZE = 100;

    private PageParameters() {
    }

    /** @throws InvalidSubscriptionsRequestException if {@code page} or {@code size} is out of range */
    static PageRequest of(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new InvalidSubscriptionsRequestException(
                    "`page` must be >= 0 and `size` between 1 and %d.".formatted(MAX_SIZE));
        }
        return new PageRequest(page, size);
    }
}
