alter table products
    add column search_name varchar(120);

create function set_product_search_name()
returns trigger
language plpgsql
as $$
begin
    new.search_name := translate(
        lower(regexp_replace(trim(normalize(new.name, NFC)), '\s+', ' ', 'g')),
        'áàâãäéèêëíìîïóòôõöúùûüçñýÿ',
        'aaaaaeeeeiiiiooooouuuucnyy'
    );
    return new;
end;
$$;

create trigger products_set_search_name
before insert or update of name on products
for each row
execute function set_product_search_name();

update products
set search_name = translate(
    lower(regexp_replace(trim(normalize(name, NFC)), '\s+', ' ', 'g')),
    'áàâãäéèêëíìîïóòôõöúùûüçñýÿ',
    'aaaaaeeeeiiiiooooouuuucnyy'
);

alter table products
    alter column search_name set not null;

create index idx_products_search_name
    on products (search_name, id);
