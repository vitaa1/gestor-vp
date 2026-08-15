do $migration$
begin
    if exists (select 1 from pg_roles where rolname = 'vence_facil_runtime') then
        execute 'revoke all privileges on table public.flyway_schema_history from vence_facil_runtime';
    end if;
end
$migration$;
