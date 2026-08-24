select entry.id
from stock_entries entry
join products product on product.id = entry.product_id
where entry.available_quantity > 0
  and product.search_name like '%especial 99000%'
  and entry.expiration_date between date '0001-01-01' and date '9999-12-31'
order by entry.expiration_date, entry.created_at, entry.id
limit 51;
