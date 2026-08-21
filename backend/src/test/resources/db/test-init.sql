create role gestor_vp_migration login password 'migration-test-password';
create role gestor_vp_runtime login password 'runtime-test-password';

grant usage, create on schema public to gestor_vp_migration;
grant usage on schema public to gestor_vp_runtime;

grant gestor_vp_migration to test with set true, inherit false;
set role gestor_vp_migration;

alter default privileges in schema public
    grant select, insert, update, delete on tables to gestor_vp_runtime;

alter default privileges in schema public
    grant usage, select on sequences to gestor_vp_runtime;

reset role;
grant gestor_vp_migration to test with set false, inherit false;
