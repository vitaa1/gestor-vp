alter table stock_movements
    add constraint chk_stock_movements_reason
    check (
        (movement_type = 'ENTRY' and reason is null)
        or (
            movement_type = 'WITHDRAWAL'
            and reason is not null
            and reason in ('SOLD', 'USED', 'DONATED', 'LOST', 'EXPIRED')
        )
    );
