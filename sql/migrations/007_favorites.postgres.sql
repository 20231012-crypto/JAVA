-- Wishlist / "yêu thích" — a customer can favorite a product; admin can see which products are
-- favorited most. Real counts only, computed from this table, never fabricated.
-- Additive only — safe to run against the live database.

CREATE TABLE IF NOT EXISTS favorites (
  user_id     INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  product_id  INT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, product_id)
);
