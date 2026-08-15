create role vence_facil_migration login password 'migration-test-password';
create role vence_facil_runtime login password 'runtime-test-password';

grant usage, create on schema public to vence_facil_migration;
grant usage on schema public to vence_facil_runtime;

grant vence_facil_migration to test with set true, inherit false;
set role vence_facil_migration;

alter default privileges in schema public
    grant select, insert, update, delete on tables to vence_facil_runtime;

alter default privileges in schema public
    grant usage, select on sequences to vence_facil_runtime;

reset role;
grant vence_facil_migration to test with set false, inherit false;
