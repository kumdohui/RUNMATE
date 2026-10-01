ALTER TABLE social_accounts
    DROP COLUMN provider_email;

ALTER TABLE posts
    DROP COLUMN thumbnail_url;

ALTER TABLE post_images
    DROP COLUMN image_url;
