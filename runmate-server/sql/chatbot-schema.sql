CREATE TABLE IF NOT EXISTS chat_conversations (
    conversation_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_message_at DATETIME NULL,
    deleted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id),
    KEY idx_chat_conversations_user_recent (
        user_id,
        status,
        last_message_at,
        conversation_id
    ),
    CONSTRAINT fk_chat_conversations_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    model_name VARCHAR(100) NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    UNIQUE KEY uk_chat_messages_sequence (
        conversation_id,
        sequence_number
    ),
    KEY idx_chat_messages_recent (
        conversation_id,
        status,
        sequence_number
    ),
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES chat_conversations (conversation_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_chat_messages_model CHECK (
        (role = 'USER' AND model_name IS NULL)
        OR (role = 'ASSISTANT' AND model_name IS NOT NULL)
    )
);
