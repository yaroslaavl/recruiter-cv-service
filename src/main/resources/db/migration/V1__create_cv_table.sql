CREATE TABLE cv (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);
