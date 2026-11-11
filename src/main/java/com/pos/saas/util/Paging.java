package com.pos.saas.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Paging {

    public static final int MAX_SIZE = 100;

    private Paging() {}

    /** page is 0-based; size is clamped to 1..100 so nobody can request a million rows. */
    public static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
