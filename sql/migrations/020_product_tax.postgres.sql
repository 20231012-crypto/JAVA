-- VAT, recorded on the product and carried onto the order.
--
-- The rate belongs to the dish, not to the sale: bottled drinks and prepared food are taxed
-- differently, and a cashier at the till should not be deciding which. So it is set once when the
-- product is created, and the POS just sells — no per-transaction tax switch to get wrong.
--
-- THE PRICE IS TAX-INCLUSIVE. products.price stays exactly what the student pays, which is what is
-- printed on the menu board and what every existing order was charged. tax_percent records how much
-- of that price is VAT, so a receipt can break it out. Treating price as pre-tax instead would
-- silently raise the cost of every item on a live site, which is not a change to make quietly.
--
-- orders.tax_amount snapshots the VAT contained in the sale at the moment it happened, for the same
-- reason order_items snapshots unit_price: a rate edited next term must not rewrite last term's
-- receipts.
--
-- Defaults of 0 mean every existing product and order reads as "no VAT recorded", which is exactly
-- what they are — the field did not exist when they were made.
--
-- Additive only — safe to run against the live database.

ALTER TABLE products ADD COLUMN IF NOT EXISTS tax_percent DECIMAL(5,2) NOT NULL DEFAULT 0;

ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_products_tax_percent;
-- Upper bound of 100 because the rate is a percentage of a tax-inclusive price: at 100 the whole
-- price would be tax, and anything beyond it is arithmetic nonsense rather than a policy.
ALTER TABLE products ADD CONSTRAINT chk_products_tax_percent
  CHECK (tax_percent >= 0 AND tax_percent < 100);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(12,0) NOT NULL DEFAULT 0;

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_tax_nonneg;
ALTER TABLE orders ADD CONSTRAINT chk_orders_tax_nonneg CHECK (tax_amount >= 0);
