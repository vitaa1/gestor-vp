do $$
begin
    if exists (select 1 from pg_roles where rolname = 'gestor_vp_runtime') then
        execute 'revoke all privileges on table public.flyway_schema_history from gestor_vp_runtime';
    end if;
end
$$;
