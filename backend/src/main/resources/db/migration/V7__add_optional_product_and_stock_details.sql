alter table products
    add column barcode varchar(14),
    add column category varchar(120),
    add constraint chk_products_barcode_format
        check (barcode is null or barcode ~ '^[0-9]{8,14}$');

create unique index uq_products_barcode
    on products (barcode)
    where barcode is not null;

alter table stock_entries
    add column unit_cost numeric(12, 2),
    add column supplier varchar(120),
    add column batch_number varchar(120),
    add constraint chk_stock_entries_unit_cost
        check (unit_cost is null or unit_cost >= 0);
