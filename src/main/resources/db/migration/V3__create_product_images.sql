-- Optional image metadata only. Existing products are unchanged.
CREATE TABLE product_images (
    product_id uuid CONSTRAINT pk_product_images PRIMARY KEY,
    original_key text NOT NULL,
    thumbnail_key text NOT NULL,
    media_type text NOT NULL,
    original_size bigint NOT NULL,
    thumbnail_size bigint NOT NULL,
    width integer NOT NULL,
    height integer NOT NULL,
    thumbnail_width integer NOT NULL,
    thumbnail_height integer NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE NO ACTION,
    CONSTRAINT uq_product_images_original_key UNIQUE (original_key),
    CONSTRAINT uq_product_images_thumbnail_key UNIQUE (thumbnail_key),
    CONSTRAINT ck_product_images_keys CHECK (
        original_key ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/original$'
        AND thumbnail_key = split_part(original_key, '/', 1) || '/thumbnail'),
    CONSTRAINT ck_product_images_media CHECK (media_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_product_images_sizes CHECK (original_size BETWEEN 1 AND 10485760 AND thumbnail_size BETWEEN 1 AND 10485760),
    CONSTRAINT ck_product_images_dimensions CHECK (
        width BETWEEN 1 AND 16384 AND height BETWEEN 1 AND 16384
        AND width::bigint * height <= 40000000
        AND thumbnail_width BETWEEN 1 AND 320 AND thumbnail_height BETWEEN 1 AND 320
        AND thumbnail_width <= width AND thumbnail_height <= height)
);
