CREATE TABLE cv (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id VARCHAR(250) NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    is_main BOOL NOT NULL DEFAULT FALSE,
                    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);
