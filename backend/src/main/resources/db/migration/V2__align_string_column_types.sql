ALTER TABLE appointments
ALTER COLUMN verification_hash TYPE VARCHAR(64)
        USING rtrim(verification_hash),
    ALTER COLUMN qr_token_hash TYPE VARCHAR(64)
        USING rtrim(qr_token_hash);

ALTER TABLE refresh_token_sessions
ALTER COLUMN token_hash TYPE VARCHAR(64)
        USING rtrim(token_hash);

ALTER TABLE compensation_packages
ALTER COLUMN currency TYPE VARCHAR(3)
        USING rtrim(currency);