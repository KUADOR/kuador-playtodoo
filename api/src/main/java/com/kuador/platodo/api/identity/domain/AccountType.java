package com.kuador.platodo.api.identity.domain;

/** Kind of account behind an access token (claim {@code typ}); each kind lives in a different place. */
public enum AccountType {

    /** Staff of one complex; the account lives in the schema of that complex. */
    STAFF,
    /** Player with one global account, valid on every complex. */
    PLAYER,
    /** Member of the Kuador team; reaches complex data only through an audited access session. */
    PLATFORM
}
