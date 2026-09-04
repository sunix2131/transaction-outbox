create table payment (
    id uuid primary key,
    idempotency_key varchar(200) not null unique,
    request_hash char(64) not null,
    account_id uuid not null,
    amount numeric(19, 2) not null check (amount > 0),
    currency char(3) not null check (currency = upper(currency)),
    status varchar(20) not null check (status in ('ACCEPTED')),
    created_at timestamptz not null
);

create table outbox_event (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(120) not null,
    payload jsonb not null,
    status varchar(20) not null default 'PENDING' check (status in ('PENDING', 'PUBLISHED', 'DEAD')),
    attempts integer not null default 0 check (attempts >= 0),
    available_at timestamptz not null,
    published_at timestamptz,
    last_error varchar(500),
    created_at timestamptz not null,
    check ((status = 'PUBLISHED') = (published_at is not null))
);

create index outbox_event_ready_idx on outbox_event (available_at, created_at) where status = 'PENDING';

create table processed_event (
    event_id uuid primary key,
    processed_at timestamptz not null
);

create table payment_projection (
    payment_id uuid primary key,
    account_id uuid not null,
    amount numeric(19, 2) not null,
    currency char(3) not null,
    accepted_at timestamptz not null,
    applied_count integer not null check (applied_count > 0)
);
