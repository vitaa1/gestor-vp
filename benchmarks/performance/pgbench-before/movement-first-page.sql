select movement.id
from stock_movements movement
join stock_entries entry on entry.id = movement.stock_entry_id
join products product on product.id = entry.product_id
order by movement.created_at desc, movement.id desc
limit 21;
