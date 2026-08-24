\set ON_ERROR_STOP on
\pset pager off
\timing on

select 'active-first-page' as benchmark;
explain (analyze, buffers, settings)
select entry.id from stock_entries entry join products product on product.id = entry.product_id
where entry.available_quantity > 0 and strpos(product.search_name, '') > 0
  and entry.expiration_date between date '0001-01-01' and date '9999-12-31'
order by entry.expiration_date, entry.created_at, entry.id limit 51;

select 'active-rare-search' as benchmark;
explain (analyze, buffers, settings)
select entry.id from stock_entries entry join products product on product.id = entry.product_id
where entry.available_quantity > 0 and strpos(product.search_name, 'especial 99000') > 0
  and entry.expiration_date between date '0001-01-01' and date '9999-12-31'
order by entry.expiration_date, entry.created_at, entry.id limit 51;

select 'movement-first-page' as benchmark;
explain (analyze, buffers, settings)
select movement.id from stock_movements movement
join stock_entries entry on entry.id = movement.stock_entry_id
join products product on product.id = entry.product_id
order by movement.created_at desc, movement.id desc limit 21;

select 'movement-cursor-page' as benchmark;
explain (analyze, buffers, settings)
select movement.id from stock_movements movement
join stock_entries entry on entry.id = movement.stock_entry_id
join products product on product.id = entry.product_id
where movement.created_at < timestamptz '2026-01-02 00:00:00+00'
   or (movement.created_at = timestamptz '2026-01-02 00:00:00+00' and movement.id < 345600)
order by movement.created_at desc, movement.id desc limit 21;
