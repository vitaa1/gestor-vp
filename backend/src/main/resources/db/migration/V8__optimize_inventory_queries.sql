create index idx_stock_movements_cursor
    on stock_movements (created_at desc, id desc);
