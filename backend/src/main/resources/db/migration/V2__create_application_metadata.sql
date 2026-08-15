create table application_metadata (
    metadata_key varchar(100) primary key,
    metadata_value varchar(500) not null,
    updated_at timestamp with time zone not null
);
