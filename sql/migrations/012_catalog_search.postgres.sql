-- Accent-insensitive menu search.
--
-- Vietnamese is regularly typed without diacritics when searching (it is faster), so a plain
-- ILIKE would make "banh mi" return nothing while "Bánh mì" sits in the menu. unaccent() strips
-- the diacritics on both sides of the comparison so either spelling matches.
--
-- No index to go with it: ILIKE '%...%' cannot use a btree index anyway, and the products table
-- holds tens of rows, so a sequential scan is the correct plan here. Revisit if the menu ever
-- grows into the thousands.
--
-- Additive only — safe to run against the live database.

CREATE EXTENSION IF NOT EXISTS unaccent;
