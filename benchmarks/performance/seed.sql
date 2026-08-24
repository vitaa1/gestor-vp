\set ON_ERROR_STOP on

truncate table stock_movements, stock_entries, products restart identity cascade;

insert into products (name, normalized_name, search_name, created_at, category)
select
    case when product_number % 1000 = 0
        then 'Produto especial ' || product_number
        else 'Produto comum ' || product_number
    end,
    'produto-' || product_number,
    case when product_number % 1000 = 0
        then 'produto especial ' || product_number
        else 'produto comum ' || product_number
    end,
    timestamptz '2026-01-01 00:00:00+00' + product_number * interval '1 second',
    'Benchmark'
from generate_series(1, 100000) as products(product_number);

insert into stock_entries (
    product_id, initial_quantity, available_quantity, expiration_date, created_at,
    unit_cost, supplier, batch_number
)
select
    product_number,
    100,
    case when product_number % 10 = 0 then 0 else 100 end,
    date '2026-08-23' + (product_number % 365),
    timestamptz '2026-01-01 00:00:00+00' + product_number * interval '1 second',
    10.00,
    'Fornecedor benchmark',
    'LOTE-' || product_number
from generate_series(1, 100000) as entries(product_number);

insert into stock_movements (stock_entry_id, movement_type, quantity, reason, created_at)
select
    ((movement_number - 1) % 100000) + 1,
    case when movement_number % 2 = 0 then 'ENTRY' else 'WITHDRAWAL' end,
    1,
    case when movement_number % 2 = 0 then null else 'SOLD' end,
    timestamptz '2026-01-01 00:00:00+00'
        + ((movement_number - 1) / 4) * interval '1 second'
from generate_series(1, 400000) as movements(movement_number);

vacuum (analyze) products;
vacuum (analyze) stock_entries;
vacuum (analyze) stock_movements;
