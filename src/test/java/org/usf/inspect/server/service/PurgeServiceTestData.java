package org.usf.inspect.server.service;

import static java.util.UUID.fromString;

import java.util.UUID;

final class PurgeServiceTestData {

    static final String APP = "app-a";
    static final String NAMESPACE = "JARVIS_DEV";

    static final UUID OLD_INSTANCE_ID = fromString("10000000-0000-0000-0000-000000000001");
    static final UUID RECENT_INSTANCE_ID = fromString("10000000-0000-0000-0000-000000000002");
    static final UUID OTHER_INSTANCE_ID = fromString("10000000-0000-0000-0000-000000000003");
    static final UUID OLD_REQUEST_ID = fromString("20000000-0000-0000-0000-000000000001");
    static final UUID RECENT_REQUEST_ID = fromString("20000000-0000-0000-0000-000000000002");
    static final UUID ORPHAN_INSTANCE_ID = fromString("30000000-0000-0000-0000-000000000001");

    static final UUID ACTIVE_INSTANCE_ID = fromString("11111111-1111-1111-1111-111111111111");
    static final UUID CLOSED_INSTANCE_ID = fromString("22222222-2222-2222-2222-222222222222");
    static final UUID ABANDONED_INSTANCE_ID = fromString("33333333-3333-3333-3333-333333333333");
    static final UUID ACTIVE_REQUEST_ID = fromString("44444444-4444-4444-4444-444444444444");
    static final UUID CLOSED_REQUEST_ID = fromString("55555555-5555-5555-5555-555555555555");
    static final UUID ABANDONED_REQUEST_ID = fromString("66666666-6666-6666-6666-666666666666");
    static final UUID NO_TRACE_INSTANCE_ID = fromString("77777777-7777-7777-7777-777777777777");
    static final UUID NO_TRACE_REQUEST_ID = fromString("88888888-8888-8888-8888-888888888888");
    static final UUID RECENT_NO_TRACE_INSTANCE_ID = fromString("99999999-9999-9999-9999-999999999999");
    static final UUID RECENT_NO_TRACE_REQUEST_ID = fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private PurgeServiceTestData() {
    }
}
